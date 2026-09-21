package com.dongbacsaigon.backend.media.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryAsset;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryDeleteResult;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryGateway;
import com.dongbacsaigon.backend.media.config.CloudinaryProperties;
import com.dongbacsaigon.backend.media.config.MediaProperties;
import com.dongbacsaigon.backend.media.dto.MediaPageResponse;
import com.dongbacsaigon.backend.media.dto.MediaResponse;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadRequest;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadResponse;
import com.dongbacsaigon.backend.media.dto.MediaUpdateRequest;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.entity.MediaUploadIntent;
import com.dongbacsaigon.backend.media.entity.MediaUploadIntentStatus;
import com.dongbacsaigon.backend.media.repository.MediaRepository;
import com.dongbacsaigon.backend.media.repository.MediaUploadIntentRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.entity.UserRole;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MediaService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final MediaUploadIntentRepository mediaUploadIntentRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final CloudinaryProperties cloudinaryProperties;
    private final MediaProperties mediaProperties;
    private final CloudinaryGateway cloudinaryGateway;
    private final MediaValidationService mediaValidationService;
    private final MediaUsageService mediaUsageService;
    private final AuditService auditService;

    public MediaService(
            MediaUploadIntentRepository mediaUploadIntentRepository,
            MediaRepository mediaRepository,
            UserRepository userRepository,
            CloudinaryProperties cloudinaryProperties,
            MediaProperties mediaProperties,
            CloudinaryGateway cloudinaryGateway,
            MediaValidationService mediaValidationService,
            MediaUsageService mediaUsageService,
            AuditService auditService
    ) {
        this.mediaUploadIntentRepository = mediaUploadIntentRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.cloudinaryProperties = cloudinaryProperties;
        this.mediaProperties = mediaProperties;
        this.cloudinaryGateway = cloudinaryGateway;
        this.mediaValidationService = mediaValidationService;
        this.mediaUsageService = mediaUsageService;
        this.auditService = auditService;
    }

    @Transactional
    public MediaSignedUploadResponse createSignedUpload(MediaSignedUploadRequest request, UUID userId) {
        User user = requireUser(userId);
        MediaType mediaType = request.mediaType();
        if (mediaType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Media type is required.");
        }

        Instant now = Instant.now();
        String folder = currentMediaFolder(now);
        String publicId = UUID.randomUUID().toString();
        MediaUploadIntent intent = new MediaUploadIntent(
                publicId,
                folder,
                mediaType,
                normalizeOptional(request.originalFilename(), 255),
                user,
                now.plus(mediaProperties.uploadIntentTtl())
        );
        mediaUploadIntentRepository.save(intent);

        long timestamp = now.getEpochSecond();
        Map<String, Object> signedParameters = Map.of(
                "timestamp", timestamp,
                "folder", folder,
                "public_id", publicId
        );

        return new MediaSignedUploadResponse(
                intent.getId(),
                cloudinaryProperties.cloudName(),
                cloudinaryProperties.apiKey(),
                timestamp,
                cloudinaryGateway.signUploadParameters(signedParameters),
                folder,
                publicId,
                mediaType.cloudinaryResourceType(),
                "https://api.cloudinary.com/v1_1/"
                        + cloudinaryProperties.cloudName()
                        + "/"
                        + mediaType.cloudinaryResourceType()
                        + "/upload",
                intent.getExpiresAt()
        );
    }

    @Transactional
    public MediaResponse completeUpload(UUID intentId, UUID userId) {
        User user = requireUser(userId);
        MediaUploadIntent intent = mediaUploadIntentRepository.findByIdForUpdate(intentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Upload intent not found."));

        if (!intent.getRequestedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the upload intent owner can complete it.");
        }
        if (intent.getStatus() == MediaUploadIntentStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Upload intent has already been completed.");
        }
        if (intent.getStatus() != MediaUploadIntentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Upload intent is not pending.");
        }
        if (intent.isExpired(Instant.now())) {
            intent.expire();
            throw new ApiException(HttpStatus.CONFLICT, "Upload intent has expired.");
        }

        CloudinaryAsset asset;
        try {
            asset = cloudinaryGateway.fetchResource(intent.expectedCloudinaryPublicId(), intent.getMediaType());
            mediaValidationService.validateCloudinaryAsset(
                    asset,
                    intent.getMediaType(),
                    intent.expectedCloudinaryPublicId(),
                    intent.getFolder()
            );
        } catch (RuntimeException exception) {
            intent.fail();
            destroyInvalidAssetQuietly(intent);
            throw exception;
        }

        if (mediaRepository.existsByAssetId(asset.assetId())) {
            intent.fail();
            throw new ApiException(HttpStatus.CONFLICT, "Cloudinary asset has already been registered.");
        }

        Media media = new Media(
                asset.assetId(),
                asset.publicId(),
                asset.resourceType(),
                asset.format().toLowerCase(Locale.ROOT),
                asset.secureUrl(),
                asset.width(),
                asset.height(),
                asset.bytes(),
                asset.durationSeconds(),
                asset.folder(),
                intent.getOriginalFilename(),
                user
        );

        try {
            Media savedMedia = mediaRepository.save(media);
            intent.complete(Instant.now());
            auditService.recordSuccessAfterCommit(
                    user,
                    AuditAction.MEDIA_UPLOAD_COMPLETED,
                    AuditTargetType.MEDIA,
                    savedMedia.getId(),
                    null
            );
            return MediaMapper.toResponse(savedMedia);
        } catch (DataIntegrityViolationException exception) {
            intent.fail();
            throw new ApiException(HttpStatus.CONFLICT, "Cloudinary asset has already been registered.");
        }
    }

    @Transactional(readOnly = true)
    public MediaPageResponse listMedia(
            int page,
            int size,
            MediaType mediaType,
            MediaStatus status,
            String search,
            UUID uploadedBy
    ) {
        Page<MediaResponse> mediaPage = mediaRepository.findAll(
                        specification(mediaType, status, search, uploadedBy),
                        pageRequest(page, size)
                )
                .map(MediaMapper::toResponse);

        return new MediaPageResponse(
                mediaPage.getContent(),
                mediaPage.getNumber(),
                mediaPage.getSize(),
                mediaPage.getTotalElements(),
                mediaPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MediaResponse getMedia(UUID mediaId) {
        return MediaMapper.toResponse(requireMedia(mediaId));
    }

    @Transactional
    public MediaResponse updateMedia(UUID mediaId, MediaUpdateRequest request, UUID userId) {
        User user = requireUser(userId);
        Media media = requireMedia(mediaId);
        ensureStaffCanManageOwnedMedia(user, media);
        media.updateAltText(normalizeOptional(request.altText(), 255));
        auditService.recordSuccessAfterCommit(
                user,
                AuditAction.MEDIA_METADATA_UPDATED,
                AuditTargetType.MEDIA,
                media.getId(),
                null
        );
        return MediaMapper.toResponse(media);
    }

    @Transactional
    public void deleteMedia(UUID mediaId, UUID userId) {
        User user = requireUser(userId);
        Media media = requireMedia(mediaId);
        ensureStaffCanManageOwnedMedia(user, media);

        if (!media.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Media is already deleted.");
        }
        if (mediaUsageService.isReferenced(media.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Media is currently referenced and cannot be deleted.");
        }

        CloudinaryDeleteResult deleteResult = cloudinaryGateway.destroy(media.getPublicId(), media.getResourceType());
        if (!deleteResult.acceptable()) {
            throw new ApiException(HttpStatus.CONFLICT, "Cloudinary media deletion failed.");
        }

        media.markDeleted(user, Instant.now());
        auditService.recordSuccessAfterCommit(
                user,
                AuditAction.MEDIA_DELETED,
                AuditTargetType.MEDIA,
                media.getId(),
                deleteResult.result()
        );
    }

    public Media requireActiveMedia(UUID mediaId) {
        Media media = requireMedia(mediaId);
        mediaValidationService.requireActiveMedia(media, "Media must be active.");
        return media;
    }

    public Media requireActiveImage(UUID mediaId) {
        Media media = requireMedia(mediaId);
        mediaValidationService.requireActiveImage(media, "Media must be an active image.");
        return media;
    }

    private Media requireMedia(UUID mediaId) {
        return mediaRepository.findDetailedById(mediaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found."));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized."));
    }

    private void ensureStaffCanManageOwnedMedia(User user, Media media) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!media.getUploadedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STAFF can manage only media they uploaded.");
        }
    }

    private String currentMediaFolder(Instant now) {
        YearMonth yearMonth = YearMonth.from(now.atZone(ZoneOffset.UTC));
        return cloudinaryProperties.rootFolder()
                + "/media/"
                + yearMonth.getYear()
                + "/"
                + String.format("%02d", yearMonth.getMonthValue());
    }

    private void destroyInvalidAssetQuietly(MediaUploadIntent intent) {
        try {
            cloudinaryGateway.destroy(intent.expectedCloudinaryPublicId(), intent.getMediaType());
        } catch (RuntimeException ignored) {
            // Best-effort cleanup only. The upload registration still fails.
        }
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.length() <= maxLength ? trimmedValue : trimmedValue.substring(0, maxLength);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0.");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size == 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100.");
        }
        return size;
    }

    private Specification<Media> specification(MediaType mediaType, MediaStatus status, String search, UUID uploadedBy) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (mediaType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("resourceType"), mediaType));
            }
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            if (uploadedBy != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("uploadedBy").get("id"), uploadedBy));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("originalFilename")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("altText")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("publicId")), pattern)
                ));
            }
            return predicate;
        };
    }
}

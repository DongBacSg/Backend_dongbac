package com.dongbacsaigon.backend.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryAsset;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryGateway;
import com.dongbacsaigon.backend.media.config.CloudinaryProperties;
import com.dongbacsaigon.backend.media.config.MediaProperties;
import com.dongbacsaigon.backend.media.dto.MediaResponse;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadRequest;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadResponse;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.entity.MediaUploadIntent;
import com.dongbacsaigon.backend.media.entity.MediaUploadIntentStatus;
import com.dongbacsaigon.backend.media.repository.MediaRepository;
import com.dongbacsaigon.backend.media.repository.MediaUploadIntentRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MediaServiceTest {

    private final MediaUploadIntentRepository mediaUploadIntentRepository = mock(MediaUploadIntentRepository.class);
    private final MediaRepository mediaRepository = mock(MediaRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CloudinaryGateway cloudinaryGateway = mock(CloudinaryGateway.class);
    private final MediaUsageService mediaUsageService = mock(MediaUsageService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final CloudinaryProperties cloudinaryProperties = new CloudinaryProperties(
            "demo-cloud",
            "demo-key",
            "super-secret",
            "/dongbac/",
            10
    );
    private final MediaProperties mediaProperties = new MediaProperties(1_000_000, 20_000_000, 15);
    private final MediaService mediaService = new MediaService(
            mediaUploadIntentRepository,
            mediaRepository,
            userRepository,
            cloudinaryProperties,
            mediaProperties,
            cloudinaryGateway,
            new MediaValidationService(mediaProperties),
            mediaUsageService,
            auditService
    );

    @Test
    void createSignedUploadReturnsCloudinarySignatureWithoutApiSecret() {
        User staff = staff();
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(mediaUploadIntentRepository.save(any(MediaUploadIntent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cloudinaryGateway.signUploadParameters(anyMap())).thenReturn("signed-upload");

        MediaSignedUploadResponse response = mediaService.createSignedUpload(
                new MediaSignedUploadRequest(MediaType.IMAGE, " hero.png "),
                staff.getId()
        );

        ArgumentCaptor<Map<String, Object>> signedParameters = mapCaptor();
        verify(cloudinaryGateway).signUploadParameters(signedParameters.capture());

        assertThat(response.cloudName()).isEqualTo("demo-cloud");
        assertThat(response.apiKey()).isEqualTo("demo-key");
        assertThat(response.signature()).isEqualTo("signed-upload");
        assertThat(response.resourceType()).isEqualTo("image");
        assertThat(response.folder()).startsWith("dongbac/media/");
        assertThat(response.uploadUrl()).isEqualTo("https://api.cloudinary.com/v1_1/demo-cloud/image/upload");
        assertThat(response.toString()).doesNotContain("super-secret");
        assertThat(signedParameters.getValue())
                .containsEntry("folder", response.folder())
                .containsEntry("public_id", response.publicId())
                .containsKey("timestamp");
    }

    @Test
    void completeUploadFetchesCloudinaryAssetAndPersistsServerSideMetadata() {
        User staff = staff();
        MediaUploadIntent intent = pendingIntent(staff);
        CloudinaryAsset asset = new CloudinaryAsset(
                "asset-1",
                intent.expectedCloudinaryPublicId(),
                MediaType.IMAGE,
                "PNG",
                "https://res.cloudinary.com/demo/image/upload/v1/hero.png",
                1280,
                720,
                900_000,
                null,
                intent.getFolder()
        );
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(mediaUploadIntentRepository.findByIdForUpdate(intent.getId())).thenReturn(Optional.of(intent));
        when(cloudinaryGateway.fetchResource(intent.expectedCloudinaryPublicId(), MediaType.IMAGE)).thenReturn(asset);
        when(mediaRepository.existsByAssetId("asset-1")).thenReturn(false);
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaResponse response = mediaService.completeUpload(intent.getId(), staff.getId());

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(mediaCaptor.capture());

        assertThat(intent.getStatus()).isEqualTo(MediaUploadIntentStatus.COMPLETED);
        assertThat(intent.getCompletedAt()).isNotNull();
        assertThat(response.publicId()).isEqualTo(intent.expectedCloudinaryPublicId());
        assertThat(response.format()).isEqualTo("png");
        assertThat(response.status()).isEqualTo(MediaStatus.ACTIVE);
        assertThat(response.originalFilename()).isEqualTo("hero.png");
        assertThat(response.uploadedBy()).isEqualTo(staff.getId());
        assertThat(mediaCaptor.getValue().getAssetId()).isEqualTo("asset-1");
        assertThat(mediaCaptor.getValue().getWidth()).isEqualTo(1280);
        verify(auditService).recordSuccessAfterCommit(
                staff,
                AuditAction.MEDIA_UPLOAD_COMPLETED,
                AuditTargetType.MEDIA,
                response.id(),
                null
        );
    }

    @Test
    void completeUploadRejectsInvalidCloudinaryFormatAndDestroysInvalidAsset() {
        User staff = staff();
        MediaUploadIntent intent = pendingIntent(staff);
        CloudinaryAsset asset = new CloudinaryAsset(
                "asset-gif",
                intent.expectedCloudinaryPublicId(),
                MediaType.IMAGE,
                "gif",
                "https://res.cloudinary.com/demo/image/upload/v1/hero.gif",
                640,
                480,
                100_000,
                null,
                intent.getFolder()
        );
        when(userRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(mediaUploadIntentRepository.findByIdForUpdate(intent.getId())).thenReturn(Optional.of(intent));
        when(cloudinaryGateway.fetchResource(intent.expectedCloudinaryPublicId(), MediaType.IMAGE)).thenReturn(asset);

        assertThatThrownBy(() -> mediaService.completeUpload(intent.getId(), staff.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Uploaded asset format is not allowed.");

        assertThat(intent.getStatus()).isEqualTo(MediaUploadIntentStatus.FAILED);
        verify(cloudinaryGateway).destroy(intent.expectedCloudinaryPublicId(), MediaType.IMAGE);
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void deleteMediaRejectsReferencedMediaBeforeCloudinaryDeletion() {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        Media media = imageMedia(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(mediaRepository.findDetailedById(media.getId())).thenReturn(Optional.of(media));
        when(mediaUsageService.isReferenced(media.getId())).thenReturn(true);

        assertThatThrownBy(() -> mediaService.deleteMedia(media.getId(), admin.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Media is currently referenced and cannot be deleted.");

        verify(cloudinaryGateway, never()).destroy(media.getPublicId(), media.getResourceType());
    }

    @Test
    void staffCannotDeleteMediaUploadedByAnotherStaffMember() {
        User uploader = staff();
        User actor = User.staff("other@example.com", "$2a$12$hash", "Other Staff", UUID.randomUUID());
        Media media = imageMedia(uploader);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(mediaRepository.findDetailedById(media.getId())).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> mediaService.deleteMedia(media.getId(), actor.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("STAFF can manage only media they uploaded.");

        verify(mediaUsageService, never()).isReferenced(media.getId());
        verify(cloudinaryGateway, never()).destroy(media.getPublicId(), media.getResourceType());
    }

    private User staff() {
        return User.staff("staff@example.com", "$2a$12$hash", "Staff", UUID.randomUUID());
    }

    private MediaUploadIntent pendingIntent(User user) {
        return new MediaUploadIntent(
                "hero-public-id",
                "dongbac/media/2026/09",
                MediaType.IMAGE,
                "hero.png",
                user,
                java.time.Instant.now().plusSeconds(900)
        );
    }

    private Media imageMedia(User uploader) {
        return new Media(
                "asset-image",
                "dongbac/media/2026/09/image",
                MediaType.IMAGE,
                "webp",
                "https://res.cloudinary.com/demo/image/upload/v1/image.webp",
                800,
                600,
                300_000,
                BigDecimal.ZERO,
                "dongbac/media/2026/09",
                "image.webp",
                uploader
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}

package com.dongbacsaigon.backend.site.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.dongbacsaigon.backend.article.service.ArticleContentSanitizer;
import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.catalog.service.SlugService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.service.MediaMapper;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.site.dto.AdminManufacturingServiceResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingServicePageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingServiceRequest;
import com.dongbacsaigon.backend.site.dto.PublicManufacturingServiceResponse;
import com.dongbacsaigon.backend.site.dto.PublicManufacturingServiceSummary;
import com.dongbacsaigon.backend.site.entity.ManufacturingService;
import com.dongbacsaigon.backend.site.repository.ManufacturingServiceRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ManufacturingServiceManager {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LIST_SORT = Sort.by("sortOrder").ascending()
            .and(Sort.by("title").ascending())
            .and(Sort.by("id").ascending());

    private final ManufacturingServiceRepository repository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final SlugService slugService;
    private final ArticleContentSanitizer contentSanitizer;
    private final AuditService auditService;

    public ManufacturingServiceManager(
            ManufacturingServiceRepository repository,
            UserRepository userRepository,
            MediaService mediaService,
            SlugService slugService,
            ArticleContentSanitizer contentSanitizer,
            AuditService auditService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.mediaService = mediaService;
        this.slugService = slugService;
        this.contentSanitizer = contentSanitizer;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ManufacturingServicePageResponse listAdmin(int page, int size, String search, Boolean active) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0.");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100.");
        }
        String normalizedSearch = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;
        Page<ManufacturingService> result = repository.findAdminPage(
                normalizedSearch, active, PageRequest.of(page, size, LIST_SORT)
        );
        return new ManufacturingServicePageResponse(
                result.getContent().stream().map(this::toAdminResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminManufacturingServiceResponse getAdmin(UUID id) {
        return toAdminResponse(requireService(id));
    }

    @Transactional
    public AdminManufacturingServiceResponse create(ManufacturingServiceRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        ResolvedDetails details = resolve(request, null, null);
        ManufacturingService service = new ManufacturingService(
                details.slug(), details.title(), details.summary(), details.content(), details.featuredMedia(),
                details.seoTitle(), details.seoDescription(), details.sortOrder(), details.active()
        );
        requireUniqueSlug(details.slug(), service.getId());
        repository.save(service);
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SERVICE_CREATED,
                AuditTargetType.MANUFACTURING_SERVICE, service.getId(), null);
        return toAdminResponse(service);
    }

    @Transactional
    public AdminManufacturingServiceResponse update(UUID id, ManufacturingServiceRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        ManufacturingService service = requireService(id);
        ResolvedDetails details = resolve(request, service.getSortOrder(), service.isActive());
        requireUniqueSlug(details.slug(), id);
        service.update(details.slug(), details.title(), details.summary(), details.content(),
                details.featuredMedia(), details.seoTitle(), details.seoDescription(), details.sortOrder(), details.active());
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SERVICE_UPDATED,
                AuditTargetType.MANUFACTURING_SERVICE, id, null);
        return toAdminResponse(service);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        User actor = requireUser(actorId);
        ManufacturingService service = requireService(id);
        repository.delete(service);
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SERVICE_DELETED,
                AuditTargetType.MANUFACTURING_SERVICE, id, null);
    }

    @Transactional(readOnly = true)
    public List<PublicManufacturingServiceSummary> listPublic() {
        return repository.findByActiveTrueOrderBySortOrderAscTitleAscIdAsc().stream()
                .map(this::toPublicSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicManufacturingServiceResponse getPublic(String slug) {
        ManufacturingService service = repository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturing service not found."));
        return new PublicManufacturingServiceResponse(
                service.getId(), service.getSlug(), service.getTitle(), service.getSummary(), service.getContent(),
                MediaMapper.toPublicResponse(service.getFeaturedMedia()), service.getSeoTitle(), service.getSeoDescription()
        );
    }

    private ResolvedDetails resolve(ManufacturingServiceRequest request, Integer existingSortOrder, Boolean existingActive) {
        String title = plainText(request.title());
        if (!StringUtils.hasText(title)) throw new ApiException(HttpStatus.BAD_REQUEST, "Service title is required.");
        String slug = slugService.normalize(StringUtils.hasText(request.slug()) ? request.slug() : title);
        if (slug.length() > 180) throw new ApiException(HttpStatus.BAD_REQUEST, "Service slug is too long.");
        String summary = plainText(request.summary());
        String content = StringUtils.hasText(request.content())
                ? contentSanitizer.sanitizeRequired(request.content()) : null;
        String seoTitle = plainText(request.seoTitle());
        String seoDescription = plainText(request.seoDescription());
        Media featuredMedia = request.featuredMediaId() == null
                ? null : mediaService.requireActiveImage(request.featuredMediaId());
        int sortOrder = request.sortOrder() == null
                ? (existingSortOrder == null ? 0 : existingSortOrder) : request.sortOrder();
        if (sortOrder < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Sort order must not be negative.");
        boolean active = request.active() == null
                ? (existingActive == null || existingActive) : request.active();
        return new ResolvedDetails(slug, title, summary, content, featuredMedia,
                seoTitle, seoDescription, sortOrder, active);
    }

    private String plainText(String value) {
        return StringUtils.hasText(value) ? Jsoup.clean(value.trim(), Safelist.none()).trim() : null;
    }

    private void requireUniqueSlug(String slug, UUID id) {
        if (repository.existsBySlugAndIdNot(slug, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Manufacturing service slug is already in use.");
        }
    }

    private ManufacturingService requireService(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturing service not found."));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized."));
    }

    private AdminManufacturingServiceResponse toAdminResponse(ManufacturingService service) {
        return new AdminManufacturingServiceResponse(
                service.getId(), service.getSlug(), service.getTitle(), service.getSummary(), service.getContent(),
                MediaMapper.toPublicResponse(service.getFeaturedMedia()), service.getSeoTitle(), service.getSeoDescription(),
                service.getSortOrder(), service.isActive(), service.getCreatedAt(), service.getUpdatedAt()
        );
    }

    private PublicManufacturingServiceSummary toPublicSummary(ManufacturingService service) {
        return new PublicManufacturingServiceSummary(
                service.getId(), service.getSlug(), service.getTitle(), service.getSummary(),
                MediaMapper.toPublicResponse(service.getFeaturedMedia()), service.getSeoTitle()
        );
    }

    private record ResolvedDetails(
            String slug, String title, String summary, String content, Media featuredMedia,
            String seoTitle, String seoDescription, int sortOrder, boolean active
    ) {
    }
}

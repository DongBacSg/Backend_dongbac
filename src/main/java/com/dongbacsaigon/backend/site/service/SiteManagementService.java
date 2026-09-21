package com.dongbacsaigon.backend.site.service;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.site.dto.ManufacturingPageRequest;
import com.dongbacsaigon.backend.site.dto.ManufacturingPageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingSectionRequest;
import com.dongbacsaigon.backend.site.dto.ManufacturingSectionResponse;
import com.dongbacsaigon.backend.site.dto.SiteBannerRequest;
import com.dongbacsaigon.backend.site.dto.SiteBannerResponse;
import com.dongbacsaigon.backend.site.dto.SitePartnerRequest;
import com.dongbacsaigon.backend.site.dto.SitePartnerResponse;
import com.dongbacsaigon.backend.site.dto.SiteSettingsRequest;
import com.dongbacsaigon.backend.site.dto.SiteSettingsResponse;
import com.dongbacsaigon.backend.site.entity.ManufacturingPage;
import com.dongbacsaigon.backend.site.entity.ManufacturingSection;
import com.dongbacsaigon.backend.site.entity.SiteBanner;
import com.dongbacsaigon.backend.site.entity.SitePartner;
import com.dongbacsaigon.backend.site.entity.SiteSettings;
import com.dongbacsaigon.backend.site.repository.ManufacturingPageRepository;
import com.dongbacsaigon.backend.site.repository.ManufacturingSectionRepository;
import com.dongbacsaigon.backend.site.repository.SiteBannerRepository;
import com.dongbacsaigon.backend.site.repository.SitePartnerRepository;
import com.dongbacsaigon.backend.site.repository.SiteSettingsRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SiteManagementService {

    private final SiteBannerRepository siteBannerRepository;
    private final SitePartnerRepository sitePartnerRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final ManufacturingPageRepository manufacturingPageRepository;
    private final ManufacturingSectionRepository manufacturingSectionRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final SafeUrlValidator safeUrlValidator;
    private final AuditService auditService;

    public SiteManagementService(
            SiteBannerRepository siteBannerRepository,
            SitePartnerRepository sitePartnerRepository,
            SiteSettingsRepository siteSettingsRepository,
            ManufacturingPageRepository manufacturingPageRepository,
            ManufacturingSectionRepository manufacturingSectionRepository,
            UserRepository userRepository,
            MediaService mediaService,
            SafeUrlValidator safeUrlValidator,
            AuditService auditService
    ) {
        this.siteBannerRepository = siteBannerRepository;
        this.sitePartnerRepository = sitePartnerRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.manufacturingPageRepository = manufacturingPageRepository;
        this.manufacturingSectionRepository = manufacturingSectionRepository;
        this.userRepository = userRepository;
        this.mediaService = mediaService;
        this.safeUrlValidator = safeUrlValidator;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SiteBannerResponse> listBanners(boolean publicOnly) {
        List<SiteBanner> banners = publicOnly
                ? siteBannerRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()
                : siteBannerRepository.findAllByOrderBySortOrderAscCreatedAtAsc();
        return banners.stream().map(SiteMapper::toBannerResponse).toList();
    }

    @Transactional
    public SiteBannerResponse createBanner(SiteBannerRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        Media media = mediaService.requireActiveImage(request.mediaId());
        SiteBanner banner = siteBannerRepository.save(new SiteBanner(
                media,
                normalizeOptional(request.altText(), 255),
                request.sortOrder(),
                request.active(),
                actor
        ));
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_BANNER_CREATED, AuditTargetType.BANNER, banner.getId(), null);
        return SiteMapper.toBannerResponse(banner);
    }

    @Transactional
    public SiteBannerResponse updateBanner(UUID id, SiteBannerRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        SiteBanner banner = siteBannerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Banner not found."));
        Media media = mediaService.requireActiveImage(request.mediaId());
        banner.update(media, normalizeOptional(request.altText(), 255), request.sortOrder(), request.active(), actor);
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_BANNER_UPDATED, AuditTargetType.BANNER, banner.getId(), null);
        return SiteMapper.toBannerResponse(banner);
    }

    @Transactional
    public void deleteBanner(UUID id, UUID actorId) {
        User actor = requireUser(actorId);
        SiteBanner banner = siteBannerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Banner not found."));
        siteBannerRepository.delete(banner);
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_BANNER_DELETED, AuditTargetType.BANNER, id, null);
    }

    @Transactional(readOnly = true)
    public List<SitePartnerResponse> listPartners(boolean publicOnly) {
        List<SitePartner> partners = publicOnly
                ? sitePartnerRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()
                : sitePartnerRepository.findAllByOrderBySortOrderAscCreatedAtAsc();
        return partners.stream().map(SiteMapper::toPartnerResponse).toList();
    }

    @Transactional
    public SitePartnerResponse createPartner(SitePartnerRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        Media logo = mediaService.requireActiveImage(request.logoMediaId());
        SitePartner partner = sitePartnerRepository.save(new SitePartner(
                normalizeRequired(request.name(), "Partner name is required.", 160),
                logo,
                safeUrlValidator.normalizeOptionalUrl(request.websiteUrl()),
                request.sortOrder(),
                request.active(),
                actor
        ));
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_PARTNER_CREATED, AuditTargetType.PARTNER, partner.getId(), null);
        return SiteMapper.toPartnerResponse(partner);
    }

    @Transactional
    public SitePartnerResponse updatePartner(UUID id, SitePartnerRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        SitePartner partner = sitePartnerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partner not found."));
        Media logo = mediaService.requireActiveImage(request.logoMediaId());
        partner.update(
                normalizeRequired(request.name(), "Partner name is required.", 160),
                logo,
                safeUrlValidator.normalizeOptionalUrl(request.websiteUrl()),
                request.sortOrder(),
                request.active(),
                actor
        );
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_PARTNER_UPDATED, AuditTargetType.PARTNER, partner.getId(), null);
        return SiteMapper.toPartnerResponse(partner);
    }

    @Transactional
    public void deletePartner(UUID id, UUID actorId) {
        User actor = requireUser(actorId);
        SitePartner partner = sitePartnerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partner not found."));
        sitePartnerRepository.delete(partner);
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_PARTNER_DELETED, AuditTargetType.PARTNER, id, null);
    }

    @Transactional(readOnly = true)
    public SiteSettingsResponse getSettings() {
        return siteSettingsRepository.findById(SiteSettings.SINGLETON_ID)
                .map(SiteMapper::toSettingsResponse)
                .orElseGet(() -> SiteMapper.toSettingsResponse(null));
    }

    @Transactional
    public SiteSettingsResponse updateSettings(SiteSettingsRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        SiteSettings settings = siteSettingsRepository.findById(SiteSettings.SINGLETON_ID)
                .orElseGet(() -> new SiteSettings(SiteSettings.SINGLETON_ID));
        Media logo = request.logoMediaId() == null ? null : mediaService.requireActiveImage(request.logoMediaId());
        settings.update(
                normalizeOptional(request.companyName(), 160),
                normalizeOptional(request.slogan(), 5000),
                normalizeOptional(request.vision(), 5000),
                normalizeOptional(request.mission(), 5000),
                normalizeOptional(request.philosophy(), 5000),
                normalizeOptional(request.brandNarrative(), 5000),
                normalizeOptional(request.coreValues(), 5000),
                logo,
                normalizeOptional(request.officeAddress(), 5000),
                normalizeOptional(request.factoryAddress(), 5000),
                normalizeOptional(request.phone(), 64),
                normalizeOptional(request.email(), 320),
                safeUrlValidator.normalizeOptionalUrl(request.facebookUrl()),
                safeUrlValidator.normalizeOptionalUrl(request.youtubeUrl()),
                safeUrlValidator.normalizeOptionalUrl(request.zaloUrl()),
                safeUrlValidator.normalizeOptionalUrl(request.mapUrl()),
                safeUrlValidator.normalizeOptionalUrl(request.tvcUrl()),
                actor
        );
        SiteSettings saved = siteSettingsRepository.save(settings);
        auditService.recordSuccessAfterCommit(actor, AuditAction.SITE_SETTINGS_UPDATED, AuditTargetType.SITE_SETTINGS, saved.getId(), null);
        return SiteMapper.toSettingsResponse(saved);
    }

    @Transactional(readOnly = true)
    public ManufacturingPageResponse getManufacturing(boolean publicOnly) {
        ManufacturingPage page = manufacturingPageRepository.findById(ManufacturingPage.SINGLETON_ID).orElse(null);
        List<ManufacturingSection> sections = publicOnly
                ? manufacturingSectionRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()
                : manufacturingSectionRepository.findAllByOrderBySortOrderAscCreatedAtAsc();
        return SiteMapper.toManufacturingPageResponse(page, sections);
    }

    @Transactional
    public ManufacturingPageResponse updateManufacturingPage(ManufacturingPageRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        ManufacturingPage page = manufacturingPageRepository.findById(ManufacturingPage.SINGLETON_ID)
                .orElseGet(() -> new ManufacturingPage(ManufacturingPage.SINGLETON_ID));
        Media hero = request.heroMediaId() == null ? null : mediaService.requireActiveImage(request.heroMediaId());
        page.update(normalizeOptional(request.title(), 160), normalizeOptional(request.introduction(), 5000), hero, actor);
        ManufacturingPage saved = manufacturingPageRepository.save(page);
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_PAGE_UPDATED, AuditTargetType.MANUFACTURING, saved.getId(), null);
        return getManufacturing(false);
    }

    @Transactional(readOnly = true)
    public List<ManufacturingSectionResponse> listManufacturingSections() {
        return manufacturingSectionRepository.findAllByOrderBySortOrderAscCreatedAtAsc()
                .stream()
                .map(SiteMapper::toManufacturingSectionResponse)
                .toList();
    }

    @Transactional
    public ManufacturingSectionResponse createManufacturingSection(ManufacturingSectionRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        Media media = request.mediaId() == null ? null : mediaService.requireActiveImage(request.mediaId());
        ManufacturingSection section = manufacturingSectionRepository.save(new ManufacturingSection(
                normalizeRequired(request.title(), "Section title is required.", 160),
                normalizeOptional(request.content(), 5000),
                media,
                request.sortOrder(),
                request.active(),
                actor
        ));
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SECTION_CREATED, AuditTargetType.MANUFACTURING, section.getId(), null);
        return SiteMapper.toManufacturingSectionResponse(section);
    }

    @Transactional
    public ManufacturingSectionResponse updateManufacturingSection(UUID id, ManufacturingSectionRequest request, UUID actorId) {
        User actor = requireUser(actorId);
        ManufacturingSection section = manufacturingSectionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturing section not found."));
        Media media = request.mediaId() == null ? null : mediaService.requireActiveImage(request.mediaId());
        section.update(
                normalizeRequired(request.title(), "Section title is required.", 160),
                normalizeOptional(request.content(), 5000),
                media,
                request.sortOrder(),
                request.active(),
                actor
        );
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SECTION_UPDATED, AuditTargetType.MANUFACTURING, section.getId(), null);
        return SiteMapper.toManufacturingSectionResponse(section);
    }

    @Transactional
    public void deleteManufacturingSection(UUID id, UUID actorId) {
        User actor = requireUser(actorId);
        ManufacturingSection section = manufacturingSectionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manufacturing section not found."));
        manufacturingSectionRepository.delete(section);
        auditService.recordSuccessAfterCommit(actor, AuditAction.MANUFACTURING_SECTION_DELETED, AuditTargetType.MANUFACTURING, id, null);
    }

    private User requireUser(UUID actorId) {
        return userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized."));
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.length() <= maxLength ? trimmedValue : trimmedValue.substring(0, maxLength);
    }

    private String normalizeRequired(String value, String message, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return normalizeOptional(value, maxLength);
    }
}

package com.dongbacsaigon.backend.site.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.audit.entity.AuditAction;
import com.dongbacsaigon.backend.audit.entity.AuditTargetType;
import com.dongbacsaigon.backend.audit.service.AuditService;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.site.dto.SiteBannerRequest;
import com.dongbacsaigon.backend.site.dto.SiteBannerResponse;
import com.dongbacsaigon.backend.site.dto.SitePartnerRequest;
import com.dongbacsaigon.backend.site.entity.SiteBanner;
import com.dongbacsaigon.backend.site.entity.SitePartner;
import com.dongbacsaigon.backend.site.repository.ManufacturingPageRepository;
import com.dongbacsaigon.backend.site.repository.ManufacturingSectionRepository;
import com.dongbacsaigon.backend.site.repository.SiteBannerRepository;
import com.dongbacsaigon.backend.site.repository.SitePartnerRepository;
import com.dongbacsaigon.backend.site.repository.SiteSettingsRepository;
import com.dongbacsaigon.backend.user.entity.User;
import com.dongbacsaigon.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

class SiteManagementServiceTest {

    private final SiteBannerRepository siteBannerRepository = mock(SiteBannerRepository.class);
    private final SitePartnerRepository sitePartnerRepository = mock(SitePartnerRepository.class);
    private final SiteSettingsRepository siteSettingsRepository = mock(SiteSettingsRepository.class);
    private final ManufacturingPageRepository manufacturingPageRepository = mock(ManufacturingPageRepository.class);
    private final ManufacturingSectionRepository manufacturingSectionRepository = mock(ManufacturingSectionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MediaService mediaService = mock(MediaService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final SiteManagementService siteManagementService = new SiteManagementService(
            siteBannerRepository,
            sitePartnerRepository,
            siteSettingsRepository,
            manufacturingPageRepository,
            manufacturingSectionRepository,
            userRepository,
            mediaService,
            new SafeUrlValidator(),
            auditService
    );

    @Test
    void createBannerRequiresActiveImageAndAuditsChange() {
        User admin = admin();
        Media media = imageMedia(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(mediaService.requireActiveImage(media.getId())).thenReturn(media);
        when(siteBannerRepository.save(any(SiteBanner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SiteBannerResponse response = siteManagementService.createBanner(
                new SiteBannerRequest(media.getId(), " Homepage hero ", 2, true),
                admin.getId()
        );

        assertThat(response.media().id()).isEqualTo(media.getId());
        assertThat(response.altText()).isEqualTo("Homepage hero");
        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.active()).isTrue();
        verify(mediaService).requireActiveImage(media.getId());
        verify(auditService).recordSuccessAfterCommit(
                admin,
                AuditAction.SITE_BANNER_CREATED,
                AuditTargetType.BANNER,
                response.id(),
                null
        );
    }

    @Test
    void createPartnerRejectsUnsafeWebsiteUrl() {
        User admin = admin();
        Media media = imageMedia(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(mediaService.requireActiveImage(media.getId())).thenReturn(media);

        assertThatThrownBy(() -> siteManagementService.createPartner(
                new SitePartnerRequest("Partner", media.getId(), "javascript:alert(1)", 0, true),
                admin.getId()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("URL must use http or https.");

        verify(sitePartnerRepository, never()).save(any(SitePartner.class));
    }

    @Test
    void publicBannerListReadsOnlyActiveBanners() {
        User admin = admin();
        SiteBanner banner = new SiteBanner(imageMedia(admin), "Hero", 1, true, admin);
        when(siteBannerRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()).thenReturn(List.of(banner));

        List<SiteBannerResponse> responses = siteManagementService.listBanners(true);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).active()).isTrue();
        verify(siteBannerRepository, never()).findAllByOrderBySortOrderAscCreatedAtAsc();
    }

    @Test
    void siteReferenceCheckerDetectsMediaUsageAcrossSiteModules() {
        UUID mediaId = UUID.randomUUID();
        SiteMediaReferenceChecker checker = new SiteMediaReferenceChecker(
                siteBannerRepository,
                sitePartnerRepository,
                siteSettingsRepository,
                manufacturingPageRepository,
                manufacturingSectionRepository
        );
        when(siteBannerRepository.existsByMediaId(mediaId)).thenReturn(false);
        when(sitePartnerRepository.existsByLogoMediaId(mediaId)).thenReturn(true);

        assertThat(checker.isReferenced(mediaId)).isTrue();
    }

    @Test
    void safeUrlValidatorAllowsOnlyHttpAndHttpsUrls() {
        SafeUrlValidator validator = new SafeUrlValidator();

        assertThat(validator.normalizeOptionalUrl(" https://dongbacsaigon.vn "))
                .isEqualTo("https://dongbacsaigon.vn");
        assertThatThrownBy(() -> validator.normalizeOptionalUrl("ftp://dongbacsaigon.vn/file"))
                .isInstanceOf(ApiException.class)
                .hasMessage("URL must use http or https.");
    }

    private User admin() {
        return User.admin("admin@example.com", "$2a$12$hash", "Admin");
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
}

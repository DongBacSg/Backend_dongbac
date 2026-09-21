package com.dongbacsaigon.backend.site.service;

import java.util.List;

import com.dongbacsaigon.backend.media.service.MediaMapper;
import com.dongbacsaigon.backend.site.dto.ManufacturingPageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingSectionResponse;
import com.dongbacsaigon.backend.site.dto.SiteBannerResponse;
import com.dongbacsaigon.backend.site.dto.SitePartnerResponse;
import com.dongbacsaigon.backend.site.dto.SiteSettingsResponse;
import com.dongbacsaigon.backend.site.entity.ManufacturingPage;
import com.dongbacsaigon.backend.site.entity.ManufacturingSection;
import com.dongbacsaigon.backend.site.entity.SiteBanner;
import com.dongbacsaigon.backend.site.entity.SitePartner;
import com.dongbacsaigon.backend.site.entity.SiteSettings;

public final class SiteMapper {

    private SiteMapper() {
    }

    public static SiteBannerResponse toBannerResponse(SiteBanner banner) {
        return new SiteBannerResponse(
                banner.getId(),
                MediaMapper.toPublicResponse(banner.getMedia()),
                banner.getAltText(),
                banner.getSortOrder(),
                banner.isActive(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }

    public static SitePartnerResponse toPartnerResponse(SitePartner partner) {
        return new SitePartnerResponse(
                partner.getId(),
                partner.getName(),
                MediaMapper.toPublicResponse(partner.getLogoMedia()),
                partner.getWebsiteUrl(),
                partner.getSortOrder(),
                partner.isActive(),
                partner.getCreatedAt(),
                partner.getUpdatedAt()
        );
    }

    public static SiteSettingsResponse toSettingsResponse(SiteSettings settings) {
        if (settings == null) {
            return new SiteSettingsResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new SiteSettingsResponse(
                settings.getId(),
                settings.getCompanyName(),
                settings.getSlogan(),
                settings.getVision(),
                settings.getMission(),
                settings.getPhilosophy(),
                settings.getBrandNarrative(),
                settings.getCoreValues(),
                MediaMapper.toPublicResponse(settings.getLogoMedia()),
                settings.getOfficeAddress(),
                settings.getFactoryAddress(),
                settings.getPhone(),
                settings.getEmail(),
                settings.getFacebookUrl(),
                settings.getYoutubeUrl(),
                settings.getZaloUrl(),
                settings.getMapUrl(),
                settings.getTvcUrl(),
                settings.getUpdatedAt()
        );
    }

    public static ManufacturingSectionResponse toManufacturingSectionResponse(ManufacturingSection section) {
        return new ManufacturingSectionResponse(
                section.getId(),
                section.getTitle(),
                section.getContent(),
                MediaMapper.toPublicResponse(section.getMedia()),
                section.getSortOrder(),
                section.isActive(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }

    public static ManufacturingPageResponse toManufacturingPageResponse(
            ManufacturingPage page,
            List<ManufacturingSection> sections
    ) {
        List<ManufacturingSectionResponse> sectionResponses = sections.stream()
                .map(SiteMapper::toManufacturingSectionResponse)
                .toList();
        if (page == null) {
            return new ManufacturingPageResponse(null, null, null, null, sectionResponses, null);
        }
        return new ManufacturingPageResponse(
                page.getId(),
                page.getTitle(),
                page.getIntroduction(),
                MediaMapper.toPublicResponse(page.getHeroMedia()),
                sectionResponses,
                page.getUpdatedAt()
        );
    }
}

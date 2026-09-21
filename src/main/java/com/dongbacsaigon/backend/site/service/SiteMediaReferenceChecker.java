package com.dongbacsaigon.backend.site.service;

import java.util.UUID;

import com.dongbacsaigon.backend.media.service.MediaReferenceChecker;
import com.dongbacsaigon.backend.site.repository.ManufacturingPageRepository;
import com.dongbacsaigon.backend.site.repository.ManufacturingSectionRepository;
import com.dongbacsaigon.backend.site.repository.SiteBannerRepository;
import com.dongbacsaigon.backend.site.repository.SitePartnerRepository;
import com.dongbacsaigon.backend.site.repository.SiteSettingsRepository;
import org.springframework.stereotype.Component;

@Component
class SiteMediaReferenceChecker implements MediaReferenceChecker {

    private final SiteBannerRepository siteBannerRepository;
    private final SitePartnerRepository sitePartnerRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final ManufacturingPageRepository manufacturingPageRepository;
    private final ManufacturingSectionRepository manufacturingSectionRepository;

    SiteMediaReferenceChecker(
            SiteBannerRepository siteBannerRepository,
            SitePartnerRepository sitePartnerRepository,
            SiteSettingsRepository siteSettingsRepository,
            ManufacturingPageRepository manufacturingPageRepository,
            ManufacturingSectionRepository manufacturingSectionRepository
    ) {
        this.siteBannerRepository = siteBannerRepository;
        this.sitePartnerRepository = sitePartnerRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.manufacturingPageRepository = manufacturingPageRepository;
        this.manufacturingSectionRepository = manufacturingSectionRepository;
    }

    @Override
    public boolean isReferenced(UUID mediaId) {
        return siteBannerRepository.existsByMediaId(mediaId)
                || sitePartnerRepository.existsByLogoMediaId(mediaId)
                || siteSettingsRepository.existsByLogoMediaId(mediaId)
                || manufacturingPageRepository.existsByHeroMediaId(mediaId)
                || manufacturingSectionRepository.existsByMediaId(mediaId);
    }
}

package com.dongbacsaigon.backend.site.controller;

import java.util.List;

import com.dongbacsaigon.backend.site.dto.ManufacturingPageResponse;
import com.dongbacsaigon.backend.site.dto.SiteBannerResponse;
import com.dongbacsaigon.backend.site.dto.SitePartnerResponse;
import com.dongbacsaigon.backend.site.dto.SiteSettingsResponse;
import com.dongbacsaigon.backend.site.service.SiteManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/site")
@Tag(name = "Public Site Content")
class PublicSiteController {

    private final SiteManagementService siteManagementService;

    PublicSiteController(SiteManagementService siteManagementService) {
        this.siteManagementService = siteManagementService;
    }

    @GetMapping("/banners")
    @Operation(summary = "List active public banners")
    List<SiteBannerResponse> listPublicBanners() {
        return siteManagementService.listBanners(true);
    }

    @GetMapping("/partners")
    @Operation(summary = "List active public partners")
    List<SitePartnerResponse> listPublicPartners() {
        return siteManagementService.listPartners(true);
    }

    @GetMapping("/settings")
    @Operation(summary = "Get public site settings")
    SiteSettingsResponse getPublicSettings() {
        return siteManagementService.getSettings();
    }

    @GetMapping("/manufacturing")
    @Operation(summary = "Get public manufacturing page")
    ManufacturingPageResponse getPublicManufacturing() {
        return siteManagementService.getManufacturing(true);
    }
}

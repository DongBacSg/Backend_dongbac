package com.dongbacsaigon.backend.site.controller;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.common.response.MessageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingPageRequest;
import com.dongbacsaigon.backend.site.dto.ManufacturingPageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingSectionRequest;
import com.dongbacsaigon.backend.site.dto.ManufacturingSectionResponse;
import com.dongbacsaigon.backend.site.dto.AdminManufacturingServiceResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingServicePageResponse;
import com.dongbacsaigon.backend.site.dto.ManufacturingServiceRequest;
import com.dongbacsaigon.backend.site.dto.SiteBannerRequest;
import com.dongbacsaigon.backend.site.dto.SiteBannerResponse;
import com.dongbacsaigon.backend.site.dto.SitePartnerRequest;
import com.dongbacsaigon.backend.site.dto.SitePartnerResponse;
import com.dongbacsaigon.backend.site.dto.SiteSettingsRequest;
import com.dongbacsaigon.backend.site.dto.SiteSettingsResponse;
import com.dongbacsaigon.backend.site.service.SiteManagementService;
import com.dongbacsaigon.backend.site.service.ManufacturingServiceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Site Management")
class AdminSiteController {

    private final SiteManagementService siteManagementService;
    private final ManufacturingServiceManager manufacturingServiceManager;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    AdminSiteController(
            SiteManagementService siteManagementService,
            ManufacturingServiceManager manufacturingServiceManager,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.siteManagementService = siteManagementService;
        this.manufacturingServiceManager = manufacturingServiceManager;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/banners")
    @Operation(summary = "List banners", description = "ADMIN only.")
    List<SiteBannerResponse> listBanners() {
        return siteManagementService.listBanners(false);
    }

    @PostMapping("/banners")
    @Operation(summary = "Create banner", description = "ADMIN only.")
    SiteBannerResponse createBanner(@Valid @RequestBody SiteBannerRequest request, Authentication authentication) {
        return siteManagementService.createBanner(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/banners/{id}")
    @Operation(summary = "Update banner", description = "ADMIN only.")
    SiteBannerResponse updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody SiteBannerRequest request,
            Authentication authentication
    ) {
        return siteManagementService.updateBanner(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/banners/{id}")
    @Operation(summary = "Delete banner", description = "ADMIN only.")
    MessageResponse deleteBanner(@PathVariable UUID id, Authentication authentication) {
        siteManagementService.deleteBanner(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Banner deleted.");
    }

    @GetMapping("/partners")
    @Operation(summary = "List partners", description = "ADMIN only.")
    List<SitePartnerResponse> listPartners() {
        return siteManagementService.listPartners(false);
    }

    @PostMapping("/partners")
    @Operation(summary = "Create partner", description = "ADMIN only.")
    SitePartnerResponse createPartner(@Valid @RequestBody SitePartnerRequest request, Authentication authentication) {
        return siteManagementService.createPartner(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/partners/{id}")
    @Operation(summary = "Update partner", description = "ADMIN only.")
    SitePartnerResponse updatePartner(
            @PathVariable UUID id,
            @Valid @RequestBody SitePartnerRequest request,
            Authentication authentication
    ) {
        return siteManagementService.updatePartner(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/partners/{id}")
    @Operation(summary = "Delete partner", description = "ADMIN only.")
    MessageResponse deletePartner(@PathVariable UUID id, Authentication authentication) {
        siteManagementService.deletePartner(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Partner deleted.");
    }

    @GetMapping("/settings")
    @Operation(summary = "Get site settings", description = "ADMIN only.")
    SiteSettingsResponse getSettings() {
        return siteManagementService.getSettings();
    }

    @PutMapping("/settings")
    @Operation(summary = "Update site settings", description = "ADMIN only.")
    SiteSettingsResponse updateSettings(@Valid @RequestBody SiteSettingsRequest request, Authentication authentication) {
        return siteManagementService.updateSettings(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @GetMapping("/manufacturing")
    @Operation(summary = "Get manufacturing page", description = "ADMIN only.")
    ManufacturingPageResponse getManufacturing() {
        return siteManagementService.getManufacturing(false);
    }

    @PutMapping("/manufacturing")
    @Operation(summary = "Update manufacturing page", description = "ADMIN only.")
    ManufacturingPageResponse updateManufacturingPage(
            @Valid @RequestBody ManufacturingPageRequest request,
            Authentication authentication
    ) {
        return siteManagementService.updateManufacturingPage(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @GetMapping("/manufacturing/sections")
    @Operation(summary = "List manufacturing sections", description = "ADMIN only.")
    List<ManufacturingSectionResponse> listManufacturingSections() {
        return siteManagementService.listManufacturingSections();
    }

    @PostMapping("/manufacturing/sections")
    @Operation(summary = "Create manufacturing section", description = "ADMIN only.")
    ManufacturingSectionResponse createManufacturingSection(
            @Valid @RequestBody ManufacturingSectionRequest request,
            Authentication authentication
    ) {
        return siteManagementService.createManufacturingSection(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/manufacturing/sections/{id}")
    @Operation(summary = "Update manufacturing section", description = "ADMIN only.")
    ManufacturingSectionResponse updateManufacturingSection(
            @PathVariable UUID id,
            @Valid @RequestBody ManufacturingSectionRequest request,
            Authentication authentication
    ) {
        return siteManagementService.updateManufacturingSection(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/manufacturing/sections/{id}")
    @Operation(summary = "Delete manufacturing section", description = "ADMIN only.")
    MessageResponse deleteManufacturingSection(@PathVariable UUID id, Authentication authentication) {
        siteManagementService.deleteManufacturingSection(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Manufacturing section deleted.");
    }

    @GetMapping("/manufacturing/services")
    @Operation(summary = "List manufacturing services", description = "ADMIN only. Search title or slug and filter active status.")
    ManufacturingServicePageResponse listManufacturingServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return manufacturingServiceManager.listAdmin(page, size, search, active);
    }

    @GetMapping("/manufacturing/services/{id}")
    @Operation(summary = "Get manufacturing service", description = "ADMIN only.")
    AdminManufacturingServiceResponse getManufacturingService(@PathVariable UUID id) {
        return manufacturingServiceManager.getAdmin(id);
    }

    @PostMapping("/manufacturing/services")
    @Operation(summary = "Create manufacturing service", description = "ADMIN only. Featured media must be an active image.")
    AdminManufacturingServiceResponse createManufacturingService(
            @Valid @RequestBody ManufacturingServiceRequest request,
            Authentication authentication
    ) {
        return manufacturingServiceManager.create(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PatchMapping("/manufacturing/services/{id}")
    @Operation(summary = "Update manufacturing service", description = "ADMIN only.")
    AdminManufacturingServiceResponse updateManufacturingService(
            @PathVariable UUID id,
            @Valid @RequestBody ManufacturingServiceRequest request,
            Authentication authentication
    ) {
        return manufacturingServiceManager.update(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/manufacturing/services/{id}")
    @Operation(summary = "Delete manufacturing service", description = "ADMIN only. Does not delete featured Media.")
    MessageResponse deleteManufacturingService(@PathVariable UUID id, Authentication authentication) {
        manufacturingServiceManager.delete(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Manufacturing service deleted.");
    }
}

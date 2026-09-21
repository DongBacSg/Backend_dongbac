package com.dongbacsaigon.backend.media.controller;

import java.util.UUID;

import com.dongbacsaigon.backend.auth.security.AuthenticatedUserProvider;
import com.dongbacsaigon.backend.common.response.MessageResponse;
import com.dongbacsaigon.backend.media.dto.MediaPageResponse;
import com.dongbacsaigon.backend.media.dto.MediaResponse;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadRequest;
import com.dongbacsaigon.backend.media.dto.MediaSignedUploadResponse;
import com.dongbacsaigon.backend.media.dto.MediaUpdateRequest;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.service.MediaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Media Library")
class MediaController {

    private final MediaService mediaService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    MediaController(MediaService mediaService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.mediaService = mediaService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/uploads/signature")
    @Operation(summary = "Create signed Cloudinary direct-upload parameters", description = "ADMIN/STAFF.")
    MediaSignedUploadResponse createSignedUpload(
            @Valid @RequestBody MediaSignedUploadRequest request,
            Authentication authentication
    ) {
        return mediaService.createSignedUpload(request, authenticatedUserProvider.requireUserId(authentication));
    }

    @PostMapping("/uploads/{intentId}/complete")
    @Operation(summary = "Verify Cloudinary upload and register media metadata", description = "ADMIN/STAFF.")
    MediaResponse completeUpload(@PathVariable UUID intentId, Authentication authentication) {
        return mediaService.completeUpload(intentId, authenticatedUserProvider.requireUserId(authentication));
    }

    @GetMapping
    @Operation(summary = "List media metadata", description = "ADMIN/STAFF.")
    MediaPageResponse listMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) MediaStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID uploadedBy
    ) {
        return mediaService.listMedia(page, size, mediaType, status, search, uploadedBy);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media metadata", description = "ADMIN/STAFF.")
    MediaResponse getMedia(@PathVariable UUID id) {
        return mediaService.getMedia(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update safe media metadata", description = "ADMIN/STAFF. STAFF can update owned media only.")
    MediaResponse updateMedia(
            @PathVariable UUID id,
            @Valid @RequestBody MediaUpdateRequest request,
            Authentication authentication
    ) {
        return mediaService.updateMedia(id, request, authenticatedUserProvider.requireUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete unused active media", description = "ADMIN can delete any unused media; STAFF can delete owned unused media.")
    MessageResponse deleteMedia(@PathVariable UUID id, Authentication authentication) {
        mediaService.deleteMedia(id, authenticatedUserProvider.requireUserId(authentication));
        return new MessageResponse("Media deleted.");
    }
}

package com.dongbacsaigon.backend.media.service;

import java.util.Locale;
import java.util.Set;

import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.cloudinary.CloudinaryAsset;
import com.dongbacsaigon.backend.media.config.MediaProperties;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import com.dongbacsaigon.backend.media.entity.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MediaValidationService {

    private static final Set<String> ALLOWED_IMAGE_FORMATS = Set.of("jpg", "jpeg", "png", "webp", "avif");
    private static final Set<String> ALLOWED_VIDEO_FORMATS = Set.of("mp4", "webm", "mov");

    private final MediaProperties mediaProperties;

    public MediaValidationService(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    public void validateCloudinaryAsset(
            CloudinaryAsset asset,
            MediaType expectedMediaType,
            String expectedPublicId,
            String expectedFolder
    ) {
        if (asset == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Cloudinary resource was not found.");
        }
        if (!expectedPublicId.equals(asset.publicId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Uploaded asset public id does not match the upload intent.");
        }
        if (asset.resourceType() != expectedMediaType) {
            throw new ApiException(HttpStatus.CONFLICT, "Uploaded asset resource type does not match the upload intent.");
        }
        if (asset.folder() != null && !expectedFolder.equals(asset.folder())) {
            throw new ApiException(HttpStatus.CONFLICT, "Uploaded asset folder does not match the upload intent.");
        }
        validateFormat(expectedMediaType, asset.format());
        validateSize(expectedMediaType, asset.bytes());
    }

    public void requireActiveImage(Media media, String message) {
        if (media == null || media.getStatus() != MediaStatus.ACTIVE || media.getResourceType() != MediaType.IMAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public void requireActiveMedia(Media media, String message) {
        if (media == null || media.getStatus() != MediaStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateFormat(MediaType mediaType, String format) {
        if (format == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Uploaded asset format is missing.");
        }
        String normalizedFormat = format.trim().toLowerCase(Locale.ROOT);
        boolean allowed = mediaType == MediaType.IMAGE
                ? ALLOWED_IMAGE_FORMATS.contains(normalizedFormat)
                : ALLOWED_VIDEO_FORMATS.contains(normalizedFormat);
        if (!allowed) {
            throw new ApiException(HttpStatus.CONFLICT, "Uploaded asset format is not allowed.");
        }
    }

    private void validateSize(MediaType mediaType, long bytes) {
        long maxBytes = mediaType == MediaType.IMAGE
                ? mediaProperties.imageMaxBytes()
                : mediaProperties.videoMaxBytes();
        if (bytes > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded asset exceeds the configured size limit.");
        }
    }
}

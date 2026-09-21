package com.dongbacsaigon.backend.media.service;

import com.dongbacsaigon.backend.media.dto.MediaResponse;
import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.user.entity.User;

public final class MediaMapper {

    private MediaMapper() {
    }

    public static MediaResponse toResponse(Media media) {
        User uploadedBy = media.getUploadedBy();
        User deletedBy = media.getDeletedBy();
        return new MediaResponse(
                media.getId(),
                media.getPublicId(),
                media.getResourceType(),
                media.getFormat(),
                media.getSecureUrl(),
                media.getWidth(),
                media.getHeight(),
                media.getBytes(),
                media.getDurationSeconds(),
                media.getOriginalFilename(),
                media.getAltText(),
                media.getStatus(),
                uploadedBy == null ? null : uploadedBy.getId(),
                media.getCreatedAt(),
                media.getUpdatedAt(),
                media.getDeletedAt(),
                deletedBy == null ? null : deletedBy.getId()
        );
    }

    public static PublicMediaResponse toPublicResponse(Media media) {
        if (media == null) {
            return null;
        }
        return new PublicMediaResponse(
                media.getId(),
                media.getSecureUrl(),
                media.getResourceType(),
                media.getFormat(),
                media.getWidth(),
                media.getHeight(),
                media.getAltText()
        );
    }
}

package com.dongbacsaigon.backend.media.dto;

import com.dongbacsaigon.backend.media.entity.MediaType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MediaSignedUploadRequest(
        @NotNull
        MediaType mediaType,

        @Size(max = 255)
        String originalFilename
) {
}

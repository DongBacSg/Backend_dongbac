package com.dongbacsaigon.backend.media.dto;

import jakarta.validation.constraints.Size;

public record MediaUpdateRequest(
        @Size(max = 255)
        String altText
) {
}

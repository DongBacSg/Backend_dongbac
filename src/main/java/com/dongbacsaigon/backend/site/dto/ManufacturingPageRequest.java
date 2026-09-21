package com.dongbacsaigon.backend.site.dto;

import java.util.UUID;

import jakarta.validation.constraints.Size;

public record ManufacturingPageRequest(
        @Size(max = 160)
        String title,
        String introduction,
        UUID heroMediaId
) {
}

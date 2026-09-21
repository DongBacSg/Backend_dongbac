package com.dongbacsaigon.backend.catalog.dto;

import java.util.UUID;

public record RelatedProductResponse(UUID productId, int sortOrder) {
}

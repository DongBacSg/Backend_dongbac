package com.dongbacsaigon.backend.catalog.dto;

import java.util.UUID;

public record CatalogUserSummary(UUID id, String email, String fullName) {
}

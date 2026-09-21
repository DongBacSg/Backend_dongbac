package com.dongbacsaigon.backend.lead.dto;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.dto.CatalogUserSummary;
import com.dongbacsaigon.backend.lead.entity.LeadStatus;

public record LeadSummaryResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        String companyName,
        LeadStatus status,
        CatalogUserSummary assignedTo,
        CatalogUserSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}

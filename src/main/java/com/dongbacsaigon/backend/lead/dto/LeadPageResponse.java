package com.dongbacsaigon.backend.lead.dto;

import java.util.List;

public record LeadPageResponse(
        List<LeadSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

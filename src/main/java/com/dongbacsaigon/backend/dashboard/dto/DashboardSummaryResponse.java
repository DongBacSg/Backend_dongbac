package com.dongbacsaigon.backend.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardSummaryResponse(
        LeadCounts leads,
        ApprovalCounts approvals,
        PublicationCounts products,
        PublicationCounts articles,
        MediaCounts media
) {
    public record LeadCounts(
            long total,
            @JsonProperty("new") long newCount,
            long contacted,
            long inProgress,
            long completed,
            long cancelled,
            long spam
    ) {
    }

    public record ApprovalCounts(long pending) {
    }

    public record PublicationCounts(long total, long published, long draft, long unpublished, long archived) {
    }

    public record MediaCounts(long active) {
    }
}

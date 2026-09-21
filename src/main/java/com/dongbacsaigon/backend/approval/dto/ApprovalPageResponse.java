package com.dongbacsaigon.backend.approval.dto;

import java.util.List;

public record ApprovalPageResponse(
        List<ApprovalRequestResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

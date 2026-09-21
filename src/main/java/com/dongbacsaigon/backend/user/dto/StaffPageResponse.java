package com.dongbacsaigon.backend.user.dto;

import java.util.List;

public record StaffPageResponse(
        List<StaffAccountResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

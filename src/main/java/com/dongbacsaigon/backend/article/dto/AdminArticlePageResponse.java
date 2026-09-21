package com.dongbacsaigon.backend.article.dto;

import java.util.List;

public record AdminArticlePageResponse(
        List<AdminArticleListItemResponse> content, int page, int size, long totalElements, int totalPages
) {
}

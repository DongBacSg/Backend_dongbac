package com.dongbacsaigon.backend.article.dto;

import java.util.List;

public record PublicArticlePageResponse(
        List<PublicArticleSummaryResponse> content, int page, int size, long totalElements, int totalPages
) {
}

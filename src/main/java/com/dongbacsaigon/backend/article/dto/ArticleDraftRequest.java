package com.dongbacsaigon.backend.article.dto;

import java.util.List;

import com.dongbacsaigon.backend.article.entity.ArticleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArticleDraftRequest(
        @NotNull ArticleType articleType,
        @NotBlank @Size(max = 240) String title,
        @Size(max = 260) String slug,
        @Size(max = 2000) String summary,
        @NotBlank @Size(max = 200000) String content,
        @Size(max = 240) String seoTitle,
        @Size(max = 500) String seoDescription,
        @Valid List<ArticleMediaRequest> media
) {
}

package com.dongbacsaigon.backend.article.dto;

import com.dongbacsaigon.backend.article.entity.ArticleMediaUsageType;
import com.dongbacsaigon.backend.media.dto.PublicMediaResponse;

public record ArticleMediaResponse(PublicMediaResponse media, ArticleMediaUsageType usageType, int sortOrder, String caption) {
}

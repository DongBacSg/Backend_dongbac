package com.dongbacsaigon.backend.catalog.service;

import java.util.List;

import com.dongbacsaigon.backend.catalog.dto.CatalogUserSummary;
import com.dongbacsaigon.backend.catalog.dto.CategoryResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductImageResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionResponse;
import com.dongbacsaigon.backend.catalog.dto.ProductRevisionSummaryResponse;
import com.dongbacsaigon.backend.catalog.dto.RelatedProductResponse;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionImage;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionRelatedProduct;
import com.dongbacsaigon.backend.media.service.MediaMapper;
import com.dongbacsaigon.backend.user.entity.User;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public static ProductRevisionSummaryResponse toRevisionSummary(ProductRevision revision) {
        if (revision == null) {
            return null;
        }
        return new ProductRevisionSummaryResponse(
                revision.getId(),
                revision.getRevisionNumber(),
                revision.getStatus(),
                revision.getName(),
                revision.getSlug(),
                toCategoryResponse(revision.getCategory()),
                toUserSummary(revision.getCreatedBy()),
                revision.getCreatedAt(),
                revision.getUpdatedAt(),
                revision.getPublishedAt()
        );
    }

    public static ProductRevisionResponse toRevisionResponse(
            ProductRevision revision,
            List<ProductRevisionImage> images,
            List<ProductRevisionRelatedProduct> relatedProducts
    ) {
        return new ProductRevisionResponse(
                revision.getId(),
                revision.getProduct().getId(),
                revision.getRevisionNumber(),
                revision.getStatus(),
                toCategoryResponse(revision.getCategory()),
                revision.getName(),
                revision.getSlug(),
                revision.getShortDescription(),
                revision.getContent(),
                revision.getSeoTitle(),
                revision.getSeoDescription(),
                images.stream().map(CatalogMapper::toImageResponse).toList(),
                relatedProducts.stream()
                        .map(relationship -> new RelatedProductResponse(
                                relationship.getRelatedProduct().getId(),
                                relationship.getSortOrder()
                        ))
                        .toList(),
                toUserSummary(revision.getCreatedBy()),
                revision.getCreatedAt(),
                revision.getUpdatedAt(),
                revision.getPublishedAt()
        );
    }

    public static ProductImageResponse toImageResponse(ProductRevisionImage image) {
        return new ProductImageResponse(
                MediaMapper.toPublicResponse(image.getMedia()),
                image.getSortOrder(),
                image.isPrimary()
        );
    }

    public static CatalogUserSummary toUserSummary(User user) {
        return user == null ? null : new CatalogUserSummary(user.getId(), user.getEmail(), user.getFullName());
    }
}

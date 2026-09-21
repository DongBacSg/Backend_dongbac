package com.dongbacsaigon.backend.catalog.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dongbacsaigon.backend.catalog.dto.ProductImageResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductPageResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductSummaryResponse;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionImage;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionRelatedProduct;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.CategoryRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRelatedProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PublicCatalogService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRevisionRepository revisionRepository;
    private final ProductRevisionImageRepository imageRepository;
    private final ProductRevisionRelatedProductRepository relatedRepository;
    private final SlugService slugService;

    public PublicCatalogService(
            CategoryRepository categoryRepository,
            ProductRevisionRepository revisionRepository,
            ProductRevisionImageRepository imageRepository,
            ProductRevisionRelatedProductRepository relatedRepository,
            SlugService slugService
    ) {
        this.categoryRepository = categoryRepository;
        this.revisionRepository = revisionRepository;
        this.imageRepository = imageRepository;
        this.relatedRepository = relatedRepository;
        this.slugService = slugService;
    }

    @Transactional(readOnly = true)
    public PublicProductPageResponse list(int page, int size, String categorySlug, String search) {
        String normalizedCategory = null;
        if (StringUtils.hasText(categorySlug)) {
            normalizedCategory = slugService.normalize(categorySlug);
            if (categoryRepository.findBySlugAndActiveTrue(normalizedCategory).isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Category not found.");
            }
        }
        Page<ProductRevision> revisions = revisionRepository.findPublicPage(
                ProductPublicationStatus.PUBLISHED,
                ProductRevisionStatus.PUBLISHED,
                normalizedCategory,
                normalizeSearch(search),
                PageRequest.of(
                        validatePage(page),
                        validateSize(size),
                        Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by("name"))
                )
        );
        List<UUID> revisionIds = revisions.getContent().stream().map(ProductRevision::getId).toList();
        Map<UUID, List<ProductRevisionImage>> imagesByRevision = revisionIds.isEmpty()
                ? Map.of()
                : imageRepository.findByRevisionIdInOrderByRevisionIdAscSortOrderAsc(revisionIds)
                        .stream()
                        .collect(Collectors.groupingBy(image -> image.getRevision().getId()));
        List<PublicProductSummaryResponse> content = revisions.getContent().stream()
                .map(revision -> toSummary(revision, imagesByRevision.getOrDefault(revision.getId(), List.of())))
                .toList();
        return new PublicProductPageResponse(content, revisions.getNumber(), revisions.getSize(), revisions.getTotalElements(), revisions.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PublicProductResponse getBySlug(String productSlug) {
        String slug = slugService.normalize(productSlug);
        ProductRevision revision = revisionRepository.findPublicBySlug(
                slug,
                ProductPublicationStatus.PUBLISHED,
                ProductRevisionStatus.PUBLISHED
        ).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
        return toDetail(revision);
    }

    @Transactional(readOnly = true)
    public PublicProductResponse getByCategoryAndSlug(String categorySlug, String productSlug) {
        String normalizedCategory = slugService.normalize(categorySlug);
        PublicProductResponse response = getBySlug(productSlug);
        if (!response.category().slug().equals(normalizedCategory)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found in category.");
        }
        return response;
    }

    private PublicProductResponse toDetail(ProductRevision revision) {
        List<ProductRevisionImage> images = imageRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId());
        List<ProductImageResponse> imageResponses = images.stream().map(CatalogMapper::toImageResponse).toList();
        ProductImageResponse primaryImage = imageResponses.stream().filter(ProductImageResponse::primary).findFirst().orElse(null);
        List<ProductRevisionRelatedProduct> relationships = relatedRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId());
        List<UUID> relatedRevisionIds = relationships.stream()
                .map(ProductRevisionRelatedProduct::getRelatedProduct)
                .filter(this::isPublic)
                .map(Product::getCurrentPublishedRevision)
                .distinct()
                .map(ProductRevision::getId)
                .toList();
        Map<UUID, ProductRevisionImage> primaryByRevision = relatedRevisionIds.isEmpty()
                ? Map.of()
                : imageRepository.findByRevisionIdInOrderByRevisionIdAscSortOrderAsc(relatedRevisionIds)
                        .stream()
                        .filter(ProductRevisionImage::isPrimary)
                        .collect(Collectors.toMap(image -> image.getRevision().getId(), image -> image));
        List<PublicProductSummaryResponse> related = relationships.stream()
                .map(ProductRevisionRelatedProduct::getRelatedProduct)
                .filter(this::isPublic)
                .map(Product::getCurrentPublishedRevision)
                .map(relatedRevision -> toSummary(
                        relatedRevision,
                        primaryByRevision.get(relatedRevision.getId()) == null
                                ? List.of()
                                : List.of(primaryByRevision.get(relatedRevision.getId()))
                ))
                .toList();
        return new PublicProductResponse(
                revision.getProduct().getId(),
                revision.getName(),
                revision.getSlug(),
                revision.getShortDescription(),
                revision.getContent(),
                revision.getSeoTitle(),
                revision.getSeoDescription(),
                CatalogMapper.toCategoryResponse(revision.getCategory()),
                primaryImage,
                imageResponses,
                related,
                revision.getPublishedAt()
        );
    }

    private PublicProductSummaryResponse toSummary(ProductRevision revision, List<ProductRevisionImage> images) {
        ProductImageResponse primary = images.stream()
                .filter(ProductRevisionImage::isPrimary)
                .findFirst()
                .map(CatalogMapper::toImageResponse)
                .orElse(null);
        return new PublicProductSummaryResponse(
                revision.getProduct().getId(),
                revision.getName(),
                revision.getSlug(),
                revision.getShortDescription(),
                CatalogMapper.toCategoryResponse(revision.getCategory()),
                primary,
                revision.getPublishedAt()
        );
    }

    private boolean isPublic(Product product) {
        ProductRevision revision = product.getCurrentPublishedRevision();
        return product.getPublicationStatus() == ProductPublicationStatus.PUBLISHED
                && revision != null
                && revision.getStatus() == ProductRevisionStatus.PUBLISHED
                && revision.getCategory().isActive();
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0.");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size == 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100.");
        }
        return size;
    }
}

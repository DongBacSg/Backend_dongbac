package com.dongbacsaigon.backend.catalog.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.dto.ProductDraftRequest;
import com.dongbacsaigon.backend.catalog.dto.ProductImageRequest;
import com.dongbacsaigon.backend.catalog.dto.RelatedProductRequest;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionImage;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionRelatedProduct;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRelatedProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CatalogValidationService {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final ProductRevisionRepository revisionRepository;
    private final ProductRevisionImageRepository imageRepository;
    private final ProductRevisionRelatedProductRepository relatedRepository;
    private final MediaService mediaService;
    private final SlugService slugService;

    public CatalogValidationService(
            CategoryService categoryService,
            ProductRepository productRepository,
            ProductRevisionRepository revisionRepository,
            ProductRevisionImageRepository imageRepository,
            ProductRevisionRelatedProductRepository relatedRepository,
            MediaService mediaService,
            SlugService slugService
    ) {
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.revisionRepository = revisionRepository;
        this.imageRepository = imageRepository;
        this.relatedRepository = relatedRepository;
        this.mediaService = mediaService;
        this.slugService = slugService;
    }

    public ResolvedDraft resolveDraft(ProductDraftRequest request, UUID productId) {
        Category category = categoryService.requireActiveCategory(request.categoryId());
        String name = normalizeRequired(request.name(), "Product name is required.");
        String slug = slugService.normalize(StringUtils.hasText(request.slug()) ? request.slug() : name);
        requirePublishedSlugAvailable(slug, productId);

        List<ProductImageRequest> imageRequests = request.images() == null ? List.of() : request.images();
        Set<UUID> mediaIds = new HashSet<>();
        List<ResolvedImage> images = new ArrayList<>();
        long primaryCount = imageRequests.stream().filter(ProductImageRequest::primary).count();
        if (!imageRequests.isEmpty() && primaryCount != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A product revision with images must have exactly one primary image.");
        }
        for (ProductImageRequest imageRequest : imageRequests) {
            if (!mediaIds.add(imageRequest.mediaId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A media item cannot be added to the same revision more than once.");
            }
            Media media = mediaService.requireActiveImage(imageRequest.mediaId());
            images.add(new ResolvedImage(media, imageRequest.sortOrder(), imageRequest.primary()));
        }

        List<RelatedProductRequest> relatedRequests = request.relatedProducts() == null
                ? List.of()
                : request.relatedProducts();
        Set<UUID> relatedIds = new HashSet<>();
        List<ResolvedRelatedProduct> relatedProducts = new ArrayList<>();
        for (RelatedProductRequest relatedRequest : relatedRequests) {
            if (productId.equals(relatedRequest.productId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A product cannot be related to itself.");
            }
            if (!relatedIds.add(relatedRequest.productId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A related product cannot be added more than once.");
            }
            Product relatedProduct = productRepository.findById(relatedRequest.productId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Related product not found."));
            relatedProducts.add(new ResolvedRelatedProduct(relatedProduct, relatedRequest.sortOrder()));
        }

        return new ResolvedDraft(
                category,
                name,
                slug,
                normalizeOptional(request.shortDescription()),
                normalizeOptional(request.content()),
                normalizeOptional(request.seoTitle()),
                normalizeOptional(request.seoDescription()),
                images,
                relatedProducts
        );
    }

    public void validateStoredRevision(ProductRevision revision) {
        if (!revision.getCategory().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Product category is inactive.");
        }
        requirePublishedSlugAvailable(revision.getSlug(), revision.getProduct().getId());

        List<ProductRevisionImage> images = imageRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId());
        long primaryCount = images.stream().filter(ProductRevisionImage::isPrimary).count();
        if (!images.isEmpty() && primaryCount != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "A product revision with images must have exactly one primary image.");
        }
        Set<UUID> mediaIds = new HashSet<>();
        for (ProductRevisionImage image : images) {
            if (!mediaIds.add(image.getMedia().getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "Product revision contains duplicate images.");
            }
            mediaService.requireActiveImage(image.getMedia().getId());
        }

        Set<UUID> relatedIds = new HashSet<>();
        for (ProductRevisionRelatedProduct relationship : relatedRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId())) {
            UUID relatedId = relationship.getRelatedProduct().getId();
            if (relatedId.equals(revision.getProduct().getId()) || !relatedIds.add(relatedId)) {
                throw new ApiException(HttpStatus.CONFLICT, "Product revision contains invalid related products.");
            }
        }
    }

    public void requirePublishedSlugAvailable(String slug, UUID productId) {
        if (revisionRepository.existsByStatusAndSlugAndProductIdNot(ProductRevisionStatus.PUBLISHED, slug, productId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Product slug is already used by another published product.");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record ResolvedDraft(
            Category category,
            String name,
            String slug,
            String shortDescription,
            String content,
            String seoTitle,
            String seoDescription,
            List<ResolvedImage> images,
            List<ResolvedRelatedProduct> relatedProducts
    ) {
    }

    public record ResolvedImage(Media media, int sortOrder, boolean primary) {
    }

    public record ResolvedRelatedProduct(Product product, int sortOrder) {
    }
}

package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.dto.ProductDraftRequest;
import com.dongbacsaigon.backend.catalog.dto.ProductImageRequest;
import com.dongbacsaigon.backend.catalog.entity.Category;
import com.dongbacsaigon.backend.catalog.repository.ProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRelatedProductRepository;
import com.dongbacsaigon.backend.catalog.repository.ProductRevisionRepository;
import com.dongbacsaigon.backend.common.exception.ApiException;
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.media.service.MediaService;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;

class CatalogValidationServiceTest {

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductRevisionRepository revisionRepository = mock(ProductRevisionRepository.class);
    private final ProductRevisionImageRepository imageRepository = mock(ProductRevisionImageRepository.class);
    private final ProductRevisionRelatedProductRepository relatedRepository = mock(ProductRevisionRelatedProductRepository.class);
    private final MediaService mediaService = mock(MediaService.class);
    private final CatalogValidationService service = new CatalogValidationService(
            categoryService,
            productRepository,
            revisionRepository,
            imageRepository,
            relatedRepository,
            mediaService,
            new SlugService()
    );

    @Test
    void activeImageIsAcceptedAndOrderingIsPreserved() {
        User user = admin();
        Category category = new Category("Category", "category", null, 0, true, user);
        Media first = image(user, "first");
        Media second = image(user, "second");
        UUID productId = UUID.randomUUID();
        when(categoryService.requireActiveCategory(category.getId())).thenReturn(category);
        when(mediaService.requireActiveImage(first.getId())).thenReturn(first);
        when(mediaService.requireActiveImage(second.getId())).thenReturn(second);

        CatalogValidationService.ResolvedDraft draft = service.resolveDraft(
                request(category.getId(), List.of(
                        new ProductImageRequest(first.getId(), 0, true),
                        new ProductImageRequest(second.getId(), 2, false)
                )),
                productId
        );

        assertThat(draft.images()).extracting(CatalogValidationService.ResolvedImage::sortOrder)
                .containsExactly(0, 2);
    }

    @Test
    void twoPrimaryImagesAreRejected() {
        User user = admin();
        Category category = new Category("Category", "category", null, 0, true, user);
        when(categoryService.requireActiveCategory(category.getId())).thenReturn(category);

        assertThatThrownBy(() -> service.resolveDraft(
                request(category.getId(), List.of(
                        new ProductImageRequest(UUID.randomUUID(), 0, true),
                        new ProductImageRequest(UUID.randomUUID(), 1, true)
                )),
                UUID.randomUUID()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("A product revision with images must have exactly one primary image.");
    }

    @Test
    void duplicateImageIsRejected() {
        User user = admin();
        Category category = new Category("Category", "category", null, 0, true, user);
        UUID mediaId = UUID.randomUUID();
        when(categoryService.requireActiveCategory(category.getId())).thenReturn(category);

        assertThatThrownBy(() -> service.resolveDraft(
                request(category.getId(), List.of(
                        new ProductImageRequest(mediaId, 0, true),
                        new ProductImageRequest(mediaId, 1, false)
                )),
                UUID.randomUUID()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("A media item cannot be added to the same revision more than once.");
    }

    @Test
    void invalidMediaFromPhaseFourServiceIsRejected() {
        User user = admin();
        Category category = new Category("Category", "category", null, 0, true, user);
        UUID mediaId = UUID.randomUUID();
        when(categoryService.requireActiveCategory(category.getId())).thenReturn(category);
        when(mediaService.requireActiveImage(mediaId)).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Media must be an active image."
        ));

        assertThatThrownBy(() -> service.resolveDraft(
                request(category.getId(), List.of(new ProductImageRequest(mediaId, 0, true))),
                UUID.randomUUID()
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Media must be an active image.");
    }

    private ProductDraftRequest request(UUID categoryId, List<ProductImageRequest> images) {
        return new ProductDraftRequest(categoryId, "Product", null, null, null, null, null, images, List.of());
    }

    private User admin() {
        return User.admin("admin@example.com", "$2a$12$hash", "Admin");
    }

    private Media image(User user, String suffix) {
        return new Media(
                "asset-" + suffix,
                "dongbac/media/" + suffix,
                MediaType.IMAGE,
                "webp",
                "https://example.com/" + suffix + ".webp",
                800,
                600,
                1000,
                BigDecimal.ZERO,
                "dongbac/media",
                suffix + ".webp",
                user
        );
    }
}

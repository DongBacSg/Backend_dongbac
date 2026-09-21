package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.dto.PublicProductPageResponse;
import com.dongbacsaigon.backend.catalog.dto.PublicProductResponse;
import com.dongbacsaigon.backend.catalog.entity.Category;
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
import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaType;
import com.dongbacsaigon.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class PublicCatalogServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRevisionRepository revisionRepository = mock(ProductRevisionRepository.class);
    private final ProductRevisionImageRepository imageRepository = mock(ProductRevisionImageRepository.class);
    private final ProductRevisionRelatedProductRepository relatedRepository = mock(ProductRevisionRelatedProductRepository.class);
    private final PublicCatalogService service = new PublicCatalogService(
            categoryRepository,
            revisionRepository,
            imageRepository,
            relatedRepository,
            new SlugService()
    );

    @Test
    void publicListRequestsOnlyCurrentPublishedProducts() {
        Fixture fixture = publishedFixture("product");
        when(revisionRepository.findPublicPage(
                eq(ProductPublicationStatus.PUBLISHED),
                eq(ProductRevisionStatus.PUBLISHED),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(fixture.revision())));
        when(imageRepository.findByRevisionIdInOrderByRevisionIdAscSortOrderAsc(List.of(fixture.revision().getId())))
                .thenReturn(List.of());

        PublicProductPageResponse response = service.list(0, 20, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).slug()).isEqualTo("product");
        verify(revisionRepository).findPublicPage(
                eq(ProductPublicationStatus.PUBLISHED),
                eq(ProductRevisionStatus.PUBLISHED),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
    }

    @Test
    void detailReturnsSafeImageAndFiltersUnpublishedRelatedProduct() {
        Fixture fixture = publishedFixture("product");
        Media media = image(fixture.admin());
        ProductRevisionImage image = new ProductRevisionImage(fixture.revision(), media, 0, true);

        Product unpublished = new Product(fixture.admin());
        ProductRevisionRelatedProduct relationship = new ProductRevisionRelatedProduct(fixture.revision(), unpublished, 0);
        when(revisionRepository.findPublicBySlug(
                "product",
                ProductPublicationStatus.PUBLISHED,
                ProductRevisionStatus.PUBLISHED
        )).thenReturn(Optional.of(fixture.revision()));
        when(imageRepository.findByRevisionIdOrderBySortOrderAsc(fixture.revision().getId())).thenReturn(List.of(image));
        when(relatedRepository.findByRevisionIdOrderBySortOrderAsc(fixture.revision().getId())).thenReturn(List.of(relationship));

        PublicProductResponse response = service.getBySlug("product");

        assertThat(response.primaryImage().media().id()).isEqualTo(media.getId());
        assertThat(response.primaryImage().media().secureUrl()).isEqualTo(media.getSecureUrl());
        assertThat(response.relatedProducts()).isEmpty();
    }

    private Fixture publishedFixture(String slug) {
        User admin = User.admin("admin@example.com", "$2a$12$hash", "Admin");
        Category category = new Category("Category", "category", null, 0, true, admin);
        Product product = new Product(admin);
        ProductRevision revision = new ProductRevision(
                product,
                product.nextRevisionNumber(),
                category,
                "Product",
                slug,
                "Short",
                "Content",
                null,
                null,
                admin
        );
        revision.publish(admin, Instant.now());
        product.publish(revision);
        return new Fixture(admin, revision);
    }

    private Media image(User user) {
        return new Media(
                "asset-id",
                "dongbac/media/product",
                MediaType.IMAGE,
                "webp",
                "https://example.com/product.webp",
                800,
                600,
                1000,
                BigDecimal.ZERO,
                "dongbac/media",
                "product.webp",
                user
        );
    }

    private record Fixture(User admin, ProductRevision revision) {
    }
}

package com.dongbacsaigon.backend.catalog.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductRevisionRelatedProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRevisionRelatedProductRepository
        extends JpaRepository<ProductRevisionRelatedProduct, UUID> {

    @EntityGraph(attributePaths = {
            "relatedProduct",
            "relatedProduct.currentPublishedRevision",
            "relatedProduct.currentPublishedRevision.category"
    })
    List<ProductRevisionRelatedProduct> findByRevisionIdOrderBySortOrderAsc(UUID revisionId);

    boolean existsByRelatedProductId(UUID productId);

    void deleteByRevisionId(UUID revisionId);
}

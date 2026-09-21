package com.dongbacsaigon.backend.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.Product;
import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("select product.publicationStatus as status, count(product) as count from Product product group by product.publicationStatus")
    List<ProductPublicationStatusCount> countGroupedByPublicationStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"createdBy", "currentPublishedRevision", "currentPublishedRevision.category"})
    Optional<Product> findDetailedById(UUID id);

    @Query("""
            select (count(product) > 0)
            from Product product
            join product.currentPublishedRevision revision
            where product.publicationStatus = :status
              and revision.category.id = :categoryId
            """)
    boolean existsPublicProductByCategory(
            @Param("categoryId") UUID categoryId,
            @Param("status") ProductPublicationStatus status
    );
}

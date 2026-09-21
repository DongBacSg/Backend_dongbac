package com.dongbacsaigon.backend.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;
import com.dongbacsaigon.backend.catalog.entity.ProductRevision;
import com.dongbacsaigon.backend.catalog.entity.ProductRevisionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRevisionRepository extends JpaRepository<ProductRevision, UUID> {

    @EntityGraph(attributePaths = {"product", "category", "createdBy", "publishedBy"})
    Optional<ProductRevision> findByIdAndProductId(UUID id, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select revision
            from ProductRevision revision
            join fetch revision.product
            join fetch revision.category
            join fetch revision.createdBy
            left join fetch revision.publishedBy
            where revision.product.id = :productId
              and revision.revisionNumber = :revisionNumber
            """)
    Optional<ProductRevision> findByProductAndNumberForUpdate(
            @Param("productId") UUID productId,
            @Param("revisionNumber") long revisionNumber
    );

    Optional<ProductRevision> findByProductIdAndRevisionNumber(UUID productId, long revisionNumber);

    @EntityGraph(attributePaths = {"category", "createdBy", "publishedBy"})
    List<ProductRevision> findByProductIdOrderByRevisionNumberDesc(UUID productId);

    @EntityGraph(attributePaths = {"category", "createdBy", "publishedBy"})
    Optional<ProductRevision> findFirstByProductIdOrderByRevisionNumberDesc(UUID productId);

    boolean existsByProductIdAndStatusIn(UUID productId, List<ProductRevisionStatus> statuses);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByStatusAndSlugAndProductIdNot(ProductRevisionStatus status, String slug, UUID productId);

    @EntityGraph(attributePaths = {"product", "product.currentPublishedRevision", "category", "createdBy", "publishedBy"})
    @Query("""
            select revision
            from ProductRevision revision
            where revision.revisionNumber = revision.product.latestRevisionNumber
              and (:status is null or revision.product.publicationStatus = :status)
              and (:revisionStatus is null or revision.status = :revisionStatus)
              and (:categoryId is null or revision.category.id = :categoryId)
              and (
                  :search is null
                  or lower(revision.name) like concat('%', :search, '%')
                  or lower(revision.slug) like concat('%', :search, '%')
              )
            """)
    Page<ProductRevision> findAdminPage(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            @Param("status") ProductPublicationStatus status,
            @Param("revisionStatus") ProductRevisionStatus revisionStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"product", "category"})
    @Query("""
            select revision
            from ProductRevision revision
            where revision.product.currentPublishedRevision = revision
              and revision.product.publicationStatus = :productStatus
              and revision.status = :revisionStatus
              and revision.category.active = true
              and (:categorySlug is null or revision.category.slug = :categorySlug)
              and (
                  :search is null
                  or lower(revision.name) like concat('%', :search, '%')
                  or lower(revision.shortDescription) like concat('%', :search, '%')
              )
            """)
    Page<ProductRevision> findPublicPage(
            @Param("productStatus") ProductPublicationStatus productStatus,
            @Param("revisionStatus") ProductRevisionStatus revisionStatus,
            @Param("categorySlug") String categorySlug,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"product", "category"})
    @Query("""
            select revision
            from ProductRevision revision
            where revision.product.currentPublishedRevision = revision
              and revision.product.publicationStatus = :productStatus
              and revision.status = :revisionStatus
              and revision.category.active = true
              and revision.slug = :slug
            """)
    Optional<ProductRevision> findPublicBySlug(
            @Param("slug") String slug,
            @Param("productStatus") ProductPublicationStatus productStatus,
            @Param("revisionStatus") ProductRevisionStatus revisionStatus
    );
}

package com.dongbacsaigon.backend.catalog.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.ProductRevisionImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRevisionImageRepository extends JpaRepository<ProductRevisionImage, UUID> {

    @EntityGraph(attributePaths = "media")
    List<ProductRevisionImage> findByRevisionIdOrderBySortOrderAsc(UUID revisionId);

    @EntityGraph(attributePaths = "media")
    List<ProductRevisionImage> findByRevisionIdInOrderByRevisionIdAscSortOrderAsc(List<UUID> revisionIds);

    boolean existsByMediaId(UUID mediaId);

    void deleteByRevisionId(UUID revisionId);
}

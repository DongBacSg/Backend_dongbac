package com.dongbacsaigon.backend.site.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.ManufacturingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManufacturingServiceRepository extends JpaRepository<ManufacturingService, UUID> {

    @EntityGraph(attributePaths = "featuredMedia")
    @Query("""
            select service from ManufacturingService service
            where (:search is null or lower(service.title) like concat('%', :search, '%')
                or lower(service.slug) like concat('%', :search, '%'))
              and (:active is null or service.active = :active)
            """)
    Page<ManufacturingService> findAdminPage(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "featuredMedia")
    List<ManufacturingService> findByActiveTrueOrderBySortOrderAscTitleAscIdAsc();

    @EntityGraph(attributePaths = "featuredMedia")
    Optional<ManufacturingService> findBySlugAndActiveTrue(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByFeaturedMediaId(UUID mediaId);
}

package com.dongbacsaigon.backend.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Category> findBySlugAndActiveTrue(String slug);

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
}

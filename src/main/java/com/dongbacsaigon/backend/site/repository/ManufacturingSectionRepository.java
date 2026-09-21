package com.dongbacsaigon.backend.site.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.ManufacturingSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturingSectionRepository extends JpaRepository<ManufacturingSection, UUID> {

    List<ManufacturingSection> findAllByOrderBySortOrderAscCreatedAtAsc();

    List<ManufacturingSection> findByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    boolean existsByMediaId(UUID mediaId);
}

package com.dongbacsaigon.backend.site.repository;

import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.ManufacturingPage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturingPageRepository extends JpaRepository<ManufacturingPage, UUID> {

    boolean existsByHeroMediaId(UUID mediaId);
}

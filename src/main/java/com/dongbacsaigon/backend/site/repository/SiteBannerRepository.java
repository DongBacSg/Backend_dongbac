package com.dongbacsaigon.backend.site.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.SiteBanner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteBannerRepository extends JpaRepository<SiteBanner, UUID> {

    List<SiteBanner> findAllByOrderBySortOrderAscCreatedAtAsc();

    List<SiteBanner> findByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    boolean existsByMediaId(UUID mediaId);
}

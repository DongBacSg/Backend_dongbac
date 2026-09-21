package com.dongbacsaigon.backend.site.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.SitePartner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitePartnerRepository extends JpaRepository<SitePartner, UUID> {

    List<SitePartner> findAllByOrderBySortOrderAscCreatedAtAsc();

    List<SitePartner> findByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    boolean existsByLogoMediaId(UUID mediaId);
}

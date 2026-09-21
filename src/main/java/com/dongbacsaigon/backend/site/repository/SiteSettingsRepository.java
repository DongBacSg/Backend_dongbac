package com.dongbacsaigon.backend.site.repository;

import java.util.UUID;

import com.dongbacsaigon.backend.site.entity.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {

    boolean existsByLogoMediaId(UUID mediaId);
}

package com.dongbacsaigon.backend.media.repository;

import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.media.entity.MediaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MediaRepository extends JpaRepository<Media, UUID>, JpaSpecificationExecutor<Media> {

    boolean existsByAssetId(String assetId);
    long countByStatus(MediaStatus status);

    @EntityGraph(attributePaths = {"uploadedBy", "deletedBy"})
    Optional<Media> findDetailedById(UUID id);
}

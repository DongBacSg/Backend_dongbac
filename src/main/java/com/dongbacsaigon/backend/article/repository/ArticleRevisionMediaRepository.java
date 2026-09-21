package com.dongbacsaigon.backend.article.repository;

import java.util.List;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticleRevisionMedia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRevisionMediaRepository extends JpaRepository<ArticleRevisionMedia, UUID> {

    @EntityGraph(attributePaths = "media")
    List<ArticleRevisionMedia> findByRevisionIdOrderByUsageTypeAscSortOrderAsc(UUID revisionId);

    @EntityGraph(attributePaths = "media")
    List<ArticleRevisionMedia> findByRevisionIdInOrderByRevisionIdAscUsageTypeAscSortOrderAsc(List<UUID> revisionIds);

    boolean existsByMediaId(UUID mediaId);
    void deleteByRevisionId(UUID revisionId);
}

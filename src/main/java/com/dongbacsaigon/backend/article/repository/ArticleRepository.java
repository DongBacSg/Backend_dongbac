package com.dongbacsaigon.backend.article.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.Article;
import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    @Query("select article.publicationStatus as status, count(article) as count from Article article group by article.publicationStatus")
    List<ArticlePublicationStatusCount> countGroupedByPublicationStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select article from Article article where article.id = :id")
    Optional<Article> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {
            "createdBy",
            "currentPublishedRevision",
            "currentPublishedRevision.createdBy",
            "currentPublishedRevision.publishedBy"
    })
    Optional<Article> findDetailedById(UUID id);
}

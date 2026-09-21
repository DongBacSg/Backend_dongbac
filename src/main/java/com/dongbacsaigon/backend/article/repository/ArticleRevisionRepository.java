package com.dongbacsaigon.backend.article.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;
import com.dongbacsaigon.backend.article.entity.ArticleRevision;
import com.dongbacsaigon.backend.article.entity.ArticleRevisionStatus;
import com.dongbacsaigon.backend.article.entity.ArticleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRevisionRepository extends JpaRepository<ArticleRevision, UUID> {

    @EntityGraph(attributePaths = {"article", "createdBy", "publishedBy"})
    Optional<ArticleRevision> findByIdAndArticleId(UUID id, UUID articleId);

    Optional<ArticleRevision> findByArticleIdAndRevisionNumber(UUID articleId, long revisionNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select revision from ArticleRevision revision
            join fetch revision.article
            join fetch revision.createdBy
            left join fetch revision.publishedBy
            where revision.article.id = :articleId and revision.revisionNumber = :revisionNumber
            """)
    Optional<ArticleRevision> findByArticleAndNumberForUpdate(@Param("articleId") UUID articleId, @Param("revisionNumber") long revisionNumber);

    @EntityGraph(attributePaths = {"createdBy", "publishedBy"})
    List<ArticleRevision> findByArticleIdOrderByRevisionNumberDesc(UUID articleId);

    @EntityGraph(attributePaths = {"createdBy", "publishedBy"})
    Optional<ArticleRevision> findFirstByArticleIdOrderByRevisionNumberDesc(UUID articleId);

    boolean existsByArticleIdAndStatusIn(UUID articleId, List<ArticleRevisionStatus> statuses);
    boolean existsByStatusAndSlugAndArticleIdNot(ArticleRevisionStatus status, String slug, UUID articleId);

    @EntityGraph(attributePaths = {
            "article",
            "article.currentPublishedRevision",
            "article.currentPublishedRevision.createdBy",
            "article.currentPublishedRevision.publishedBy",
            "createdBy",
            "publishedBy"
    })
    @Query("""
            select revision from ArticleRevision revision
            where revision.revisionNumber = revision.article.latestRevisionNumber
              and (:type is null or revision.articleType = :type)
              and (:publicationStatus is null or revision.article.publicationStatus = :publicationStatus)
              and (:revisionStatus is null or revision.status = :revisionStatus)
              and (:search is null or lower(revision.title) like concat('%', :search, '%')
                   or lower(revision.slug) like concat('%', :search, '%')
                   or lower(revision.summary) like concat('%', :search, '%'))
            """)
    Page<ArticleRevision> findAdminPage(
            @Param("search") String search,
            @Param("type") ArticleType type,
            @Param("publicationStatus") ArticlePublicationStatus publicationStatus,
            @Param("revisionStatus") ArticleRevisionStatus revisionStatus,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "article")
    @Query("""
            select revision from ArticleRevision revision
            where revision.article.currentPublishedRevision = revision
              and revision.article.publicationStatus = :publicationStatus
              and revision.status = :revisionStatus
              and (:type is null or revision.articleType = :type)
              and (:search is null or lower(revision.title) like concat('%', :search, '%')
                   or lower(revision.summary) like concat('%', :search, '%'))
            """)
    Page<ArticleRevision> findPublicPage(
            @Param("publicationStatus") ArticlePublicationStatus publicationStatus,
            @Param("revisionStatus") ArticleRevisionStatus revisionStatus,
            @Param("type") ArticleType type,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "article")
    @Query("""
            select revision from ArticleRevision revision
            where revision.article.currentPublishedRevision = revision
              and revision.article.publicationStatus = :publicationStatus
              and revision.status = :revisionStatus
              and revision.slug = :slug
            """)
    Optional<ArticleRevision> findPublicBySlug(
            @Param("slug") String slug,
            @Param("publicationStatus") ArticlePublicationStatus publicationStatus,
            @Param("revisionStatus") ArticleRevisionStatus revisionStatus
    );
}

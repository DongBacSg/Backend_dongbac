package com.dongbacsaigon.backend.article.entity;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "article_revisions")
public class ArticleRevision {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "revision_number", nullable = false)
    private long revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ArticleRevisionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "article_type", nullable = false, length = 32)
    private ArticleType articleType;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 260)
    private String slug;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "seo_title", length = 240)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected ArticleRevision() {
    }

    public ArticleRevision(
            Article article,
            long revisionNumber,
            ArticleType articleType,
            String title,
            String slug,
            String summary,
            String content,
            String seoTitle,
            String seoDescription,
            User createdBy
    ) {
        this.id = UUID.randomUUID();
        this.article = article;
        this.revisionNumber = revisionNumber;
        this.status = ArticleRevisionStatus.DRAFT;
        this.articleType = articleType;
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.content = content;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Article getArticle() { return article; }
    public long getRevisionNumber() { return revisionNumber; }
    public ArticleRevisionStatus getStatus() { return status; }
    public ArticleType getArticleType() { return articleType; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getSeoTitle() { return seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public User getPublishedBy() { return publishedBy; }
    public long getVersion() { return version; }

    public void updateDraft(ArticleType articleType, String title, String slug, String summary, String content, String seoTitle, String seoDescription) {
        this.articleType = articleType;
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.content = content;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
    }

    public void submit() { status = ArticleRevisionStatus.PENDING_REVIEW; }
    public void publish(User admin, Instant now) { status = ArticleRevisionStatus.PUBLISHED; publishedBy = admin; publishedAt = now; }
    public void reject() { status = ArticleRevisionStatus.REJECTED; }
    public void archive() { status = ArticleRevisionStatus.ARCHIVED; }
}

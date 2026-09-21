package com.dongbacsaigon.backend.site.entity;

import java.time.Instant;
import java.util.UUID;

import com.dongbacsaigon.backend.media.entity.Media;
import com.dongbacsaigon.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "site_partners")
public class SitePartner {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logo_media_id", nullable = false)
    private Media logoMedia;

    @Column(name = "website_url", columnDefinition = "text")
    private String websiteUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SitePartner() {
    }

    public SitePartner(String name, Media logoMedia, String websiteUrl, int sortOrder, boolean active, User actor) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.logoMedia = logoMedia;
        this.websiteUrl = websiteUrl;
        this.sortOrder = sortOrder;
        this.active = active;
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Media getLogoMedia() { return logoMedia; }
    public String getWebsiteUrl() { return websiteUrl; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String name, Media logoMedia, String websiteUrl, int sortOrder, boolean active, User actor) {
        this.name = name;
        this.logoMedia = logoMedia;
        this.websiteUrl = websiteUrl;
        this.sortOrder = sortOrder;
        this.active = active;
        this.updatedBy = actor;
    }
}

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
@Table(name = "manufacturing_page")
public class ManufacturingPage {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String introduction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_media_id")
    private Media heroMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ManufacturingPage() {
    }

    public ManufacturingPage(UUID id) {
        this.id = id;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getIntroduction() { return introduction; }
    public Media getHeroMedia() { return heroMedia; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String introduction, Media heroMedia, User actor) {
        this.title = title;
        this.introduction = introduction;
        this.heroMedia = heroMedia;
        this.updatedBy = actor;
    }
}

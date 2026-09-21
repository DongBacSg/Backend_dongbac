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
@Table(name = "site_settings")
public class SiteSettings {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_name", length = 160)
    private String companyName;

    @Column(columnDefinition = "text")
    private String slogan;

    @Column(columnDefinition = "text")
    private String vision;

    @Column(columnDefinition = "text")
    private String mission;

    @Column(columnDefinition = "text")
    private String philosophy;

    @Column(name = "brand_narrative", columnDefinition = "text")
    private String brandNarrative;

    @Column(name = "core_values", columnDefinition = "text")
    private String coreValues;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_media_id")
    private Media logoMedia;

    @Column(name = "office_address", columnDefinition = "text")
    private String officeAddress;

    @Column(name = "factory_address", columnDefinition = "text")
    private String factoryAddress;

    @Column(length = 64)
    private String phone;

    @Column(length = 320)
    private String email;

    @Column(name = "facebook_url", columnDefinition = "text")
    private String facebookUrl;

    @Column(name = "youtube_url", columnDefinition = "text")
    private String youtubeUrl;

    @Column(name = "zalo_url", columnDefinition = "text")
    private String zaloUrl;

    @Column(name = "map_url", columnDefinition = "text")
    private String mapUrl;

    @Column(name = "tvc_url", columnDefinition = "text")
    private String tvcUrl;

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

    protected SiteSettings() {
    }

    public SiteSettings(UUID id) {
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
    public String getCompanyName() { return companyName; }
    public String getSlogan() { return slogan; }
    public String getVision() { return vision; }
    public String getMission() { return mission; }
    public String getPhilosophy() { return philosophy; }
    public String getBrandNarrative() { return brandNarrative; }
    public String getCoreValues() { return coreValues; }
    public Media getLogoMedia() { return logoMedia; }
    public String getOfficeAddress() { return officeAddress; }
    public String getFactoryAddress() { return factoryAddress; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getFacebookUrl() { return facebookUrl; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getZaloUrl() { return zaloUrl; }
    public String getMapUrl() { return mapUrl; }
    public String getTvcUrl() { return tvcUrl; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            String companyName,
            String slogan,
            String vision,
            String mission,
            String philosophy,
            String brandNarrative,
            String coreValues,
            Media logoMedia,
            String officeAddress,
            String factoryAddress,
            String phone,
            String email,
            String facebookUrl,
            String youtubeUrl,
            String zaloUrl,
            String mapUrl,
            String tvcUrl,
            User actor
    ) {
        this.companyName = companyName;
        this.slogan = slogan;
        this.vision = vision;
        this.mission = mission;
        this.philosophy = philosophy;
        this.brandNarrative = brandNarrative;
        this.coreValues = coreValues;
        this.logoMedia = logoMedia;
        this.officeAddress = officeAddress;
        this.factoryAddress = factoryAddress;
        this.phone = phone;
        this.email = email;
        this.facebookUrl = facebookUrl;
        this.youtubeUrl = youtubeUrl;
        this.zaloUrl = zaloUrl;
        this.mapUrl = mapUrl;
        this.tvcUrl = tvcUrl;
        this.updatedBy = actor;
    }
}

package com.dongbacsaigon.backend.lead.entity;

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
@Table(name = "customer_leads")
public class CustomerLead {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(length = 64)
    private String phone;

    @Column(length = 320)
    private String email;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(length = 240)
    private String subject;

    @Column(columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LeadStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "internal_note", columnDefinition = "text")
    private String internalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CustomerLead() {
    }

    public CustomerLead(
            String fullName,
            String phone,
            String email,
            String companyName,
            String message,
            String internalNote,
            User createdBy
    ) {
        this(fullName, phone, email, companyName, null, message, internalNote, createdBy);
    }

    public CustomerLead(
            String fullName,
            String phone,
            String email,
            String companyName,
            String subject,
            String message,
            String internalNote,
            User createdBy
    ) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.companyName = companyName;
        this.subject = subject;
        this.message = message;
        this.internalNote = internalNote;
        this.createdBy = createdBy;
        this.status = LeadStatus.NEW;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getCompanyName() { return companyName; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public LeadStatus getStatus() { return status; }
    public User getAssignedTo() { return assignedTo; }
    public String getInternalNote() { return internalNote; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void updateDetails(
            String fullName,
            String phone,
            String email,
            String companyName,
            String message,
            String internalNote
    ) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.companyName = companyName;
        this.message = message;
        this.internalNote = internalNote;
    }

    public void changeStatus(LeadStatus status) { this.status = status; }
    public void assignTo(User assignedTo) { this.assignedTo = assignedTo; }
}

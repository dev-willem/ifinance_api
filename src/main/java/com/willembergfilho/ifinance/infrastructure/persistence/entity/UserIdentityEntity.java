package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Extra OAuth2 identity linked to an existing user; the original one stays in users.subject_id. */
@Entity
@Table(name = "user_identities")
public class UserIdentityEntity {

    @Id
    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserIdentityEntity() {}

    public UserIdentityEntity(String subjectId, UUID userId, String provider) {
        this.subjectId = subjectId;
        this.userId = userId;
        this.provider = provider;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getSubjectId() { return subjectId; }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public Instant getCreatedAt() { return createdAt; }
}

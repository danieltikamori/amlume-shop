/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.model;

import jakarta.persistence.*;
import me.amlu.authserver.user.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SoftDelete;
import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

@EntityListeners(AuditingEntityListener.class) // Enables JPA Auditing
@FilterDef(name = "deletedFilter", defaultCondition = "deleted = false") // Simplified condition for boolean flag
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = Long.class)})
// Added Setter - needed by AuditingEntityListener to set values
@MappedSuperclass
// --- Make the class generic for the ID type ---
public abstract class BaseEntity<ID extends Serializable> implements Serializable, Auditable<User, ID, Instant> {
    @Serial
    private static final long serialVersionUID = 1L;

    // Assuming ID is defined in subclasses

    @Version
    @Column(nullable = false) // Ensure a version is not null
    private int version;

    @Column(name = "idempotency_key", unique = true, nullable = true, updatable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @CreatedDate // Automatically set by AuditingEntityListener
    @Column(name = "created_date", nullable = false, updatable = false) // Use nullable=false
    private Instant createdDate;

    @CreatedBy // Automatically set by AuditingEntityListener
    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch for audit users
    @JoinColumn(name = "created_by", nullable = true, updatable = false)
    private User createdBy;

    @LastModifiedDate // Automatically set by AuditingEntityListener
    @Column(name = "last_modified_date", nullable = false) // Use nullable=false
    private Instant lastModifiedDate; // Renamed for consistency

    @LastModifiedBy // Added - Automatically set by AuditingEntityListener
    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch for audit users
    @JoinColumn(name = "last_modified_by", nullable = true)
    private User lastModifiedBy;

    @SoftDelete // Use Hibernate's annotation for soft delete (uses boolean 'deleted' field by default)
    @Column(name = "deleted", nullable = false) // Use nullable=false
    private boolean deleted = false;

    // --- Fields for tracking *when* and *by whom* soft delete happened ---
    // NOTE: These fields are NOT automatically populated by Hibernate's @SoftDelete.
    // Need custom logic (e.g., overriding repository delete methods, AOP)
    // to populate these when a soft delete occurs (which is an UPDATE).
    @Column(name = "deleted_at", nullable = true) // Nullable because it's only set when deleted
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch
    @JoinColumn(name = "deleted_by", nullable = true) // Nullable because it's only set when deleted
    private User deletedBy;

    // --- The abstract getId() uses the generic type ---
    @Override
    @Transient
    public abstract ID getId();

    // --- isNew() now correctly uses the generic getId() ---
    @Override
    public boolean isNew() {
        return getId() == null;
    }

    public BaseEntity() {
    }

    // --- Constructor to accept the generic builder ---
    protected BaseEntity(BaseEntityBuilder<ID, ?, ?> b) {
        this.version = b.version;
        this.idempotencyKey = b.idempotencyKey;
        this.createdDate = b.createdDate;
        this.createdBy = b.createdBy;
        this.lastModifiedDate = b.lastModifiedDate;
        this.lastModifiedBy = b.lastModifiedBy;
        this.deleted = b.deleted;
        this.deletedAt = b.deletedAt;
        this.deletedBy = b.deletedBy;
    }

    // --- Implementation of Auditable ---
    // Setters are needed by the AuditingEntityListener

    @Override
    public @NonNull Optional<User> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    @Override
    public void setCreatedBy(@NonNull User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public @NonNull Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(this.createdDate);
    }

    @Override
    public void setCreatedDate(@NonNull Instant creationDate) {
        this.createdDate = creationDate;
    }

    @Override
    public @NonNull Optional<User> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }

    @Override
    public void setLastModifiedBy(@NonNull User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public @NonNull Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(this.lastModifiedDate);
    }

    @Override
    public void setLastModifiedDate(@NonNull Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }


    // Abstract method to be implemented by subclasses to return their specific ID
    // This is needed for the Auditable.isNew() method.
    @Transient // Exclude from persistence
    public abstract ID getAuditableId();

    public int getVersion() {
        return this.version;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }

    public User getDeletedBy() {
        return this.deletedBy;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    // --- Make the builder generic to avoid raw type usage ---
    public static abstract class BaseEntityBuilder<
            ID extends Serializable,
            C extends BaseEntity<ID>,
            B extends BaseEntityBuilder<ID, C, B>
            > {
        private int version;
        private String idempotencyKey;
        private Instant createdDate;
        private User createdBy;
        private Instant lastModifiedDate;
        private User lastModifiedBy;
        private boolean deleted;
        private Instant deletedAt;
        private User deletedBy;

        public B version(int version) {
            this.version = version;
            return self();
        }

        public B idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return self();
        }

        public B createdDate(Instant createdDate) {
            this.createdDate = createdDate;
            return self();
        }

        public B createdBy(User createdBy) {
            this.createdBy = createdBy;
            return self();
        }

        public B lastModifiedDate(Instant lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return self();
        }

        public B lastModifiedBy(User lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return self();
        }

        public B deleted(boolean deleted) {
            this.deleted = deleted;
            return self();
        }

        public B deletedAt(Instant deletedAt) {
            this.deletedAt = deletedAt;
            return self();
        }

        public B deletedBy(User deletedBy) {
            this.deletedBy = deletedBy;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        @Override
        public String toString() {
            return "BaseEntity.BaseEntityBuilder(version=" + this.version + ", idempotencyKey=" + this.idempotencyKey + ", createdDate=" + this.createdDate + ", createdBy=" + this.createdBy + ", lastModifiedDate=" + this.lastModifiedDate + ", lastModifiedBy=" + this.lastModifiedBy + ", deleted=" + this.deleted + ", deletedAt=" + this.deletedAt + ", deletedBy=" + this.deletedBy + ")";
        }
    }
}

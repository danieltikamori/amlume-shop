/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SoftDelete;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
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
// Example FilterDef with parameter
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = Long.class)})
// Added Setter - needed by AuditingEntityListener to set values
@MappedSuperclass
public abstract class BaseEntity implements Serializable, Auditable<User, Long, Instant> {
    @Serial
    private static final long serialVersionUID = 1L;

    // Assuming ID is defined in subclasses

    @Version
    @Column(nullable = false) // Ensure a version is not null
    private int version;

    @Column(name = "idempotency_key", unique = true, nullable = false, updatable = false)
    private String idempotencyKey;

    @CreatedDate // Automatically set by AuditingEntityListener
    @Column(name = "created_date", nullable = false, updatable = false) // Use nullable=false
    private Instant createdDate;

    @CreatedBy // Automatically set by AuditingEntityListener
    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch for audit users
    @JoinColumn(name = "created_by", nullable = false, updatable = false) // Use nullable=false
    private User createdBy;

    @LastModifiedDate // Automatically set by AuditingEntityListener
    @Column(name = "last_modified_date", nullable = false) // Use nullable=false
    private Instant lastModifiedDate; // Renamed for consistency

    @LastModifiedBy // Added - Automatically set by AuditingEntityListener
    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch for audit users
    @JoinColumn(name = "last_modified_by", nullable = false) // Use nullable=false
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

    public BaseEntity() {
    }

    protected BaseEntity(BaseEntityBuilder<?, ?> b) {
        this.version = b.version;
        this.idempotencyKey = b.idempotencyKey;
        this.createdDate = b.createdDate;
        this.createdBy = b.createdBy;
        this.lastModifiedDate = b.lastModifiedDate;
        this.lastModifiedBy = b.lastModifiedBy;
        if (b.deleted$set) {
            this.deleted = b.deleted$value;
        } else {
            this.deleted = $default$deleted();
        }
        this.deletedAt = b.deletedAt;
        this.deletedBy = b.deletedBy;
    }

    private static boolean $default$deleted() {
        return false;
    }
    // --- End of custom soft delete tracking fields ---

    // --- Implementation of Auditable ---
    // Setters are needed by the AuditingEntityListener

    @NullMarked
    @Override
    public Optional<User> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    @Override
    public void setCreatedBy(@NonNull User createdBy) {
        this.createdBy = createdBy;
    }

    @NullMarked
    @Override
    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(this.createdDate);
    }

    @NullMarked
    @Override
    public void setCreatedDate(@NonNull Instant creationDate) {
        this.createdDate = creationDate;
    }

    @org.jetbrains.annotations.NotNull
    @NullMarked
    @Override
    public Optional<User> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }

    @Override
    public void setLastModifiedBy(@NonNull User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @NullMarked
    @Override
    public Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(this.lastModifiedDate);
    }

    @NullMarked
    @Override
    public void setLastModifiedDate(@NonNull Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean isNew() {
        // Assumes ID is Long and generated by the database.
        // Adjust if your ID strategy is different.
        // This method is part of Auditable but often needs implementation in concrete entities
        // if the ID isn't directly in BaseEntity. Let's make it abstract.
        // return getId() == null; // Replace getId() with actual ID getter if ID is here
        return getAuditableId() == null; // Rely on subclasses implementing getAuditableId()
    }

    // Abstract method to be implemented by subclasses to return their specific ID
    // This is needed for the Auditable.isNew() method.
    @Transient // Exclude from persistence
    public abstract Long getAuditableId();

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

    public static abstract class BaseEntityBuilder<C extends BaseEntity, B extends BaseEntityBuilder<C, B>> {
        private int version;
        private String idempotencyKey;
        private Instant createdDate;
        private User createdBy;
        private Instant lastModifiedDate;
        private User lastModifiedBy;
        private boolean deleted$value;
        private boolean deleted$set;
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
            this.deleted$value = deleted;
            this.deleted$set = true;
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

        public String toString() {
            return "BaseEntity.BaseEntityBuilder(version=" + this.version + ", idempotencyKey=" + this.idempotencyKey + ", createdDate=" + this.createdDate + ", createdBy=" + this.createdBy + ", lastModifiedDate=" + this.lastModifiedDate + ", lastModifiedBy=" + this.lastModifiedBy + ", deleted$value=" + this.deleted$value + ", deletedAt=" + this.deletedAt + ", deletedBy=" + this.deletedBy + ")";
        }
    }
    // --- End of implementation of Auditable ---

}

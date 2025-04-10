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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SoftDelete;
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
@Getter
@Setter // Added Setter - needed by AuditingEntityListener to set values
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity implements Serializable, Auditable<User, Long, Instant> {
    @Serial
    private static final long serialVersionUID = 1L;

    // Assuming ID is defined in subclasses

    @Version
    @Column(nullable = false) // Ensure version is not null
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
    @Builder.Default
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
    // --- End of custom soft delete tracking fields ---

    // --- Implementation of Auditable ---
    // Setters are needed by the AuditingEntityListener

    @Override
    public Optional<User> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    @Override
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Optional<Instant> getCreatedDate() {
        return Optional.ofNullable(this.createdDate);
    }

    @Override
    public void setCreatedDate(Instant creationDate) {
        this.createdDate = creationDate;
    }

    @Override
    public Optional<User> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }

    @Override
    public void setLastModifiedBy(User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public Optional<Instant> getLastModifiedDate() {
        return Optional.ofNullable(this.lastModifiedDate);
    }

    @Override
    public void setLastModifiedDate(Instant lastModifiedDate) {
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
    // --- End of implementation of Auditable ---

}

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
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.mapping.SoftDeletable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "deletedFilter", defaultCondition = "deleted_at IS NULL")
@FilterDef(name = "adminFilter", defaultCondition = "1=1")
@Getter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity implements Serializable, SoftDeletable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Version
    private int version;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @CreatedDate
    @NotBlank
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME ZONE='UTC'")
    private Instant createdAt;

    @CreatedBy
    @NotBlank
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdByUser;

    @LastModifiedDate
    @NotBlank
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME ZONE='UTC'")
    Instant updatedAt;

    @NotBlank
    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedByUser;

    @SoftDelete(columnName = "deleted", strategy = SoftDeleteType.DELETED)
    @NotBlank
    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean deleted = false;

//    @SensitiveData(rolesAllowed = {"ADMIN", "ROOT"})
    @Column(nullable = true, name = "deleted_at", columnDefinition = "DATETIME ZONE='UTC'")
    Instant deletedAt;

    //    @SensitiveData(rolesAllowed = {"ADMIN", "ROOT"})
    @ManyToOne
    @JoinColumn(nullable = true, name = "deleted_by")
    User deletedByUser;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @PreRemove
    public void preRemove() {
        this.deletedAt = Instant.now();
        this.deletedByUser = this.updatedByUser; // getAuthenticatedUser();
    }

    @Override
    public void enableSoftDelete(org.hibernate.mapping.Column indicatorColumn) {
        this.deletedAt = Instant.now();
        this.deletedByUser = this.getUpdatedByUser();
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    @Override
    public org.hibernate.mapping.Column getSoftDeleteColumn() {
        return new org.hibernate.mapping.Column("deleted");
    }

}

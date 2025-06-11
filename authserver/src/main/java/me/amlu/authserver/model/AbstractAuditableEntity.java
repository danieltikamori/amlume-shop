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

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) // Apply auditing listener to all subclasses
public abstract class AbstractAuditableEntity {

    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // --- Auditing Fields ---

    /**
     * Reference to the user who created
     * Stores the user_id
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy; // Assuming auditor ID is Long

    /**
     * Reference to the user who last modified
     * Stores the user_id
     */
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private Long lastModifiedBy; // Assuming auditor ID is Long

    // --- Getters ---

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getLastModifiedBy() {
        return lastModifiedBy;
    }

    // Setters are typically not needed for these fields as they are managed by JPA/Hibernate
    // public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    // public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    // public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    // public void setLastModifiedBy(Long lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}

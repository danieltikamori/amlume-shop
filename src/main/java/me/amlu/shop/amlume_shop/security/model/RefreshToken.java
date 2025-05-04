/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "refresh_token")
@SuperBuilder // Added SuperBuilder if you use builders
@NoArgsConstructor // Added NoArgsConstructor for JPA
public class RefreshToken extends BaseEntity { // BaseEntity already implements Serializable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Use LAZY fetch
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token; // This is the HASHED token

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    // Removed redundant 'revoked' field. Rely on inherited 'deleted' field managed by @SoftDelete.
    // If 'revoked' has a distinct meaning, add it back with clear documentation.
    // @Column(name = "revoked", nullable = false)
    // private boolean revoked;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    // --- Implementation of abstract method from BaseEntity ---
    @Override
    @Transient // Exclude from persistence mapping
    public Long getAuditableId() {
        // Return the specific ID of this RefreshToken entity
        return this.id;
    }

    // --- equals() and hashCode() based on primary key 'id' ---

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // Gets the underlying class even if 'o' is a proxy
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        // Gets the underlying class even if 'this' is a proxy
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        // Compares the effective classes
        if (thisEffectiveClass != oEffectiveClass) return false;
        // Now safe to cast
        RefreshToken that = (RefreshToken) o; // Cast to RefreshToken
        // Finally, compare by ID. Use getId() in case 'id' is null (transient state).
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        // Base the hashCode primarily on the unique identifier (id)
        // Objects.hash() handles null correctly if the entity is new (ID not yet assigned)
        return Objects.hash(id);

        // --- Alternative using a constant for null ID (also common) ---
        // return id == null ? 31 : id.hashCode();
    }

}
/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.model;

import jakarta.persistence.*;
import me.amlu.authserver.model.AbstractAuditableEntity;
import org.springframework.security.core.GrantedAuthority;

import java.util.Objects;

@Entity
@Table(name = "authorities") // Ensure table name matches your DB schema if it exists
public class Authority extends AbstractAuditableEntity implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true) // Authority names (e.g., "ROLE_USER", "SCOPE_read") should be unique
    private String authority;

    // If you want a bidirectional relationship from User to Authority,
    // the @ManyToOne in User's authorities collection would be mappedBy this.
    // However, Spring Security often uses a unidirectional Set<GrantedAuthority> in UserDetails.
    // For a simple setup where Authority is just a lookup table for role names,
    // the @ManyToOne in User's authorities collection is fine.
    // The provided Authority entity has a @ManyToOne to User, which implies each authority record is specific to a user.
    // This is unusual if 'authorities' table is meant to store global roles.
    // If 'authorities' stores global roles (e.g., "ROLE_USER", "ROLE_ADMIN"), then the @ManyToOne User user; field should be removed.
    // And User would have a @ManyToMany Set<Authority> roles;
    // Given the current structure of User having Set<Authority>, let's assume 'authorities' table stores user-specific grant instances.
    // This is fine, but less common than a shared roles table.
    // For simplicity with Spring Authorization Server, often UserDetails has a Set<String> or Set<GrantedAuthority> directly.

    public Authority(String authority) {
        this.authority = authority;
    }

    public Authority(long id, String authority) {
        this.id = id;
        this.authority = authority;
    }

    public Authority() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authority otherAuthority)) return false; // Use instanceof and pattern variable
        // Class<?> oEffectiveClass = o instanceof HibernateProxy ? ... // Not strictly needed if Authority is not proxied often, but safe
        // Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ...
        // if (thisEffectiveClass != oEffectiveClass) return false;
        // Authority otherAuthority = (Authority) o;
        return Objects.equals(this.authority, otherAuthority.authority);
    }


    @Override
    public int hashCode() {
        return Objects.hash(authority);
    }

    @Override
    public String toString() {
        return this.authority; // Simple toString for readability
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }

    public void setAuthority(String name) {
        this.authority = name;
    }

}

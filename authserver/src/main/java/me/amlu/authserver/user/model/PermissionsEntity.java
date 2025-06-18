/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model;

import jakarta.persistence.*;
import me.amlu.authserver.model.AbstractAuditableEntity;
import me.amlu.authserver.oauth2.model.Authority;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionsEntity extends AbstractAuditableEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "PRODUCT_READ", "PRODUCT_CREATE", "USER_MANAGE"

    private String description;

    @ManyToMany(mappedBy = "permissions")
    // Bidirectional relationship with Role
    private Set<Authority> authorities = new HashSet<>();

    public PermissionsEntity() {
    }

    public PermissionsEntity(UUID id, String name, String description, Set<Authority> authorities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.authorities = authorities;
    }

    public static PermissionsEntityBuilder builder() {
        return new PermissionsEntityBuilder();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PermissionsEntity that = (PermissionsEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<Authority> getAuthorities() {
        return this.authorities;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public String toString() {
        return "PermissionsEntity(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ")";
    }

    public static class PermissionsEntityBuilder {
        private UUID id;
        private String name;
        private String description;
        private Set<Authority> authorities;

        PermissionsEntityBuilder() {
        }

        public PermissionsEntityBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PermissionsEntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PermissionsEntityBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionsEntityBuilder authorities(Set<Authority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public PermissionsEntity build() {
            return new PermissionsEntity(this.id, this.name, this.description, this.authorities);
        }

        public String toString() {
            return "PermissionsEntity.PermissionsEntityBuilder(id=" + this.id + ", name=" + this.name + ", description=" + this.description + ", authorities=" + this.authorities + ")";
        }
    }
}

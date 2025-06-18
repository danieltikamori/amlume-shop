/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.role.model;

import jakarta.persistence.*;
import lombok.*;
import me.amlu.authserver.model.AbstractAuditableEntity;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role entity for hierarchical role-based access control.
 * Uses PostgreSQL LTREE extension for efficient hierarchical queries.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Role extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    /**
     * LTREE path representing the role hierarchy.
     * Example: "admin.manager.regional"
     * <p>
     * Note: Requires PostgreSQL LTREE extension:
     * CREATE EXTENSION IF NOT EXISTS ltree;
     */
    @Column(columnDefinition = "ltree", nullable = false)
    private String path;

    /**
     * Direct parent role in the hierarchy.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private Role parent;

    /**
     * Direct child roles in the hierarchy.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<Role> children = new HashSet<>();

    // Already defined at AbstractAuditableEntity
//    @CreationTimestamp
//    private Instant createdAt;
//
//    @UpdateTimestamp
//    private Instant updatedAt;

    /**
     * Add a child role to this role.
     *
     * @param child The child role to add
     */
    public void addChild(Role child) {
        children.add(child);
        child.setParent(this);

        // Update the child's path based on this role's path
        if (this.path != null) {
            child.setPath(this.path + "." + child.getName().toLowerCase().replace(" ", "_"));
        } else {
            child.setPath(child.getName().toLowerCase().replace(" ", "_"));
        }
    }

    /**
     * Remove a child role from this role.
     *
     * @param child The child role to remove
     */
    public void removeChild(Role child) {
        children.remove(child);
        child.setParent(null);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Role role = (Role) o;
        return getId() != null && Objects.equals(getId(), role.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

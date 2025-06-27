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
import me.amlu.authserver.user.model.User;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.io.Serializable;
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
@NoArgsConstructor
@AllArgsConstructor
public class Role extends AbstractAuditableEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
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
    @ToString.Exclude // Avoid recursion in toString
    private Role parent;

    /**
     * The set of child roles in the hierarchy.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Avoid recursion in toString
    @Builder.Default // Ensures this is initialized to an empty set by the builder.
    private Set<Role> children = new HashSet<>();

    /**
     * The set of users assigned to this role.
     */
    @ManyToMany(mappedBy = "authorities") // Assuming 'authorities' is the field in User entity
    @ToString.Exclude // Avoid recursion in toString
    @Builder.Default // Ensures this is initialized to an empty set by the builder.
    private Set<User> users = new HashSet<>();

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

    // --- equals() and hashCode() ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Role role = (Role) o;
        return getId() != null && Objects.equals(getId(), role.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ?
                hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() :
                getClass().hashCode();
    }

    public Object getRoleName() {
        return name;
    }
}

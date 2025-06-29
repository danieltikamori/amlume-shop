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

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import me.amlu.authserver.model.BaseEntity;
import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.util.TsidKeyGenerator;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serial;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a permission in the system. Permissions are granular authorizations that can be assigned to
 * {@link Role} (authority) to control access to specific resources or functionalities.
 */
@Getter
@Entity
@Table(name = "permissions")
public class PermissionsEntity extends BaseEntity<String> {

    @Serial
    private static final long serialVersionUID = 1L;

//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID) // Example of UUID generation
//    private UUID id; // Example of UUID field

    /**
     * The unique identifier for the permission. Generated using TSID (Time-Sorted Unique Identifier) for better
     * database indexing and natural ordering.
     */
    @Id
    @Tsid
    @Column(columnDefinition = "CHAR(26)", updatable = false, nullable = false, unique = true)
    String id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "PRODUCT_READ", "PRODUCT_CREATE", "USER_MANAGE"
    /**
     * A brief description of what the permission allows.
     */
    private String description;

    /**
     * The set of roles (authorities) that possess this permission.
     * This is a bidirectional relationship mapped by the "permissions" field in the {@link Role} entity.
     */
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    /**
     * Default constructor. Initializes the ID using {@link TsidKeyGenerator#next()}.
     */
    public PermissionsEntity() {
        this.id = TsidKeyGenerator.next();
    }

    /**
     * Constructs a new PermissionsEntity with the specified name, description, and roles.
     * The ID is generated automatically.
     *
     * @param name        The unique name of the permission.
     * @param description A description of the permission.
     * @param roles The set of roles associated with this permission.
     */
    public PermissionsEntity(String name, String description, Set<Role> roles) {
        this();
        this.name = name;
        this.description = description;
        // Defensive copy to prevent external modification of the internal set
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
    }

//    public PermissionsEntity(String permissionName, String permissionDescription) {

    /// /        this.id = permissionsId.isEmpty() ? TsidKeyGenerator.next() : permissionsId;
//        this();
//        // Note: This constructor was commented out. If it were to be used,
//        // it would create a permission without any associated roles,
//        // which might be a valid use case for initial permission creation.
//        this.name = permissionName;
//        this.description = permissionDescription;
//    }
    public PermissionsEntity(String id, String permissionName, String permissionDescription) {
        this.id = id;
        this.name = permissionName;
        this.description = permissionDescription;
    }

    /**
     * Constructs a new PermissionsEntity with a specified ID, name, description, and roles.
     * If the provided ID is empty, a new TSID is generated.
     *
     * @param id          The ID of the permission. If empty, a new TSID is generated.
     * @param name        The unique name of the permission.
     * @param description A description of the permission.
     * @param roles The set of roles associated with this permission.
     */
    public PermissionsEntity(String id, String name, String description, Set<Role> roles) {
        // Ensure ID is never null or empty; generate if needed.
        this.id = (id == null || id.isEmpty()) ? TsidKeyGenerator.next() : id;
        this.name = name;
        this.description = description;
        // Defensive copy to prevent external modification of the internal set.
        // This is crucial for maintaining the integrity of the entity's state.
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
    }

    /**
     * Constructor for deserialization from Hazelcast session.
     * Only populates fields essential for the security context.
     *
     * @param id   The permission's unique ID (TSID).
     * @param name The name of the permission (e.g., "PROFILE_EDIT_OWN").
     */
    public PermissionsEntity(String id, String name) {
        // It's important to ensure the ID is not null or empty, even for deserialization.
        // If an empty ID is passed, it might indicate an issue in the serialization process.
        // For this specific use case (Hazelcast session), we assume valid IDs are provided.
        this.id = id;
        this.name = name;
        // Other fields like description and roles will be null/empty, which is
        // acceptable for a detached object used in a security context.
        // Initialize roles to an empty set to avoid NullPointerExceptions if accessed.
        this.roles = new HashSet<>();
    }

    /**
     * Provides a builder for creating {@link PermissionsEntity} instances.
     * This offers a more readable and flexible way to construct objects, especially when
     * there are many optional fields.
     *
     * @return A new instance of {@link PermissionsEntityBuilder}.
     */
    public static PermissionsEntityBuilder builder() {
        return new PermissionsEntityBuilder();
    }

    /**
     * Compares this PermissionsEntity to the specified object. The result is {@code true} if and only if
     * the argument is not null and is a PermissionsEntity object that has the same ID as this object.
     * This method correctly handles Hibernate proxies.
     *
     * @param o The object to compare this PermissionsEntity against.
     * @return {@code true} if the given object represents a PermissionsEntity equivalent to this PermissionsEntity,
     *         {@code false} otherwise.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // Handle Hibernate proxies to ensure correct comparison of proxied entities.
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PermissionsEntity that = (PermissionsEntity) o;
        // Use Objects.equals for null-safe comparison of the ID.
        // The ID is the natural key for equality in JPA entities.
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    /**
     * Returns a hash code value for the object. This method is supported for the benefit of hash tables
     * such as those provided by {@link java.util.HashMap}.
     * The hash code is based on the entity's ID, which is consistent with the {@code equals} method.
     * This method correctly handles Hibernate proxies.
     *
     * @return A hash code value for this object.
     */
    @Override
    public final int hashCode() {
        // For Hibernate entities, it's good practice to use the persistent class's hash code
        // or a constant value if the ID can be null before persistence.
        // Since our ID is generated on construction, it should always be non-null.
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String getAuditableId() {
        return this.id;
    }

    /**
     * Returns the unique ID of the permission.
     *
     * @return The permission ID.
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Returns the name of the permission.
     *
     * @return The permission name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the description of the permission.
     *
     * @return The permission description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the set of roles (roles) associated with this permission.
     *
     * @return A {@link Set} of {@link Role} objects.
     */
    public Set<Role> getRoles() {
        // Return a defensive copy to prevent external modification of the internal set.
        // This ensures the integrity of the entity's state.
        return new HashSet<>(this.roles);
    }

//    public void setId(String id) {
//        // Setter for ID is typically not provided for entities with auto-generated IDs,
//        // as changing the ID after creation can lead to inconsistencies.
//        // If an ID needs to be set, it should be done via constructors or specific builder methods.
//        this.id = id;
//    }

    /**
     * Sets the name of the permission.
     *
     * @param name The new name for the permission.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the description of the permission.
     *
     * @param description The new description for the permission.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the set of roles associated with this permission.
     *
     * @param roles The new set of roles.
     */
    public void setAuthorities(Set<Role> roles) {
        // Perform a defensive copy to prevent external modification of the internal set.
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
    }

    /**
     * Returns a string representation of the PermissionsEntity, including its ID, name, and description.
     * This is useful for logging and debugging.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return "PermissionsEntity(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ")";
    }

    /**
     * Builder class for {@link PermissionsEntity}.
     * Provides a fluent API for constructing PermissionsEntity objects.
     */
    public static class PermissionsEntityBuilder {
        private String id;
        private String name;
        private String description;
        private Set<Role> roles;

        /**
         * Private constructor to enforce usage of {@link PermissionsEntity#builder()}.
         */
        PermissionsEntityBuilder() {
        }

        /**
         * Sets the ID for the permission.
         *
         * @param id The permission ID.
         * @return The builder instance.
         */
        public PermissionsEntityBuilder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name for the permission.
         *
         * @param name The permission name.
         * @return The builder instance.
         */
        public PermissionsEntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description for the permission.
         *
         * @param description The permission description.
         * @return The builder instance.
         */
        public PermissionsEntityBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the roles for the permission.
         *
         * @param roles The set of roles.
         * @return The builder instance.
         */
        public PermissionsEntityBuilder roles(Set<Role> roles) {
            // Defensive copy to ensure the builder's internal state is not modified externally.
            this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
            return this;
        }

        /**
         * Builds a new {@link PermissionsEntity} instance using the values set in the builder.
         *
         * @return A new PermissionsEntity object.
         */
        public PermissionsEntity build() {
            return new PermissionsEntity(this.id, this.name, this.description, this.roles);
        }

        /**
         * Returns a string representation of the builder's current state.
         *
         * @return A string representation of the builder.
         */
        @Override
        public String toString() {
            return "PermissionsEntity.PermissionsEntityBuilder(id=" + this.id + ", name=" + this.name + ", description=" + this.description + ", roles=" + this.roles + ")";
        }
    }
}

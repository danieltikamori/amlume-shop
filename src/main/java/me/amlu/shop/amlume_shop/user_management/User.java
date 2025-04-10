/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.model.address.Address;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Entity
@Table(name = "_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_name"),
        @UniqueConstraint(columnNames = "user_email")
}, indexes = {
        @Index(name = "idx_user_name", columnList = "user_name"),
        @Index(name = "idx_user_email", columnList = "user_email"),
//        @Index(name = "idx_user_orders", columnList = "user_orders")
})
@Getter
@Setter // Added Setter for JPA/Hibernate and potential controlled modifications
@NoArgsConstructor // Required by JPA
@SuperBuilder // Use Lombok's builder
@ToString // Be mindful of performance impact if logging frequently
public class User extends BaseEntity implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Embedded
    private AuthenticationInfo authenticationInfo;

    @Embedded
    private ContactInfo contactInfo;

    @Embedded
    private AccountStatus accountStatus;

    @Embedded
    private MfaInfo mfaInfo;

    @Embedded
    private DeviceFingerprintingInfo deviceFingerprintingInfo;

    @Embedded
    private LocationInfo locationInfo;

    @ElementCollection(fetch = FetchType.EAGER) // EAGER fetch for roles is often acceptable
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))

    private Set<UserRole> roles = new HashSet<>(); // Initialize collection

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinTable(name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id"))
    @ToString.Exclude
    private List<Address> addresses = new ArrayList<>(); // Initialize collection

    @OneToMany(mappedBy = "categoryManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private List<Category> categories = new ArrayList<>(); // Initialize collection

    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private Set<Product> products = new HashSet<>(); // Initialize collection

    // Initialized collection
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true) // Added orphanRemoval=true for consistency
    @ToString.Exclude
    private List<RefreshToken> refreshTokens = new ArrayList<>(); // Initialize collection


    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()  // Assuming 'roles' is a Set<UserRole> and Role has a 'name' attribute
                .map(role -> {
                    assert role.getRoleName() != null;
                    return new SimpleGrantedAuthority(role.getRoleName().name());
                }) // Convert Role to GrantedAuthority. Call name() method on the enum
//                .collect(Collectors.toList());
                .toList();
//        return roles;
    }

    @Override
    public String getPassword() {
        // Ensure authenticationInfo is not null for safety, though it shouldn't be with @Embedded
        return (authenticationInfo != null) ? authenticationInfo.getPassword() : null;
    }

    @Override
    public String getUsername() {
        // Ensure authenticationInfo is not null
        return (authenticationInfo != null) ? authenticationInfo.getUsername() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isEnabled();
    }
    // --- End UserDetails ---


    // --- Auditable Implementation ---
    // Method required by BaseEntity's isNew() logic
    @Override
    @Transient
    public Long getAuditableId() {
        return this.userId;
    }
    // --- End Auditable ---


    // --- Other Methods ---

    // Optional convenience getter for ID
    public Long getId() {
        return this.userId;
    }

    // Simple role check helper
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    // Check device fingerprinting status
    public boolean isDeviceFingerprintingEnabled() {
        return deviceFingerprintingInfo != null && deviceFingerprintingInfo.isDeviceFingerprintingEnabled();
    }

    // Proxy-aware implementation of equals()
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
        // Now safe to cast (though instanceof check is slightly redundant now)
        if (!(o instanceof User user)) return false;
        // Finally, compare by ID
        return getUserId() != null && Objects.equals(getUserId(), user.getUserId());
    }

    // Proxy-awareness is needed for hashCode if it uses getClass()
    @Override
    public final int hashCode() {
        // Base the hashCode primarily on the unique identifier (userId)
        // Objects.hash() handles null correctly if the entity is new (ID not yet assigned)
        return Objects.hash(userId);

        // --- Alternative using a constant for null ID (also common) ---
        // return userId == null ? 31 : userId.hashCode();
    }

}

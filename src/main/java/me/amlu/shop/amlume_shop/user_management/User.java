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
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private @NotBlank Set<UserRole> roles = new HashSet<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinTable(name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id"))
    @ToString.Exclude
    private transient List<Address> addresses = new CopyOnWriteArrayList<>();

    @OneToMany(mappedBy = "categoryManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private transient List<Category> categories = new CopyOnWriteArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private transient Set<Product> products;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude // CascadeType.ALL is important
    private List<RefreshToken> refreshTokens;

    public User(AuthenticationInfo authenticationInfo, ContactInfo contactInfo, AccountStatus accountStatus, MfaInfo mfaInfo, DeviceFingerprintingInfo deviceFingerprintingInfo, LocationInfo locationInfo, Set<UserRole> roles, List<Address> addresses, List<Category> categories, Set<Product> products, List<RefreshToken> refreshTokens) {
        this.authenticationInfo = authenticationInfo;
        this.contactInfo = contactInfo;
        this.accountStatus = accountStatus;
        this.mfaInfo = mfaInfo;
        this.deviceFingerprintingInfo = deviceFingerprintingInfo;
        this.locationInfo = locationInfo;
        this.roles = roles;
        this.addresses = addresses;
        this.categories = categories;
        this.products = products;
        this.refreshTokens = refreshTokens;
    }

    public User(AuthenticationInfo authenticationInfo, ContactInfo contactInfo, AccountStatus accountStatus, MfaInfo mfaInfo, LocationInfo locationInfo, Set<UserRole> roles) {
        this.authenticationInfo = authenticationInfo;
        this.contactInfo = contactInfo;
        this.accountStatus = accountStatus;
        this.mfaInfo = mfaInfo;
        this.deviceFingerprintingInfo = new DeviceFingerprintingInfo(true); // Default to true for new users
        this.locationInfo = locationInfo;
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()  // Assuming 'roles' is a Set<UserRole> and Role has a 'name' attribute
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name())) // Convert Role to GrantedAuthority. Call name() method on the enum
//                .collect(Collectors.toList());
                .toList();
//        return roles;
    }

    @Override
    public Long getId() {
        return this.userId;
    }

    @Override
    @Transient
    public Long getAuditableId() {
        return this.userId;
    }

    @Override
    public String getPassword() {
        return authenticationInfo.getPassword();
    }

    @Override
    public String getUsername() {
        return authenticationInfo.getUsername();
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountStatus.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return accountStatus.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return accountStatus.isEnabled();
    }


    public boolean isDeviceFingerprintingEnabled() {
        return deviceFingerprintingInfo != null && deviceFingerprintingInfo.isDeviceFingerprintingEnabled();
    }

    public void enableDeviceFingerprinting() {
        new User(
                this.authenticationInfo,
                this.contactInfo,
                this.accountStatus,
                this.mfaInfo,
                this.deviceFingerprintingInfo.enableFingerprinting(),
                this.locationInfo,
                this.roles,
                this.addresses,
                this.categories,
                this.products,
                this.refreshTokens
        );
    }

    public void disableDeviceFingerprinting() {
        new User(
                this.authenticationInfo,
                this.contactInfo,
                this.accountStatus,
                this.mfaInfo,
                this.deviceFingerprintingInfo.disableFingerprinting(),
                this.locationInfo,
                this.roles,
                this.addresses,
                this.categories,
                this.products,
                this.refreshTokens
        );
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

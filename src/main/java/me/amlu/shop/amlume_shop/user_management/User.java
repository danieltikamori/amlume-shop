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
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.model.address.Address;
import me.amlu.shop.amlume_shop.product_management.Product;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_name"),
        @UniqueConstraint(columnNames = "user_email")
}, indexes = {
        @Index(name = "idx_user_name", columnList = "user_name"),
        @Index(name = "idx_user_email", columnList = "user_email"),
//        @Index(name = "idx_user_orders", columnList = "user_orders")
})
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
    private List<Address> addresses = new ArrayList<>(); // Initialize collection

    @OneToMany(mappedBy = "categoryManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Category> categories = new ArrayList<>(); // Initialize collection

    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Product> products = new HashSet<>(); // Initialize collection

    // Initialized collection
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // Added orphanRemoval=true for consistency
    private List<RefreshToken> refreshTokens = new ArrayList<>(); // Initialize collection

    protected User() {
    }

    protected User(UserBuilder<?, ?> b) {
        super(b);
        this.userId = b.userId;
        this.authenticationInfo = b.authenticationInfo;
        this.contactInfo = b.contactInfo;
        this.accountStatus = b.accountStatus;
        this.mfaInfo = b.mfaInfo;
        this.deviceFingerprintingInfo = b.deviceFingerprintingInfo;
        this.locationInfo = b.locationInfo;
        this.roles = b.roles;
        this.addresses = b.addresses;
        this.categories = b.categories;
        this.products = b.products;
        this.refreshTokens = b.refreshTokens;
    }

    public static UserBuilder<?, ?> builder() {
        return new UserBuilderImpl();
    }


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

    // Check MFA status
    public boolean isMfaEnabled() {
        return mfaInfo != null && mfaInfo.isMfaEnabled();
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

    public Long getUserId() {
        return this.userId;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return this.authenticationInfo;
    }

    public ContactInfo getContactInfo() {
        return this.contactInfo;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public MfaInfo getMfaInfo() {
        return this.mfaInfo;
    }

    public DeviceFingerprintingInfo getDeviceFingerprintingInfo() {
        return this.deviceFingerprintingInfo;
    }

    public LocationInfo getLocationInfo() {
        return this.locationInfo;
    }

    public Set<UserRole> getRoles() {
        return this.roles;
    }

    public List<Address> getAddresses() {
        return this.addresses;
    }

    public List<Category> getCategories() {
        return this.categories;
    }

    public Set<Product> getProducts() {
        return this.products;
    }

    public List<RefreshToken> getRefreshTokens() {
        return this.refreshTokens;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAuthenticationInfo(AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public void setMfaInfo(MfaInfo mfaInfo) {
        this.mfaInfo = mfaInfo;
    }

    public void setDeviceFingerprintingInfo(DeviceFingerprintingInfo deviceFingerprintingInfo) {
        this.deviceFingerprintingInfo = deviceFingerprintingInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }

    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public String toString() {
        return "User(userId=" + this.getUserId() + ", authenticationInfo=" + this.getAuthenticationInfo() + ", contactInfo=" + this.getContactInfo() + ", accountStatus=" + this.getAccountStatus() + ", mfaInfo=" + this.getMfaInfo() + ", deviceFingerprintingInfo=" + this.getDeviceFingerprintingInfo() + ", locationInfo=" + this.getLocationInfo() + ", roles=" + this.getRoles() + ")";
    }

    public static abstract class UserBuilder<C extends User, B extends UserBuilder<C, B>> extends BaseEntityBuilder<C, B> {
        private Long userId;
        private AuthenticationInfo authenticationInfo;
        private ContactInfo contactInfo;
        private AccountStatus accountStatus;
        private MfaInfo mfaInfo;
        private DeviceFingerprintingInfo deviceFingerprintingInfo;
        private LocationInfo locationInfo;
        private Set<UserRole> roles;
        private List<Address> addresses;
        private List<Category> categories;
        private Set<Product> products;
        private List<RefreshToken> refreshTokens;

        public B userId(Long userId) {
            this.userId = userId;
            return self();
        }

        public B authenticationInfo(AuthenticationInfo authenticationInfo) {
            this.authenticationInfo = authenticationInfo;
            return self();
        }

        public B contactInfo(ContactInfo contactInfo) {
            this.contactInfo = contactInfo;
            return self();
        }

        public B accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return self();
        }

        public B mfaInfo(MfaInfo mfaInfo) {
            this.mfaInfo = mfaInfo;
            return self();
        }

        public B deviceFingerprintingInfo(DeviceFingerprintingInfo deviceFingerprintingInfo) {
            this.deviceFingerprintingInfo = deviceFingerprintingInfo;
            return self();
        }

        public B locationInfo(LocationInfo locationInfo) {
            this.locationInfo = locationInfo;
            return self();
        }

        public B roles(Set<UserRole> roles) {
            this.roles = roles;
            return self();
        }

        public B addresses(List<Address> addresses) {
            this.addresses = addresses;
            return self();
        }

        public B categories(List<Category> categories) {
            this.categories = categories;
            return self();
        }

        public B products(Set<Product> products) {
            this.products = products;
            return self();
        }

        public B refreshTokens(List<RefreshToken> refreshTokens) {
            this.refreshTokens = refreshTokens;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        @Override
        public String toString() {
            return "User.UserBuilder(super=" + super.toString() + ", userId=" + this.userId + ", authenticationInfo=" + this.authenticationInfo + ", contactInfo=" + this.contactInfo + ", accountStatus=" + this.accountStatus + ", mfaInfo=" + this.mfaInfo + ", deviceFingerprintingInfo=" + this.deviceFingerprintingInfo + ", locationInfo=" + this.locationInfo + ", roles=" + this.roles + ", addresses=" + this.addresses + ", categories=" + this.categories + ", products=" + this.products + ", refreshTokens=" + this.refreshTokens + ")";
        }
    }

    private static final class UserBuilderImpl extends UserBuilder<User, UserBuilderImpl> {
        private UserBuilderImpl() {
        }

        protected UserBuilderImpl self() {
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}

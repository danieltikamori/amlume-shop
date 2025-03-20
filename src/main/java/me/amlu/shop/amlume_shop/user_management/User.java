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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.config.Phone;
import me.amlu.shop.amlume_shop.model.*;
import me.amlu.shop.amlume_shop.model.address.Address;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Entity
@Table(name = "_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_name"),
        @UniqueConstraint(columnNames = "user_email")
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

//    @NotBlank
//    @Size(max = 20)
//    @Column(nullable = false, unique = true)
//    private String userFirstName;
//
//    @NotBlank
//    @Size(max = 20)
//    @Column(nullable = false, unique = true)
//    private String userLastName;

    @NotBlank
    @Size(min = 3, max = 20)
    @Column(nullable = false, unique = true, name = "user_name")
    private String username;

    @NotBlank
    @Size(min = 5, max = 50)
    @Email
    @Column(nullable = false, unique = true, name = "user_email")
    private String userEmail;

    @Column(name = "user_email_verified", nullable = false)
    @Builder.Default
    private boolean userEmailVerified = false;

    @NotBlank
    @Size(min = 12, max = 120, message = "Password must be between 12 and 120 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, name = "user_password")
    private String password;  // This will store the Argon2id or other hash

//    @NotBlank
    @Phone
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters")
    @Column(name = "phone_number")
    private Phonenumber.PhoneNumber phoneNumber;

    @Column(name = "user_department")
    private String department;

    @Column(name = "user_region")
    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private @NotBlank Set<UserRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "categoryManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private transient List<Category> categories = new CopyOnWriteArrayList<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinTable(name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id"))
    @ToString.Exclude
    private transient List<Address> addresses = new CopyOnWriteArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private transient Set<Product> products;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "mfa_enforced")
    private boolean mfaEnforced;

    @Column(name = "mfa_qr_code_url")
    private String mfaQrCodeUrl;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude // CascadeType.ALL is important
    private List<RefreshToken> refreshTokens;

    @Column(name = "device_fingerprinting_enabled")
    @Builder.Default
    private boolean deviceFingerprintingEnabled = true;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private transient List<UserDeviceFingerprint> deviceFingerprints = new CopyOnWriteArrayList<>();


    @Column(name = "user_locked", nullable = false)
    @Builder.Default
    private boolean userLocked = false;

    @Column(name = "lock_time")
    private Instant lockTime;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true; // For account locking (e.g. for failed login attempts). Non locked = true

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Column(name = "account_non_expired", nullable = false)
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()  // Assuming 'roles' is a Set<UserRole> and Role has a 'name' attribute
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name())) // Convert Role to GrantedAuthority. Call name() method on the enum
//                .collect(Collectors.toList());
                .toList();
//        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof User user)) return false;
        return getUserId() != null && Objects.equals(getUserId(), user.getUserId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}

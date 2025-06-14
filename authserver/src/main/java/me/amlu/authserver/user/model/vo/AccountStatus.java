/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Crucial security feature.
 *
 * @author Daniel Itiro Tikamori
 */

@Embeddable
public class AccountStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "lockout_expiration_time") // Nullable
    private Instant lockoutExpirationTime;

    // Fields for UserDetails compatibility, can be enhanced with specific policies later
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired;

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired;

    // Private constructor for builder or factory methods
    private AccountStatus(boolean enabled, int failedLoginAttempts, Instant lockoutExpirationTime,
                          boolean accountNonExpired, boolean credentialsNonExpired) {
        this.enabled = enabled;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutExpirationTime = lockoutExpirationTime;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.emailVerified = false; // Default to unverified
    }

    public AccountStatus() {
    }

    // Factory method for a new, default account status
    public static AccountStatus initial() {
        AccountStatus status = new AccountStatus(true, 0, null, true, true);
        status.emailVerified = false; // Explicitly set to false for clarity
        return status;
    }

    // Factory method for a new account with verified email
    public static AccountStatus initialVerified() {
        AccountStatus status = new AccountStatus(true, 0, null, true, true);
        status.emailVerified = true;
        return status;
    }

    // --- Behavioral methods for the VO itself ---

    public AccountStatus recordLoginFailure() {
        int newAttempts = this.failedLoginAttempts + 1;
        return new AccountStatus(this.enabled, newAttempts, this.lockoutExpirationTime, this.accountNonExpired, this.credentialsNonExpired);
    }

    public AccountStatus resetLoginFailures() {
        // Also unlocks if it was locked due to attempts and not explicitly disabled
        return new AccountStatus(this.enabled, 0, null, this.accountNonExpired, this.credentialsNonExpired);
    }

    public AccountStatus lockUntil(Instant expirationTime) {
        Assert.notNull(expirationTime, "Lockout expiration time cannot be null.");
        return new AccountStatus(this.enabled, this.failedLoginAttempts, expirationTime, this.accountNonExpired, this.credentialsNonExpired);
    }

    public AccountStatus unlock() {
        // Only resets lockout due to failed attempts. Does not change 'enabled' status.
        return new AccountStatus(this.enabled, this.failedLoginAttempts, null, this.accountNonExpired, this.credentialsNonExpired);
    }

    public AccountStatus enable() {
        return new AccountStatus(true, this.failedLoginAttempts, this.lockoutExpirationTime, this.accountNonExpired, this.credentialsNonExpired);
    }

    public AccountStatus disable() {
        // Disabling an account might also imply locking it indefinitely,
        // but 'enabled' is the primary flag here.
        return new AccountStatus(false, this.failedLoginAttempts, this.lockoutExpirationTime, this.accountNonExpired, this.credentialsNonExpired);
    }

    // --- Methods for Email ---

    public AccountStatus verifyEmail() {
        AccountStatus status = new AccountStatus(this.enabled, this.failedLoginAttempts, this.lockoutExpirationTime, this.accountNonExpired, this.credentialsNonExpired);
        status.emailVerified = true;
        return status;
    }

    public AccountStatus unverifyEmail() {
        AccountStatus status = new AccountStatus(this.enabled, this.failedLoginAttempts, this.lockoutExpirationTime, this.accountNonExpired, this.credentialsNonExpired);
        status.emailVerified = false;
        return status;
    }

    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    // --- Methods for UserDetails compatibility ---
    public boolean isAccountNonLocked() {
        return this.lockoutExpirationTime == null || Instant.now().isAfter(this.lockoutExpirationTime);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public int getFailedLoginAttempts() {
        return this.failedLoginAttempts;
    }

    public Instant getLockoutExpirationTime() {
        return this.lockoutExpirationTime;
    }

    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    // toString, equals, hashCode can be added if needed, but for @Embeddable,
    // they are often not critical if the containing entity handles its identity.
    // Lombok's @Value could also be used if you prefer full immutability and auto-generated methods.
}

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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class AccountStatus implements Serializable {

    @Column(name = "user_locked", nullable = false)
    private boolean userLocked = false;

    @Column(name = "lock_time")
    private Instant lockTime;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true; // For account locking (e.g. for failed login attempts). Non locked = true

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    public AccountStatus(boolean userLocked, Instant lockTime, int failedLoginAttempts, boolean accountNonLocked, Instant lastLoginTime, boolean accountNonExpired, boolean credentialsNonExpired, boolean enabled, Instant creationTime) {
        this.userLocked = userLocked;
        this.lockTime = lockTime;
        this.failedLoginAttempts = failedLoginAttempts;
        this.accountNonLocked = accountNonLocked;
        this.lastLoginTime = lastLoginTime;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.creationTime = creationTime;
    }

    protected AccountStatus() { // For JPA
    }

    private static boolean $default$userLocked() {
        return false;
    }

    private static int $default$failedLoginAttempts() {
        return 0;
    }

    private static boolean $default$accountNonLocked() {
        return true;
    }

    private static boolean $default$accountNonExpired() {
        return true;
    }

    private static boolean $default$enabled() {
        return true;
    }

    public static AccountStatusBuilder builder() {
        return new AccountStatusBuilder();
    }

    public boolean isUserLocked() {
        return this.userLocked;
    }

    public Instant getLockTime() {
        return this.lockTime;
    }

    public int getFailedLoginAttempts() {
        return this.failedLoginAttempts;
    }

    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    public Instant getLastLoginTime() {
        return this.lastLoginTime;
    }

    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AccountStatus other)) return false;
        if (!other.canEqual((Object) this)) return false;
        if (this.isUserLocked() != other.isUserLocked()) return false;
        final Object this$lockTime = this.getLockTime();
        final Object other$lockTime = other.getLockTime();
        if (!Objects.equals(this$lockTime, other$lockTime)) return false;
        if (this.getFailedLoginAttempts() != other.getFailedLoginAttempts()) return false;
        if (this.isAccountNonLocked() != other.isAccountNonLocked()) return false;
        final Object this$lastLoginTime = this.getLastLoginTime();
        final Object other$lastLoginTime = other.getLastLoginTime();
        if (!Objects.equals(this$lastLoginTime, other$lastLoginTime))
            return false;
        if (this.isAccountNonExpired() != other.isAccountNonExpired()) return false;
        if (this.isCredentialsNonExpired() != other.isCredentialsNonExpired()) return false;
        if (this.isEnabled() != other.isEnabled()) return false;
        final Object this$creationTime = this.getCreationTime();
        final Object other$creationTime = other.getCreationTime();
        return Objects.equals(this$creationTime, other$creationTime);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof AccountStatus;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isUserLocked() ? 79 : 97);
        final Object $lockTime = this.getLockTime();
        result = result * PRIME + ($lockTime == null ? 43 : $lockTime.hashCode());
        result = result * PRIME + this.getFailedLoginAttempts();
        result = result * PRIME + (this.isAccountNonLocked() ? 79 : 97);
        final Object $lastLoginTime = this.getLastLoginTime();
        result = result * PRIME + ($lastLoginTime == null ? 43 : $lastLoginTime.hashCode());
        result = result * PRIME + (this.isAccountNonExpired() ? 79 : 97);
        result = result * PRIME + (this.isCredentialsNonExpired() ? 79 : 97);
        result = result * PRIME + (this.isEnabled() ? 79 : 97);
        final Object $creationTime = this.getCreationTime();
        result = result * PRIME + ($creationTime == null ? 43 : $creationTime.hashCode());
        return result;
    }

    public static class AccountStatusBuilder {
        private boolean userLocked$value;
        private boolean userLocked$set;
        private Instant lockTime;
        private int failedLoginAttempts$value;
        private boolean failedLoginAttempts$set;
        private boolean accountNonLocked$value;
        private boolean accountNonLocked$set;
        private Instant lastLoginTime;
        private boolean accountNonExpired$value;
        private boolean accountNonExpired$set;
        private boolean credentialsNonExpired;
        private boolean enabled$value;
        private boolean enabled$set;
        private Instant creationTime;

        AccountStatusBuilder() {
        }

        public AccountStatusBuilder userLocked(boolean userLocked) {
            this.userLocked$value = userLocked;
            this.userLocked$set = true;
            return this;
        }

        public AccountStatusBuilder lockTime(Instant lockTime) {
            this.lockTime = lockTime;
            return this;
        }

        public AccountStatusBuilder failedLoginAttempts(int failedLoginAttempts) {
            this.failedLoginAttempts$value = failedLoginAttempts;
            this.failedLoginAttempts$set = true;
            return this;
        }

        public AccountStatusBuilder accountNonLocked(boolean accountNonLocked) {
            this.accountNonLocked$value = accountNonLocked;
            this.accountNonLocked$set = true;
            return this;
        }

        public AccountStatusBuilder lastLoginTime(Instant lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
            return this;
        }

        public AccountStatusBuilder accountNonExpired(boolean accountNonExpired) {
            this.accountNonExpired$value = accountNonExpired;
            this.accountNonExpired$set = true;
            return this;
        }

        public AccountStatusBuilder credentialsNonExpired(boolean credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
            return this;
        }

        public AccountStatusBuilder enabled(boolean enabled) {
            this.enabled$value = enabled;
            this.enabled$set = true;
            return this;
        }

        public AccountStatusBuilder creationTime(Instant creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public AccountStatus build() {
            boolean userLocked$value = this.userLocked$value;
            if (!this.userLocked$set) {
                userLocked$value = AccountStatus.$default$userLocked();
            }
            int failedLoginAttempts$value = this.failedLoginAttempts$value;
            if (!this.failedLoginAttempts$set) {
                failedLoginAttempts$value = AccountStatus.$default$failedLoginAttempts();
            }
            boolean accountNonLocked$value = this.accountNonLocked$value;
            if (!this.accountNonLocked$set) {
                accountNonLocked$value = AccountStatus.$default$accountNonLocked();
            }
            boolean accountNonExpired$value = this.accountNonExpired$value;
            if (!this.accountNonExpired$set) {
                accountNonExpired$value = AccountStatus.$default$accountNonExpired();
            }
            boolean enabled$value = this.enabled$value;
            if (!this.enabled$set) {
                enabled$value = AccountStatus.$default$enabled();
            }
            return new AccountStatus(userLocked$value, this.lockTime, failedLoginAttempts$value, accountNonLocked$value, this.lastLoginTime, accountNonExpired$value, this.credentialsNonExpired, enabled$value, this.creationTime);
        }

        @Override
        public String toString() {
            return "AccountStatus.AccountStatusBuilder(userLocked$value=" + this.userLocked$value + ", lockTime=" + this.lockTime + ", failedLoginAttempts$value=" + this.failedLoginAttempts$value + ", accountNonLocked$value=" + this.accountNonLocked$value + ", lastLoginTime=" + this.lastLoginTime + ", accountNonExpired$value=" + this.accountNonExpired$value + ", credentialsNonExpired=" + this.credentialsNonExpired + ", enabled$value=" + this.enabled$value + ", creationTime=" + this.creationTime + ")";
        }
    }
}
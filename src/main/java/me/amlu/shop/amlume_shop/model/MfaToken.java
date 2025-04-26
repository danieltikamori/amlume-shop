/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.user_management.User;

import java.util.Objects;

@Table(name = "mfa_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
}, indexes = @Index(name = "idx_user_id", columnList = "user_id"))
@Entity
public class MfaToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mfaTokenId;

    @OneToOne(fetch = FetchType.LAZY)  // Use @OneToOne for a one-to-one relationship with User
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    // Foreign key to User
    private User user;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean enabled = false;

    public MfaToken(User user, String secret, boolean mfaEnforced) {
    }

    public MfaToken(Long mfaTokenId, User user, String secret, boolean enabled) {
        this.mfaTokenId = mfaTokenId;
        this.user = user;
        this.secret = secret;
        this.enabled = enabled;
    }

    public MfaToken() {
    }

    protected MfaToken(MfaTokenBuilder<?, ?> b) {
        super(b);
        this.mfaTokenId = b.mfaTokenId;
        this.user = b.user;
        this.secret = b.secret;
        if (b.enabled$set) {
            this.enabled = b.enabled$value;
        } else {
            this.enabled = $default$enabled();
        }
    }

    private static boolean $default$enabled() {
        return false;
    }

    public static MfaTokenBuilder<?, ?> builder() {
        return new MfaTokenBuilderImpl();
    }

//    // Constructor with builder pattern
//    private MfaToken(Builder builder) {
//        this.mfaTokenId = builder.mfaTokenId;
//        this.user = builder.user;
//        this.secret = builder.secret;
//        this.enabled = builder.enabled;
//
//    }

//    // No-args constructor required by JPA
//    public MfaToken() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MfaToken that)) return false;
        return mfaTokenId != null && mfaTokenId.equals(that.mfaTokenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mfaTokenId);
    }

    @Override
    public Long getAuditableId() {
        return mfaTokenId;
    }

    @Override
    public Long getId() {
        return mfaTokenId;
    }

    public Long getMfaTokenId() {
        return this.mfaTokenId;
    }

    public User getUser() {
        return this.user;
    }

    public String getSecret() {
        return this.secret;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setMfaTokenId(Long mfaTokenId) {
        this.mfaTokenId = mfaTokenId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String toString() {
        return "MfaToken(mfaTokenId=" + this.getMfaTokenId() + ", secret=" + this.getSecret() + ", enabled=" + this.isEnabled() + ")";
    }

    public static abstract class MfaTokenBuilder<C extends MfaToken, B extends MfaTokenBuilder<C, B>> extends BaseEntityBuilder<C, B> {
        private Long mfaTokenId;
        private User user;
        private String secret;
        private boolean enabled$value;
        private boolean enabled$set;

        public B mfaTokenId(Long mfaTokenId) {
            this.mfaTokenId = mfaTokenId;
            return self();
        }

        public B user(User user) {
            this.user = user;
            return self();
        }

        public B secret(String secret) {
            this.secret = secret;
            return self();
        }

        public B enabled(boolean enabled) {
            this.enabled$value = enabled;
            this.enabled$set = true;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "MfaToken.MfaTokenBuilder(super=" + super.toString() + ", mfaTokenId=" + this.mfaTokenId + ", user=" + this.user + ", secret=" + this.secret + ", enabled$value=" + this.enabled$value + ")";
        }
    }

    private static final class MfaTokenBuilderImpl extends MfaTokenBuilder<MfaToken, MfaTokenBuilderImpl> {
        private MfaTokenBuilderImpl() {
        }

        protected MfaTokenBuilderImpl self() {
            return this;
        }

        public MfaToken build() {
            return new MfaToken(this);
        }
    }

//    // Builder pattern implementation
//    public static class Builder {
//        private Long mfaTokenId;
//        private User user;
//        private String secret;
//        private boolean enabled;
//
//        public Builder mfaTokenId(Long mfaTokenId) {
//            this.mfaTokenId = mfaTokenId;
//            return this;
//        }
//
//        public Builder user(User user) {
//            this.user = user;
//            return this;
//        }
//
//        public Builder secret(String secret) {
//            this.secret = secret;
//            return this;
//        }
//
//        public Builder enabled(boolean enabled) {
//            this.enabled = enabled;
//            return this;
//        }
//
//        public MfaToken build() {
//            return new MfaToken(this);
//        }
//    }
}

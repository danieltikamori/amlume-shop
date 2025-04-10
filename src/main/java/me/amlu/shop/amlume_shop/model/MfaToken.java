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
import lombok.*;
import lombok.experimental.SuperBuilder;
import me.amlu.shop.amlume_shop.user_management.User;

import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
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
    @ToString.Exclude // Foreign key to User
    private User user;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    public MfaToken(User user, String secret, boolean mfaEnforced) {
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

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
import jakarta.persistence.Embedded;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MfaInfo implements Serializable {

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_enforced")
    private boolean mfaEnforced;

    @Embedded
    private MfaQrCodeUrl mfaQrCodeUrl;

    @Embedded
    private MfaSecret mfaSecret;


    public MfaInfo(boolean mfaEnabled, boolean mfaEnforced, MfaQrCodeUrl mfaQrCodeUrl, MfaSecret mfaSecret) {
        this.mfaEnabled = mfaEnabled;
        this.mfaEnforced = mfaEnforced;
        this.mfaQrCodeUrl = mfaQrCodeUrl;
        this.mfaSecret = mfaSecret;
    }

    protected MfaInfo() { // Required by JPA
    }

    private static boolean $default$mfaEnabled() {
        return false;
    }

    public static MfaInfoBuilder builder() {
        return new MfaInfoBuilder();
    }

    String getMfaQrCodeUrl() {
        return mfaQrCodeUrl.getMfaQrCodeUrlValue();
    }

    String getMfaSecret() {
        return mfaSecret.getMfaSecretValue();
    }

    public boolean isMfaEnabled() {
        return this.mfaEnabled;
    }

    public boolean isMfaEnforced() {
        return this.mfaEnforced;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MfaInfo other)) return false;
        if (!other.canEqual((Object) this)) return false;
        if (this.isMfaEnabled() != other.isMfaEnabled()) return false;
        if (this.isMfaEnforced() != other.isMfaEnforced()) return false;
        final Object this$mfaQrCodeUrl = this.getMfaQrCodeUrl();
        final Object other$mfaQrCodeUrl = other.getMfaQrCodeUrl();
        if (!Objects.equals(this$mfaQrCodeUrl, other$mfaQrCodeUrl))
            return false;
        final Object this$mfaSecret = this.getMfaSecret();
        final Object other$mfaSecret = other.getMfaSecret();
        return Objects.equals(this$mfaSecret, other$mfaSecret);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MfaInfo;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isMfaEnabled() ? 79 : 97);
        result = result * PRIME + (this.isMfaEnforced() ? 79 : 97);
        final Object $mfaQrCodeUrl = this.getMfaQrCodeUrl();
        result = result * PRIME + ($mfaQrCodeUrl == null ? 43 : $mfaQrCodeUrl.hashCode());
        final Object $mfaSecret = this.getMfaSecret();
        result = result * PRIME + ($mfaSecret == null ? 43 : $mfaSecret.hashCode());
        return result;
    }

    public static class MfaInfoBuilder {
        private boolean mfaEnabled$value;
        private boolean mfaEnabled$set;
        private boolean mfaEnforced;
        private MfaQrCodeUrl mfaQrCodeUrl;
        private MfaSecret mfaSecret;

        MfaInfoBuilder() {
        }

        public MfaInfoBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled$value = mfaEnabled;
            this.mfaEnabled$set = mfaEnabled;
            return this;
        }

        public MfaInfoBuilder mfaEnforced(boolean mfaEnforced) {
            this.mfaEnforced = mfaEnforced;
            return this;
        }

        public MfaInfoBuilder mfaQrCodeUrl(MfaQrCodeUrl mfaQrCodeUrl) {
            this.mfaQrCodeUrl = mfaQrCodeUrl;
            return this;
        }

        public MfaInfoBuilder mfaSecret(MfaSecret mfaSecret) {
            this.mfaSecret = mfaSecret;
            return this;
        }

        public MfaInfo build() {
            boolean mfaEnabled$value = this.mfaEnabled$value;
            if (!this.mfaEnabled$set) {
                mfaEnabled$value = MfaInfo.$default$mfaEnabled();
            }
            return new MfaInfo(mfaEnabled$value, this.mfaEnforced, this.mfaQrCodeUrl, this.mfaSecret);
        }

        @Override
        public String toString() {
            return "MfaInfo.MfaInfoBuilder(mfaEnabled$value=" + this.mfaEnabled$value + ", mfaEnforced=" + this.mfaEnforced + ", mfaQrCodeUrl=" + this.mfaQrCodeUrl + ", mfaSecret=" + this.mfaSecret + ")";
        }
    }
}
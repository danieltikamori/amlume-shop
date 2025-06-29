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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DeviceFingerprintingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "device_fingerprinting_enabled")
    private boolean deviceFingerprintingEnabled;

    @Embedded
    private UserDeviceFingerprints deviceFingerprints;

    public DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled, UserDeviceFingerprints deviceFingerprints) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = deviceFingerprints;
    }

    public DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = null;
    }

    // Required for JPA
    protected DeviceFingerprintingInfo() {
        this.deviceFingerprintingEnabled = false;
        this.deviceFingerprints = null;
    }

    public static DeviceFingerprintingInfoBuilder builder() {
        return new DeviceFingerprintingInfoBuilder();
    }

    public DeviceFingerprintingInfo enableFingerprinting() {
//        Objects.requireNonNull(deviceFingerprints, "deviceFingerprints cannot be null");
        return new DeviceFingerprintingInfo(true, deviceFingerprints);
    }

    public DeviceFingerprintingInfo disableFingerprinting() {
        return new DeviceFingerprintingInfo(false, null);
    }

    public boolean isDeviceFingerprintingEnabled() {
        return this.deviceFingerprintingEnabled;
    }

    public UserDeviceFingerprints getDeviceFingerprints() {
        return this.deviceFingerprints;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeviceFingerprintingInfo other)) return false;
        if (!other.canEqual((Object) this)) return false;
        if (this.isDeviceFingerprintingEnabled() != other.isDeviceFingerprintingEnabled()) return false;
        final Object this$deviceFingerprints = this.getDeviceFingerprints();
        final Object other$deviceFingerprints = other.getDeviceFingerprints();
        return Objects.equals(this$deviceFingerprints, other$deviceFingerprints);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof DeviceFingerprintingInfo;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isDeviceFingerprintingEnabled() ? 79 : 97);
        final Object $deviceFingerprints = this.getDeviceFingerprints();
        result = result * PRIME + ($deviceFingerprints == null ? 43 : $deviceFingerprints.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DeviceFingerprintingInfo(deviceFingerprintingEnabled=" + this.isDeviceFingerprintingEnabled() + ", deviceFingerprints=" + this.getDeviceFingerprints() + ")";
    }

    public static class DeviceFingerprintingInfoBuilder {
        private boolean deviceFingerprintingEnabled;
        private UserDeviceFingerprints deviceFingerprints;

        DeviceFingerprintingInfoBuilder() {
        }

        public DeviceFingerprintingInfoBuilder deviceFingerprintingEnabled(boolean deviceFingerprintingEnabled) {
            this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
            return this;
        }

        public DeviceFingerprintingInfoBuilder deviceFingerprints(UserDeviceFingerprints deviceFingerprints) {
            this.deviceFingerprints = deviceFingerprints;
            return this;
        }

        public DeviceFingerprintingInfo build() {
            return new DeviceFingerprintingInfo(this.deviceFingerprintingEnabled, this.deviceFingerprints);
        }

        @Override
        public String toString() {
            return "DeviceFingerprintingInfo.DeviceFingerprintingInfoBuilder(deviceFingerprintingEnabled=" + this.deviceFingerprintingEnabled + ", deviceFingerprints=" + this.deviceFingerprints + ")";
        }
    }
}

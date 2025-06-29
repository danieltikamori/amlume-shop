/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Embeddable class that contains information about device fingerprinting settings for a user.
 * This class is intended to be used with the User entity.
 */
@Embeddable
public class DeviceFingerprintingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Flag indicating whether device fingerprinting is enabled for the user.
     * Default value is applied by the no-arg constructor or explicitly set in builder.
     */
    @Column(name = "device_fingerprinting_enabled")
    private boolean deviceFingerprintingEnabled = true;

    /**
     * Stores the most recently verified device fingerprint for the user's session.
     * This is the value that will be embedded in JWTs.
     */
    @Column(name = "current_device_fingerprint", length = 255)
    private String currentFingerprint;

    // Removed: @Embedded private @Nullable UserDeviceFingerprints deviceFingerprints;

    /**
     * Default constructor required by JPA.
     * Initializes with fingerprinting enabled and an empty set of known devices.
     */
    public DeviceFingerprintingInfo() {
        this.deviceFingerprintingEnabled = true;
    }

    /**
     * Constructor for creating a new DeviceFingerprintingInfo with the specified settings.
     *
     * @param deviceFingerprintingEnabled whether device fingerprinting is enabled.
     * @param currentFingerprint          the current session's device fingerprint.
     */
    private DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled, String currentFingerprint) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.currentFingerprint = currentFingerprint;
    }

    /**
     * Static factory method to create a new builder for this class.
     *
     * @return a new DeviceFingerprintingInfoBuilder instance.
     */
    public static DeviceFingerprintingInfoBuilder builder() {
        return new DeviceFingerprintingInfoBuilder();
    }


    /**
     * Creates a new, immutable copy of this object with device fingerprinting enabled.
     *
     * @return a new DeviceFingerprintingInfo instance.
     */
    public DeviceFingerprintingInfo enableFingerprinting() {
        return new DeviceFingerprintingInfo(true, this.currentFingerprint);
    }

    /**
     * Creates a new, immutable copy of this object with device fingerprinting disabled.
     * Clears the current fingerprint and history for security and privacy.
     *
     * @return a new DeviceFingerprintingInfo instance with device fingerprinting disabled
     */
    public DeviceFingerprintingInfo disableFingerprinting() {
        return new DeviceFingerprintingInfo(false, null);
    }

    // --- Getters and Setters ---

    /**
     * Checks if device fingerprinting is enabled.
     *
     * @return true if device fingerprinting is enabled, false otherwise
     */
    public boolean isDeviceFingerprintingEnabled() {
        return deviceFingerprintingEnabled;
    }

    /**
     * Get the current device fingerprint
     *
     * @return the current fingerprint
     */
    public String getCurrentFingerprint() {
        return this.currentFingerprint;
    }

    public void setDeviceFingerprintingEnabled(boolean deviceFingerprintingEnabled) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
    }

    /**
     * Sets the current active fingerprint for the session.
     *
     * @param currentFingerprint The new current fingerprint.
     */
    @NullMarked
    public void setCurrentFingerprint(String currentFingerprint) {
        this.currentFingerprint = currentFingerprint;
    }


    // --- equals, hashCode, toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceFingerprintingInfo that = (DeviceFingerprintingInfo) o;
        return deviceFingerprintingEnabled == that.deviceFingerprintingEnabled &&
                Objects.equals(currentFingerprint, that.currentFingerprint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceFingerprintingEnabled, currentFingerprint);
    }

    @Override
    public String toString() {
        return "DeviceFingerprintingInfo{" +
                "deviceFingerprintingEnabled=" + deviceFingerprintingEnabled +
                ", currentFingerprint='" + currentFingerprint + '\'' +
                '}';
    }


    /**
     * Manual Builder class for creating DeviceFingerprintingInfo instances.
     */
    public static class DeviceFingerprintingInfoBuilder {
        private boolean deviceFingerprintingEnabled = true;
        private String currentFingerprint;

        DeviceFingerprintingInfoBuilder() {
        }

        /**
         * Sets whether device fingerprinting is enabled.
         *
         * @param deviceFingerprintingEnabled whether device fingerprinting is enabled
         * @return this builder
         */
        public DeviceFingerprintingInfoBuilder deviceFingerprintingEnabled(boolean deviceFingerprintingEnabled) {
            this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
            return this;
        }

        /**
         * Sets the current device fingerprint
         *
         * @param currentFingerprint the current device fingerprint
         */
        public DeviceFingerprintingInfoBuilder currentFingerprint(String currentFingerprint) {
            this.currentFingerprint = currentFingerprint;
            return this;
        }

        /**
         * Builds a new DeviceFingerprintingInfo with the configured settings.
         *
         * @return a new DeviceFingerprintingInfo
         */
        public DeviceFingerprintingInfo build() {
            return new DeviceFingerprintingInfo(this.deviceFingerprintingEnabled, this.currentFingerprint);
        }

        @Override
        public String toString() {
            return "DeviceFingerprintingInfo.DeviceFingerprintingInfoBuilder(deviceFingerprintingEnabled=" +
                    this.deviceFingerprintingEnabled +
                    ", currentFingerprint=" + this.currentFingerprint +
                    ")";
        }
    }
}

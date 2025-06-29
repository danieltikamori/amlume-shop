/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.dto;

import java.time.Instant;

/**
 * Response DTO for user device information.
 * Contains information about a device that has been used to access a user's account.
 */
public record UserDeviceResponse(
        Long id,
        String deviceName,
        String deviceFingerprint,
        String browserInfo,
        String lastKnownIp,
        String location,
        String lastKnownCountry,
        Instant lastUsedAt,
        boolean active,
        boolean trusted,
        boolean currentDevice
) {
    /**
     * Creates a builder for UserDeviceResponse.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for UserDeviceResponse.
     */
    public static class Builder {
        private Long id;
        private String deviceName;
        private String deviceFingerprint;
        private String browserInfo;
        private String lastKnownIp;
        private String location;
        private String lastKnownCountry;
        private Instant lastUsedAt;
        private boolean active;
        private boolean trusted;
        private boolean currentDevice;

        /**
         * Sets the device ID.
         *
         * @param id the device ID
         * @return this builder
         */
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the device name.
         *
         * @param deviceName the device name
         * @return this builder
         */
        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        /**
         * Sets the device fingerprint.
         *
         * @param deviceFingerprint the device fingerprint
         * @return this builder
         */
        public Builder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        /**
         * Sets the browser information.
         *
         * @param browserInfo the browser information
         * @return this builder
         */
        public Builder browserInfo(String browserInfo) {
            this.browserInfo = browserInfo;
            return this;
        }

        /**
         * Sets the last known IP address.
         *
         * @param lastKnownIp the last known IP address
         * @return this builder
         */
        public Builder lastKnownIp(String lastKnownIp) {
            this.lastKnownIp = lastKnownIp;
            return this;
        }

        /**
         * Sets the location.
         *
         * @param location the location
         * @return this builder
         */
        public Builder location(String location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the last known country.
         *
         * @param lastKnownCountry the last known country
         * @return this builder
         */
        public Builder lastKnownCountry(String lastKnownCountry) {
            this.lastKnownCountry = lastKnownCountry;
            return this;
        }

        /**
         * Sets the last used timestamp.
         *
         * @param lastUsedAt the last used timestamp
         * @return this builder
         */
        public Builder lastUsedAt(Instant lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        /**
         * Sets whether the device is active.
         *
         * @param active whether the device is active
         * @return this builder
         */
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Sets whether the device is trusted.
         *
         * @param trusted whether the device is trusted
         * @return this builder
         */
        public Builder trusted(boolean trusted) {
            this.trusted = trusted;
            return this;
        }

        /**
         * Sets whether this is the current device.
         *
         * @param currentDevice whether this is the current device
         * @return this builder
         */
        public Builder currentDevice(boolean currentDevice) {
            this.currentDevice = currentDevice;
            return this;
        }

        /**
         * Builds a new UserDeviceResponse with the configured settings.
         *
         * @return a new UserDeviceResponse
         */
        public UserDeviceResponse build() {
            return new UserDeviceResponse(
                    id,
                    deviceName,
                    deviceFingerprint,
                    browserInfo,
                    lastKnownIp,
                    location,
                    lastKnownCountry,
                    lastUsedAt,
                    active,
                    trusted,
                    currentDevice
            );
        }
    }
}

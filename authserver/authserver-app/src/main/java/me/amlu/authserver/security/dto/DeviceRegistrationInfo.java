/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for encapsulating all relevant device information during registration or update.
 * This ensures that all necessary details are passed together, improving method clarity and data completeness.
 */
public record DeviceRegistrationInfo(
        @NotBlank String deviceFingerprint,
        @NotBlank String browserInfo,
        @NotBlank String lastKnownIp,
        String location, // Can be null if GeoIP lookup fails
        String lastKnownCountry, // Can be null if GeoIP lookup fails
        @NotBlank String deviceName,
        @NotBlank String source
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String deviceFingerprint;
        private String browserInfo;
        private String lastKnownIp;
        private String location;
        private String lastKnownCountry;
        private String deviceName;
        private String source;

        public Builder deviceFingerprint(String deviceFingerprint) {
            this.deviceFingerprint = deviceFingerprint;
            return this;
        }

        public Builder browserInfo(String browserInfo) {
            this.browserInfo = browserInfo;
            return this;
        }

        public Builder lastKnownIp(String lastKnownIp) {
            this.lastKnownIp = lastKnownIp;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder lastKnownCountry(String lastKnownCountry) {
            this.lastKnownCountry = lastKnownCountry;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public DeviceRegistrationInfo build() {
            return new DeviceRegistrationInfo(
                    deviceFingerprint,
                    browserInfo,
                    lastKnownIp,
                    location,
                    lastKnownCountry,
                    deviceName,
                    source
            );
        }
    }
}

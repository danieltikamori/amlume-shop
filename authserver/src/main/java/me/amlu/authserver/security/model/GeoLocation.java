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

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents geographic location information derived from an IP address.
 * This is an immutable value object implemented as a Java record.
 * It includes a sentinel 'UNKNOWN' instance to avoid null checks in the application.
 */
public record GeoLocation(
        String countryCode,
        String countryName,
        String city,
        String postalCode,
        Double latitude,
        Double longitude,
        String timeZone,
        String subdivisionName,
        String subdivisionCode,
        String asn
) implements Serializable {

    // Constants for representing unknown values
    public static final String UNKNOWN_VALUE = "UNKNOWN";
    // ISO 3166-1 alpha-2 code for "No Country" (user-assigned)
    public static final String UNKNOWN_COUNTRY_CODE = "XX";

    // The singleton sentinel instance for an unknown location.
    private static final GeoLocation UNKNOWN = new GeoLocation(
            UNKNOWN_COUNTRY_CODE,
            UNKNOWN_VALUE,
            UNKNOWN_VALUE,
            UNKNOWN_VALUE,
            null, // Latitude can be null for unknown
            null, // Longitude can be null for unknown
            UNKNOWN_VALUE,
            UNKNOWN_VALUE,
            UNKNOWN_COUNTRY_CODE,
            UNKNOWN_VALUE
    );

    /**
     * Returns a singleton instance representing an unknown or unresolved location.
     * This avoids the need for null checks throughout the application.
     *
     * @return the UNKNOWN GeoLocation instance.
     */
    public static GeoLocation unknown() {
        return UNKNOWN;
    }

    /**
     * Checks if this GeoLocation instance represents an unknown location.
     *
     * @return true if it's the unknown sentinel object, false otherwise.
     */
    public boolean isUnknown() {
        // Check for reference equality with the singleton UNKNOWN instance.
        return this == UNKNOWN;
    }

    // The record automatically generates:
    // - A canonical constructor with all fields.
    // - Getters for all fields (e.g., `countryCode()`, `city()`).
    // - `equals(Object o)`, `hashCode()`, and `toString()` methods based on all fields.

    // Custom toString for better logging of the UNKNOWN state
    @Override
    public String toString() {
        if (isUnknown()) {
            return "GeoLocation{state=UNKNOWN}";
        }
        return "GeoLocation{" +
                "countryCode='" + countryCode + '\'' +
                ", city='" + city + '\'' +
                ", asn='" + asn + '\'' +
                '}'; // Simplified for brevity in logs
    }

    // Manual builder for convenience, especially if some fields are optional or derived
    public static GeoLocationBuilder builder() {
        return new GeoLocationBuilder();
    }

    public static class GeoLocationBuilder {
        private String countryCode;
        private String countryName;
        private String city;
        private String postalCode;
        private Double latitude;
        private Double longitude;
        private String timeZone;
        private String subdivisionName;
        private String subdivisionCode;
        private String asn;

        public GeoLocationBuilder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public GeoLocationBuilder countryName(String countryName) {
            this.countryName = countryName;
            return this;
        }

        public GeoLocationBuilder city(String city) {
            this.city = city;
            return this;
        }

        public GeoLocationBuilder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public GeoLocationBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public GeoLocationBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public GeoLocationBuilder timeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public GeoLocationBuilder subdivisionName(String subdivisionName) {
            this.subdivisionName = subdivisionName;
            return this;
        }

        public GeoLocationBuilder subdivisionCode(String subdivisionCode) {
            this.subdivisionCode = subdivisionCode;
            return this;
        }

        public GeoLocationBuilder asn(String asn) {
            this.asn = asn;
            return this;
        }

        public GeoLocation build() {
            // Ensure non-null for string fields, defaulting to UNKNOWN_VALUE if not set
            return new GeoLocation(
                    Objects.requireNonNullElse(countryCode, UNKNOWN_COUNTRY_CODE),
                    Objects.requireNonNullElse(countryName, UNKNOWN_VALUE),
                    Objects.requireNonNullElse(city, UNKNOWN_VALUE),
                    Objects.requireNonNullElse(postalCode, UNKNOWN_VALUE),
                    latitude, // Can be null
                    longitude, // Can be null
                    Objects.requireNonNullElse(timeZone, UNKNOWN_VALUE),
                    Objects.requireNonNullElse(subdivisionName, UNKNOWN_VALUE),
                    Objects.requireNonNullElse(subdivisionCode, UNKNOWN_COUNTRY_CODE),
                    Objects.requireNonNullElse(asn, UNKNOWN_VALUE)
            );
        }
    }
}

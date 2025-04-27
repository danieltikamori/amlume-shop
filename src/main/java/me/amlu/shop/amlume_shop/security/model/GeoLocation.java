/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

public class GeoLocation {
    private static final String UNKNOWN = "Unknown";
    private static final String DEFAULT_LOCATION = "Null Island";
    private static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_COUNTRY_CODE = "XX";


    private final String countryCode;
    private final String countryName;
    private final String city;
    private final String postalCode;
    private final Double latitude;
    private final Double longitude;
    private final String timeZone;
    private final String subdivisionName;
    private final String subdivisionCode;
    private final String asn;

    GeoLocation(String countryCode, String countryName, String city, String postalCode, Double latitude, Double longitude, String timeZone, String subdivisionName, String subdivisionCode, String asn) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.city = city;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = timeZone;
        this.subdivisionName = subdivisionName;
        this.subdivisionCode = subdivisionCode;
        this.asn = asn;
    }

    public static GeoLocation unknown() {
        return GeoLocation.builder()
                .countryCode(DEFAULT_COUNTRY_CODE)
                .countryName(DEFAULT_LOCATION)
                .city(DEFAULT_LOCATION)
                .postalCode(UNKNOWN)
                .latitude(0.0)
                .longitude(0.0)
                .timeZone(DEFAULT_TIMEZONE)
                .subdivisionName(UNKNOWN)
                .subdivisionCode(DEFAULT_COUNTRY_CODE)
                .asn(null)
                .build();
    }

    public static GeoLocationBuilder builder() {
        return new GeoLocationBuilder();
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public String getCountryName() {
        return this.countryName;
    }

    public String getCity() {
        return this.city;
    }

    public String getPostalCode() {
        return this.postalCode;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public String getTimeZone() {
        return this.timeZone;
    }

    public String getSubdivisionName() {
        return this.subdivisionName;
    }

    public String getSubdivisionCode() {
        return this.subdivisionCode;
    }

    public String getAsn() {
        return this.asn;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeoLocation)) return false;
        final GeoLocation other = (GeoLocation) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$countryCode = this.getCountryCode();
        final Object other$countryCode = other.getCountryCode();
        if (this$countryCode == null ? other$countryCode != null : !this$countryCode.equals(other$countryCode))
            return false;
        final Object this$countryName = this.getCountryName();
        final Object other$countryName = other.getCountryName();
        if (this$countryName == null ? other$countryName != null : !this$countryName.equals(other$countryName))
            return false;
        final Object this$city = this.getCity();
        final Object other$city = other.getCity();
        if (this$city == null ? other$city != null : !this$city.equals(other$city)) return false;
        final Object this$postalCode = this.getPostalCode();
        final Object other$postalCode = other.getPostalCode();
        if (this$postalCode == null ? other$postalCode != null : !this$postalCode.equals(other$postalCode))
            return false;
        final Object this$latitude = this.getLatitude();
        final Object other$latitude = other.getLatitude();
        if (this$latitude == null ? other$latitude != null : !this$latitude.equals(other$latitude)) return false;
        final Object this$longitude = this.getLongitude();
        final Object other$longitude = other.getLongitude();
        if (this$longitude == null ? other$longitude != null : !this$longitude.equals(other$longitude)) return false;
        final Object this$timeZone = this.getTimeZone();
        final Object other$timeZone = other.getTimeZone();
        if (this$timeZone == null ? other$timeZone != null : !this$timeZone.equals(other$timeZone)) return false;
        final Object this$subdivisionName = this.getSubdivisionName();
        final Object other$subdivisionName = other.getSubdivisionName();
        if (this$subdivisionName == null ? other$subdivisionName != null : !this$subdivisionName.equals(other$subdivisionName))
            return false;
        final Object this$subdivisionCode = this.getSubdivisionCode();
        final Object other$subdivisionCode = other.getSubdivisionCode();
        if (this$subdivisionCode == null ? other$subdivisionCode != null : !this$subdivisionCode.equals(other$subdivisionCode))
            return false;
        final Object this$asn = this.getAsn();
        final Object other$asn = other.getAsn();
        if (this$asn == null ? other$asn != null : !this$asn.equals(other$asn)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GeoLocation;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $countryCode = this.getCountryCode();
        result = result * PRIME + ($countryCode == null ? 43 : $countryCode.hashCode());
        final Object $countryName = this.getCountryName();
        result = result * PRIME + ($countryName == null ? 43 : $countryName.hashCode());
        final Object $city = this.getCity();
        result = result * PRIME + ($city == null ? 43 : $city.hashCode());
        final Object $postalCode = this.getPostalCode();
        result = result * PRIME + ($postalCode == null ? 43 : $postalCode.hashCode());
        final Object $latitude = this.getLatitude();
        result = result * PRIME + ($latitude == null ? 43 : $latitude.hashCode());
        final Object $longitude = this.getLongitude();
        result = result * PRIME + ($longitude == null ? 43 : $longitude.hashCode());
        final Object $timeZone = this.getTimeZone();
        result = result * PRIME + ($timeZone == null ? 43 : $timeZone.hashCode());
        final Object $subdivisionName = this.getSubdivisionName();
        result = result * PRIME + ($subdivisionName == null ? 43 : $subdivisionName.hashCode());
        final Object $subdivisionCode = this.getSubdivisionCode();
        result = result * PRIME + ($subdivisionCode == null ? 43 : $subdivisionCode.hashCode());
        final Object $asn = this.getAsn();
        result = result * PRIME + ($asn == null ? 43 : $asn.hashCode());
        return result;
    }

    public String toString() {
        return "GeoLocation(countryCode=" + this.getCountryCode() + ", countryName=" + this.getCountryName() + ", city=" + this.getCity() + ", postalCode=" + this.getPostalCode() + ", latitude=" + this.getLatitude() + ", longitude=" + this.getLongitude() + ", timeZone=" + this.getTimeZone() + ", subdivisionName=" + this.getSubdivisionName() + ", subdivisionCode=" + this.getSubdivisionCode() + ", asn=" + this.getAsn() + ")";
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

        GeoLocationBuilder() {
        }

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
            return new GeoLocation(this.countryCode, this.countryName, this.city, this.postalCode, this.latitude, this.longitude, this.timeZone, this.subdivisionName, this.subdivisionCode, this.asn);
        }

        public String toString() {
            return "GeoLocation.GeoLocationBuilder(countryCode=" + this.countryCode + ", countryName=" + this.countryName + ", city=" + this.city + ", postalCode=" + this.postalCode + ", latitude=" + this.latitude + ", longitude=" + this.longitude + ", timeZone=" + this.timeZone + ", subdivisionName=" + this.subdivisionName + ", subdivisionCode=" + this.subdivisionCode + ", asn=" + this.asn + ")";
        }
    }
}

//This implementation provides:
//
//Comprehensive list of known VPN ASNs
//
//Null-safe VPN detection
//
//Metrics for VPN detection
//
//Debug logging for VPN detection
//
//Easy way to update the VPN ASN list
//
//Support for multiple VPN providers worldwide
//
//
//You might want to consider:
//
//Loading the ASN list from a configuration file
//
//Implementing regular updates of the ASN list
//
//Adding more sophisticated VPN detection logic
//
//Implementing ASN reputation scoring
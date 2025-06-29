/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties related to geographic security checks, prefixed with "security.geo".
 * This includes settings for impossible travel detection, VPN/proxy detection based on ASN reputation,
 * high-risk countries, and known IP ranges for VPNs and datacenters.
 */
@Component
@ConfigurationProperties(prefix = "security.geo")
@Validated
public class GeoSecurityProperties {

    /**
     * The maximum distance (in kilometers) considered plausible for travel between two consecutive login locations
     * within the defined time window. Used for impossible travel detection.
     */
    private double suspiciousDistanceKm = 200.0; // Default value

    /**
     * The time window (in hours) used for comparing consecutive login locations for impossible travel detection.
     */
    private int timeWindowHours = 24; // Default value

    /**
     * A set of Autonomous System Numbers (ASNs) known to be commonly used by VPN providers.
     * Connections originating from these ASNs might be flagged as higher risk.
     */
    @NotEmpty(message = "Known VPN ASNs list cannot be empty in configuration (security.geo.known-vpn-asns)")
    private Set<String> knownVpnAsns = new HashSet<>();

    /**
     * A set of ISO 3166-1 alpha-2 country codes considered high-risk.
     * Connections originating from these countries might be flagged or blocked.
     */
    private Set<String> highRiskCountries = new HashSet<>();

    /**
     * A set of IP address ranges (in CIDR notation, e.g., "192.168.1.0/24") known to be used by VPN providers.
     * Populated from the environment variable specified in the configuration (e.g., KNOWN_VPN_IP_RANGES).
     */
    private Set<String> knownVpnIpRanges = new HashSet<>();

    /**
     * A set of IP address ranges (in CIDR notation) known to belong to datacenters.
     * Connections from datacenters might indicate proxy or VPN usage.
     * Populated from the environment variable specified in the configuration (e.g., KNOWN_DATACENTER_RANGES).
     */
    private Set<String> knownDatacenterRanges = new HashSet<>();

    /**
     * The reputation score threshold (between 0.0 and 1.0) below which an ASN is considered suspicious
     * enough to potentially flag a connection as VPN/Proxy, even if other factors don't meet the threshold.
     * A lower score indicates a worse reputation.
     */
    @DecimalMin(value = "0.0", message = "VPN reputation threshold must be greater than or equal to 0.0")
    @DecimalMax(value = "1.0", message = "VPN reputation threshold must be less than or equal to 1.0")
    private double vpnReputationThreshold = 0.3; // Default value (matches GeoProperties default for consistency)

    // --- Getters and Setters ---

    public double getSuspiciousDistanceKm() {
        return suspiciousDistanceKm;
    }

    public void setSuspiciousDistanceKm(double suspiciousDistanceKm) {
        this.suspiciousDistanceKm = suspiciousDistanceKm;
    }

    public int getTimeWindowHours() {
        return timeWindowHours;
    }

    public void setTimeWindowHours(int timeWindowHours) {
        this.timeWindowHours = timeWindowHours;
    }

    public Set<String> getKnownVpnAsns() {
        return knownVpnAsns;
    }

    public void setKnownVpnAsns(Set<String> knownVpnAsns) {
        this.knownVpnAsns = knownVpnAsns;
    }

    public Set<String> getHighRiskCountries() {
        return highRiskCountries;
    }

    public void setHighRiskCountries(Set<String> highRiskCountries) {
        this.highRiskCountries = highRiskCountries;
    }

    public Set<String> getKnownVpnIpRanges() {
        return knownVpnIpRanges;
    }

    public void setKnownVpnIpRanges(Set<String> knownVpnIpRanges) {
        // Spring handles splitting the comma-separated env var value automatically
        this.knownVpnIpRanges = knownVpnIpRanges;
    }

    public Set<String> getKnownDatacenterRanges() {
        return knownDatacenterRanges;
    }

    public void setKnownDatacenterRanges(Set<String> knownDatacenterRanges) {
        // Spring handles splitting the comma-separated env var value automatically
        this.knownDatacenterRanges = knownDatacenterRanges;
    }

    public double getVpnReputationThreshold() {
        return vpnReputationThreshold;
    }

    public void setVpnReputationThreshold(double vpnReputationThreshold) {
        // Validation is handled by @DecimalMin/@DecimalMax annotations via @Validated
        // Manual validation kept for robustness, though redundant with annotations.
        if (vpnReputationThreshold < 0.0 || vpnReputationThreshold > 1.0) {
            throw new IllegalArgumentException("VPN reputation threshold must be between 0.0 and 1.0");
        }
        this.vpnReputationThreshold = vpnReputationThreshold;
    }

    @Override
    public String toString() {
        return "GeoSecurityProperties{" +
                "suspiciousDistanceKm=" + suspiciousDistanceKm +
                ", timeWindowHours=" + timeWindowHours +
                ", knownVpnAsns=" + knownVpnAsns +
                ", highRiskCountries=" + highRiskCountries +
                ", knownVpnIpRanges=" + knownVpnIpRanges +
                ", knownDatacenterRanges=" + knownDatacenterRanges +
                ", vpnReputationThreshold=" + vpnReputationThreshold + // Added
                '}';
    }
}

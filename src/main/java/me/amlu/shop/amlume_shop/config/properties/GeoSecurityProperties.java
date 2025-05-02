/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "security.geo")
@Validated
public class GeoSecurityProperties {

    private double suspiciousDistanceKm = 200.0;
    private int timeWindowHours = 24;

    // Spring Boot will automatically bind the YAML list to this Set
    @NotEmpty(message = "Known VPN ASNs list cannot be empty in configuration")
    private Set<String> knownVpnAsns = new HashSet<>();

    private Set<String> highRiskCountries = new HashSet<>();

    // These will be populated correctly from the environment variable placeholders
    private Set<String> knownVpnIpRanges = new HashSet<>();
    private Set<String> knownDatacenterRanges = new HashSet<>();

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
        // Spring handles splitting the comma-separated env var value
        this.knownVpnIpRanges = knownVpnIpRanges;
    }

    public Set<String> getKnownDatacenterRanges() {
        return knownDatacenterRanges;
    }

    public void setKnownDatacenterRanges(Set<String> knownDatacenterRanges) {
        // Spring handles splitting the comma-separated env var value
        this.knownDatacenterRanges = knownDatacenterRanges;
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
                '}';
    }
}
    
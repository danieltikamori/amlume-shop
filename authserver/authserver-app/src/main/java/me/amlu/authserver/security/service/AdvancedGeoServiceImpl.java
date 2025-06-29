/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.authserver.notification.service.AlertService;
import me.amlu.authserver.security.config.properties.GeoSecurityProperties;
import me.amlu.authserver.security.enums.AlertSeverityEnum;
import me.amlu.authserver.security.enums.RiskLevel;
import me.amlu.authserver.security.model.GeoLocation;
import me.amlu.authserver.security.model.GeoLocationHistory;
import me.amlu.authserver.security.model.SecurityAlert;
import org.slf4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static me.amlu.authserver.common.CacheKeys.GEO_HISTORY_CACHE;

/**
 * Service for advanced geolocation verification, including impossible travel detection,
 * VPN/proxy checks, and country risk assessment. It utilizes MaxMind and GeoIP2 services,
 * caching for location history, and integrates with alerting and metrics.
 */
@Service
public class AdvancedGeoServiceImpl implements AdvancedGeoService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AdvancedGeoServiceImpl.class);

    // Speed threshold based on typical commercial flight speeds + buffer
    // Consider making this configurable via GeoSecurityProperties
    private static final double IMPOSSIBLE_SPEED_KMH = 1100.0;

    private final MaxMindGeoService maxMindGeoService;
    private final GeoIp2Service geoIp2Service;
    private final AlertService alertService;
    // CHANGED: Use Micrometer MeterRegistry
    private final MeterRegistry meterRegistry;
    private final EnhancedVpnDetectorService vpnDetector;
    private final AsnReputationService reputationService;
    private final GeoSecurityProperties geoSecurityProperties; // CHANGED: Inject GeoSecurityProperties

    // --- Injected Config ---
    // Removed @Value injection for suspiciousDistanceKm, now accessed via geoSecurityProperties if needed
    // private final double suspiciousDistanceKm; // Instance field

    // Removed @Qualifier injection for knownVpnAsns, now accessed via geoSecurityProperties
    // private final Set<String> knownVpnAsns;

    // --- Spring Cache ---
    private final Cache locationHistoryCacheInstance;

    // --- Constructor Updated ---
    public AdvancedGeoServiceImpl(MaxMindGeoService maxMindGeoService,
                                  GeoIp2Service geoIp2Service,
                                  AlertService alertService,
                                  // CHANGED: Inject Micrometer MeterRegistry
                                  MeterRegistry meterRegistry,
                                  EnhancedVpnDetectorService vpnDetector,
                                  AsnReputationService reputationService,
                                  GeoSecurityProperties geoSecurityProperties, // CHANGED: Inject GeoSecurityProperties
                                  CacheManager cacheManager
                                  // Removed: @Value("${security.geo.suspicious-distance-km}") double suspiciousDistanceKm,
                                  // Removed: @Qualifier("knownVpnAsns") Set<String> knownVpnAsns
    ) {
        this.maxMindGeoService = maxMindGeoService;
        this.geoIp2Service = geoIp2Service;
        this.alertService = alertService;
        // CHANGED: Assign Micrometer MeterRegistry
        this.meterRegistry = meterRegistry;
        this.vpnDetector = vpnDetector;
        this.reputationService = reputationService;
        this.geoSecurityProperties = geoSecurityProperties; // Use injected GeoSecurityProperties
        // Removed: this.suspiciousDistanceKm = suspiciousDistanceKm;
        // Removed: this.knownVpnAsns = knownVpnAsns;

        // Initialize Spring Cache instance
        this.locationHistoryCacheInstance = cacheManager.getCache(GEO_HISTORY_CACHE);
        Objects.requireNonNull(this.locationHistoryCacheInstance, "Cache '" + GEO_HISTORY_CACHE + "' not configured.");

        log.info("AdvancedGeoServiceImpl initialized.");
        // Log injected config for verification during startup (optional)
        log.debug("Suspicious Distance Km (from GeoSecurityProperties, currently unused): {}", this.geoSecurityProperties.getSuspiciousDistanceKm());
        log.debug("Known VPN ASNs count (from GeoSecurityProperties): {}", this.geoSecurityProperties.getKnownVpnAsns() != null ? this.geoSecurityProperties.getKnownVpnAsns().size() : 0);
        log.debug("High Risk Countries count (from GeoSecurityProperties): {}", this.geoSecurityProperties.getHighRiskCountries() != null ? this.geoSecurityProperties.getHighRiskCountries().size() : 0);
        log.debug("VPN Reputation Threshold (from GeoSecurityProperties): {}", this.geoSecurityProperties.getVpnReputationThreshold());
    }

    /**
     * Verifies the geographic location associated with an IP address for a given user.
     * Performs checks for impossible travel, VPN/proxy usage, and high-risk countries.
     *
     * @param ip     The IP address to verify.
     * @param userId The ID of the user associated with the IP.
     * @return A GeoVerificationResult containing the risk level and any alerts.
     */
    @Override
    public GeoVerificationResult verifyLocation(String ip, String userId) {
        // Input validation using Assert for conciseness
        Assert.hasText(ip, "IP address must not be blank");
        Assert.hasText(userId, "User ID must not be blank");

        try {
            // Use Micrometer meterRegistry to get the Timer and call recordCallable
            return meterRegistry.timer("geo.verification").recordCallable(() -> {
                GeoLocation currentLocation = maxMindGeoService.getGeoLocation(ip); // Can return GeoLocation.unknown()
                GeoLocationHistory history = getOrCreateHistory(userId);

                // Enrich currentLocation with ASN if missing and possible
                currentLocation = enrichWithAsnIfNeeded(currentLocation, ip);

                // Analyze the location (this is the core logic being timed)
                return analyzeLocation(currentLocation, history, userId);
            });
        } catch (IllegalArgumentException e) {
            // Catch validation errors from Assert (likely from inside the callable)
            log.warn("Invalid input for verifyLocation: {}", e.getMessage());
            GeoVerificationResult invalidResult = new GeoVerificationResult();
            invalidResult.setRisk(RiskLevel.HIGH);
            invalidResult.addAlert("Invalid input provided for verification.");
            return invalidResult;
        } catch (Exception e) {
            // Catch any other unexpected errors from the callable (lookup, analysis, etc.)
            log.error("Unexpected error during geo verification for ip='{}', userId='{}'", ip, userId, e);
            GeoVerificationResult errorResult = new GeoVerificationResult();
            errorResult.setRisk(RiskLevel.HIGH); // Fail closed on unexpected error
            errorResult.addAlert("Internal error during location verification.");
            return errorResult;
        }
    }

    /**
     * Enriches a GeoLocation object with ASN information if it's missing and can be looked up.
     *
     * @param location The original GeoLocation object.
     * @param ip       The IP address to look up ASN for.
     * @return The original or an enriched GeoLocation object.
     */
    private GeoLocation enrichWithAsnIfNeeded(GeoLocation location, String ip) {
        if (location != null && location.asn() == null) {
            try {
                String asn = String.valueOf(geoIp2Service.lookupAsn(ip));
                // Assuming GeoLocation is immutable (record/builder pattern)
                // GeoLocation doesn't have toBuilder(), manually copy fields
                GeoLocation.GeoLocationBuilder builder = GeoLocation.builder();

                // Copy existing fields from the original location
                builder.countryCode(location.countryCode());
                builder.countryName(location.countryName());
                builder.city(location.city());
                builder.postalCode(location.postalCode());
                builder.latitude(location.latitude());
                builder.longitude(location.longitude());
                builder.timeZone(location.timeZone());
                builder.subdivisionName(location.subdivisionName());
                builder.subdivisionCode(location.subdivisionCode());
                // Do NOT copy the original ASN, as we are setting the new one below

                // Set the new ASN
                builder.asn(asn);

                // Build the new, enriched GeoLocation object
                return builder.build();
            } catch (GeoIp2Exception e) {
                log.warn("Could not look up ASN for IP: {}", ip, e);
                // Proceed without ASN
            } catch (Exception e) {
                log.error("Unexpected error during ASN lookup enrichment for IP: {}", ip, e);
                // Proceed without ASN
            }
        }
        return location; // Return original if no enrichment needed or possible
    }

    /**
     * Retrieves the location history for a user from the cache, or creates a new one if not found.
     *
     * @param userId The ID of the user.
     * @return The user's GeoLocationHistory.
     */
    @Override
    public GeoLocationHistory getOrCreateHistory(String userId) {
        Assert.hasText(userId, "User ID must not be blank for history retrieval");
        try {
            // Use Spring Cache's get method with a loader lambda
            // This retrieves from cache or calls the lambda if not found, then caches the result.
            GeoLocationHistory history = locationHistoryCacheInstance.get(userId, GeoLocationHistory::new);
            // Ensure the loaded object is not null (though the loader should prevent this)
            return Objects.requireNonNullElseGet(history, GeoLocationHistory::new);
        } catch (Cache.ValueRetrievalException e) {
            // This exception wraps loader exceptions
            log.error("Error retrieving/loading location history for user {}. Returning new history.", userId, e);
            return new GeoLocationHistory(); // Fallback
        } catch (Exception e) {
            // Catch other potential cache interaction errors
            log.error("Unexpected cache error for user {}. Returning new history.", userId, e);
            return new GeoLocationHistory(); // Fallback
        }
    }

    /**
     * Analyzes the current location against the user's history and configured rules.
     *
     * @param currentLocation The current GeoLocation.
     * @param history         The user's GeoLocationHistory.
     * @param userId          The user ID.
     * @return A GeoVerificationResult with risk assessment and alerts.
     */
    @Override
    @Timed(value = "authserver.geo.analyzeLocation", longTask = true, extraTags = {"userId"}, description = "Time taken to analyze the geographic location associated with an IP address for a given user.")
    public GeoVerificationResult analyzeLocation(GeoLocation currentLocation,
                                                 GeoLocationHistory history,
                                                 String userId) {
        Assert.notNull(history, "GeoLocationHistory cannot be null");
        Assert.hasText(userId, "User ID must not be blank");

        GeoVerificationResult result = new GeoVerificationResult();

        // Handle case where MaxMind lookup failed or returned an "unknown" object
        if (currentLocation == null || GeoLocation.UNKNOWN_COUNTRY_CODE.equals(currentLocation.countryCode())) {
            return handleUnknownLocation(result);
        }

        // Perform checks and update result
        checkImpossibleTravel(result, currentLocation, history, userId);
        checkVpnRisk(result, currentLocation);
        checkCountryRisk(result, currentLocation);

        // --- Update History and Cache ---
        // Always update history, as even risky logins are part of the history.
        history.addLocation(currentLocation);
        // Explicitly put the modified history object back into the Spring Cache
        try {
            locationHistoryCacheInstance.put(userId, history);
        } catch (Exception e) {
            log.error("Failed to update location history cache for user {}", userId, e);
            // Decide how to handle cache write failure - potentially log and continue,
            // or maybe invalidate the entry if the write failed partially?
            // For now, just log and continue.
        }

        return result;
    }

    /**
     * Handles cases where the location could not be determined.
     * Sets risk to MEDIUM and adds an alert.
     *
     * @param result The GeoVerificationResult to update.
     * @return The updated GeoVerificationResult.
     */
    @Override
    public GeoVerificationResult handleUnknownLocation(GeoVerificationResult result) {
        result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy for unknown locations
        result.addAlert("Unable to determine location from IP address.");
        // CHANGED: Use Micrometer meterRegistry
        meterRegistry.counter("geo.unknown.location").increment();
        return result;
    }

    /**
     * Checks for impossible travel based on distance and time difference between
     * the current location and the last known location.
     *
     * @param result          The GeoVerificationResult to update.
     * @param currentLocation The current GeoLocation.
     * @param history         The user's GeoLocationHistory.
     * @param userId          The user ID.
     * @return The updated GeoVerificationResult.
     */
    @Override
    public GeoVerificationResult checkImpossibleTravel(GeoVerificationResult result,
                                                       GeoLocation currentLocation,
                                                       GeoLocationHistory history,
                                                       String userId) {
        GeoLocation lastLocation = history.getLastLocation();
        // Ensure both locations and necessary coordinates are available
        if (lastLocation != null && lastLocation.latitude() != null && lastLocation.longitude() != null &&
                currentLocation.latitude() != null && currentLocation.longitude() != null) {

            double distance = calculateDistance(
                    lastLocation.latitude(), lastLocation.longitude(),
                    currentLocation.latitude(), currentLocation.longitude()
            );

            Instant lastTimestamp = history.getLastTimestamp();
            if (lastTimestamp != null) {
                // Use timeWindowHours from GeoSecurityProperties
                Duration timeDiff = Duration.between(lastTimestamp, Instant.now());
                Duration timeWindow = Duration.ofHours(geoSecurityProperties.getTimeWindowHours());

                // Only calculate speed if within the configured time window
                if (!timeDiff.isNegative() && !timeDiff.isZero() && timeDiff.compareTo(timeWindow) <= 0 && timeDiff.toSeconds() > 1) {
                    double speedKmH = calculateSpeed(distance, timeDiff);

                    if (isImpossibleTravel(speedKmH)) {
                        result.setRisk(RiskLevel.HIGH); // Impossible travel is usually high risk
                        String alertMessage = String.format("Impossible travel detected (%.0f km/h)", speedKmH);
                        result.addAlert(alertMessage);
                        // CHANGED: Use Micrometer meterRegistry
                        meterRegistry.counter("geo.impossible.travel").increment();
                        sendImpossibleTravelAlert(userId, distance, speedKmH, timeDiff, lastLocation, currentLocation);
                    }
                } else if (timeDiff.compareTo(timeWindow) > 0) {
                    log.debug("Time difference ({}) exceeds window ({}) for impossible travel check for user {}", timeDiff, timeWindow, userId);
                } else {
                    log.debug("Time difference too small or negative for speed calculation for user {}", userId);
                    // Optional: Add a check based purely on distance if timeDiff is too small?
                    // Use suspiciousDistanceKm from GeoSecurityProperties
                    // if (distance > geoSecurityProperties.getSuspiciousDistanceKm()) { ... }
                }
            }
        }
        return result;
    }

    /**
     * Sends a security alert for detected impossible travel.
     */
    private void sendImpossibleTravelAlert(String userId, double distance, double speedKmH, Duration timeDiff, GeoLocation from, GeoLocation to) {
        alertService.sendAlert(new SecurityAlert(
                userId,
                "Impossible travel detected",
                Map.of(
                        "distanceKm", String.format("%.2f", distance),
                        "speedKmH", String.format("%.2f", speedKmH),
                        "timeDiffSeconds", String.valueOf(timeDiff.toSeconds()),
                        "fromCity", from.city() != null ? from.city() : "Unknown",
                        "fromCountry", from.countryCode() != null ? from.countryCode() : "XX",
                        "toCity", to.city() != null ? to.city() : "Unknown",
                        "toCountry", to.countryCode() != null ? to.countryCode() : "XX"
                ),
                AlertSeverityEnum.HIGH,
                Instant.now(),
                "production" // Consider making environment configurable
        ));
    }

    /**
     * Calculates the distance between two geographic points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1.
     * @param lon1 Longitude of point 1.
     * @param lat2 Latitude of point 2.
     * @param lon2 Longitude of point 2.
     * @return The distance in kilometers, or 0.0 if coordinates are invalid.
     */
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Ensure coordinates are valid before calculation
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null ||
                lat1 < -90 || lat1 > 90 || lon1 < -180 || lon1 > 180 ||
                lat2 < -90 || lat2 > 90 || lon2 < -180 || lon2 > 180) {
            log.warn("Invalid coordinates for distance calculation: ({}, {}), ({}, {})", lat1, lon1, lat2, lon2);
            // Returning 0 prevents impossible travel detection here. Consider if throwing an exception is better.
            return 0.0;
        }

        // Haversine formula
        final int EARTH_RADIUS_KM = 6371;
        double latDistanceRad = Math.toRadians(lat2 - lat1);
        double lonDistanceRad = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistanceRad / 2) * Math.sin(latDistanceRad / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistanceRad / 2) * Math.sin(lonDistanceRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates the speed in km/h given a distance and time duration.
     *
     * @param distanceKm The distance in kilometers.
     * @param time       The time duration.
     * @return The speed in km/h, or Double.POSITIVE_INFINITY if time is zero or negative.
     */
    private double calculateSpeed(double distanceKm, Duration time) {
        if (time == null || time.isZero() || time.isNegative()) {
            return Double.POSITIVE_INFINITY; // Treat zero/negative time as infinite speed
        }
        // Use higher precision for hours calculation
        double hours = (double) time.toMillis() / (3600.0 * 1000.0);
        if (hours == 0) {
            // Avoid division by zero if millis is too small, effectively infinite speed
            return Double.POSITIVE_INFINITY;
        }
        return distanceKm / hours;
    }

    /**
     * Determines if a calculated speed constitutes impossible travel.
     *
     * @param speedKmH The speed in km/h.
     * @return true if the speed exceeds the threshold, false otherwise.
     */
    private boolean isImpossibleTravel(double speedKmH) {
        // Consider making IMPOSSIBLE_SPEED_KMH configurable via GeoSecurityProperties
        return speedKmH > IMPOSSIBLE_SPEED_KMH;
    }

    /**
     * Checks if a country code belongs to the configured list of high-risk countries.
     *
     * @param countryCode The ISO country code.
     * @return true if the country is considered high-risk, false otherwise.
     */
    @Override
    public boolean isHighRiskCountry(String countryCode) {
        if (countryCode == null || geoSecurityProperties.getHighRiskCountries() == null) {
            return false;
        }
        // Use GeoSecurityProperties to access the configured list
        boolean isHighRisk = geoSecurityProperties.getHighRiskCountries().contains(countryCode);
        if (isHighRisk) {
            // CHANGED: Use Micrometer meterRegistry
            meterRegistry.counter("geo.high.risk.country").increment();
            log.debug("High-risk country detected: {}", countryCode);
        }
        return isHighRisk;
    }

    /**
     * Checks if a GeoLocation likely originates from a known VPN/proxy ASN.
     *
     * @param location The GeoLocation object, potentially containing ASN.
     * @return true if the ASN matches a known VPN ASN, false otherwise.
     */
    @Override
    public boolean isLikelyVpn(GeoLocation location) {
        if (location == null || location.asn() == null || geoSecurityProperties.getKnownVpnAsns() == null) {
            return false;
        }
        // Check against the injected set of known VPN ASNs from GeoSecurityProperties
        boolean isKnownVpn = geoSecurityProperties.getKnownVpnAsns().contains(location.asn());
        if (isKnownVpn) {
            // CHANGED: Use Micrometer meterRegistry
            meterRegistry.counter("geo.vpn.detected.asn").increment();
            log.debug("Potential VPN detected based on known ASN: {}", location.asn());
        }
        return isKnownVpn;
    }

    /**
     * Checks for VPN/proxy risk based on ASN and potentially external services.
     * Updates the GeoVerificationResult accordingly.
     */
    private void checkVpnRisk(GeoVerificationResult result, GeoLocation location) {
        // Simple check based on ASN list
        if (isLikelyVpn(location)) {
            result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy
            result.addAlert("Potential VPN/Proxy detected based on ASN: " + location.asn());
        }

        // --- Optional Advanced Check (Currently not active in default flow) ---
        // If enabled, this would use external services via isVpnConnection method.
        /*
        try {
            // Assuming GeoLocation has getIpAddress() method or IP is available
            String ipAddress = location.getIpAddress(); // Hypothetical method
            if (ipAddress != null && isVpnConnection(ipAddress)) {
                // Elevate risk if not already HIGH
                if (result.getRisk() != RiskLevel.HIGH) {
                     result.setRisk(RiskLevel.MEDIUM); // Or HIGH
                }
                result.addAlert("Potential VPN/Proxy detected based on reputation/external check.");
                // CHANGED: Use Micrometer meterRegistry
                meterRegistry.counter("geo.vpn.detected.external").increment();
            }
        } catch (GeoIp2Exception e) {
            log.warn("Error during advanced VPN check for IP: {}", location.getIpAddress(), e);
        } catch (Exception e) {
            log.error("Unexpected error during advanced VPN check for IP: {}", location.getIpAddress(), e);
        }
        */
        // --- End Optional Advanced Check ---
    }

    /**
     * Checks if the connection originates from a high-risk country.
     * Updates the GeoVerificationResult accordingly.
     */
    private void checkCountryRisk(GeoVerificationResult result, GeoLocation location) {
        if (location.countryCode() != null && isHighRiskCountry(location.countryCode())) {
            // Only set risk if it's not already HIGH (e.g., from impossible travel)
            if (result.getRisk() != RiskLevel.HIGH) {
                result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy
            }
            result.addAlert("Connection from high-risk country: " + location.countryCode());
        }
    }

    /**
     * Performs an advanced check using external services and reputation data to determine
     * if an IP address is likely associated with a VPN or proxy.
     * Note: This method is available but not used in the default `analyzeLocation` flow.
     *
     * @param ip The IP address to check.
     * @return true if the IP is likely a VPN/proxy, false otherwise.
     * @throws GeoIp2Exception If ASN lookup fails.
     */
    public boolean isVpnConnection(String ip) throws GeoIp2Exception {
        Assert.hasText(ip, "IP address must not be blank for VPN connection check");

        String asn = null;
        try {
            asn = String.valueOf(geoIp2Service.lookupAsn(ip)); // Can throw GeoIp2Exception
        } catch (GeoIp2Exception e) {
            log.warn("ASN lookup failed during advanced VPN check for IP: {}. Relying on IP-based detection.", ip, e);
            // Allow proceeding without ASN, relying solely on vpnDetector's IP check
        } catch (Exception e) {
            log.error("Unexpected error during ASN lookup for advanced VPN check, IP: {}", ip, e);
            // Decide how to handle: fail closed (return true) or proceed without ASN?
            // Let's proceed without ASN for now.
        }

        // Use EnhancedVpnDetectorService (handles null ASN if lookup failed)
        boolean isVpnDetected = vpnDetector.isLikelyVpn(ip, asn);

        // Update and consider ASN reputation if ASN is available
        if (asn != null) {
            reputationService.recordActivity(asn, isVpnDetected);
            double reputation = reputationService.getReputationScore(asn);
            log.debug("ASN {} reputation score: {}", asn, reputation);

            // Use the reputation threshold from GeoSecurityProperties
            double reputationThreshold = geoSecurityProperties.getVpnReputationThreshold();
            return isVpnDetected || reputation < reputationThreshold;
        } else {
            // If ASN lookup failed, rely solely on vpnDetector's result based on IP
            return isVpnDetected;
        }
    }
}

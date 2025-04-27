/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import me.amlu.shop.amlume_shop.config.AsnConfigLoader;
import me.amlu.shop.amlume_shop.config.ValkeyCacheConfig;
import me.amlu.shop.amlume_shop.security.enums.AlertSeverityEnum;
import me.amlu.shop.amlume_shop.security.enums.RiskLevel;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import me.amlu.shop.amlume_shop.security.model.GeoLocationHistory;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AdvancedGeoServiceImpl implements AdvancedGeoService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AdvancedGeoServiceImpl.class);

    private final MaxMindGeoService maxMindGeoService;
    private final GeoIp2Service geoIp2Service;
    private final AlertService alertService;
    private final MetricRegistry metrics;
    private final AsnConfigLoader asnConfigLoader;
    private final EnhancedVpnDetectorService vpnDetector;
    private final AsnReputationService reputationService;

    // --- Injected Config ---
    @Value("${security.geo.suspicious-distance-km:500.0}")
    private double suspiciousDistanceKm; // Instance field

    // timeWindowHours is used for cache TTL, configured in ValkeyCacheConfig now

    @Value("${security.geo.known-vpn-asns}")
    private Set<String> knownVpnAsns; // Injected from application.yml

    @Value("${security.geo.high-risk-countries}")
    private Set<String> highRiskCountries; // Injected from application.yml

    // --- Spring Cache ---
    private final Cache locationHistoryCacheInstance;

    public AdvancedGeoServiceImpl(MaxMindGeoService maxMindGeoService,
                                  GeoIp2Service geoIp2Service,
                                  AlertService alertService,
                                  MetricRegistry metrics,
                                  AsnConfigLoader asnConfigLoader,
                                  EnhancedVpnDetectorService vpnDetector,
                                  AsnReputationService reputationService,
                                  CacheManager cacheManager // Inject CacheManager
    ) {
        this.maxMindGeoService = maxMindGeoService;
        this.geoIp2Service = geoIp2Service;
        this.alertService = alertService;
        this.metrics = metrics;
        this.asnConfigLoader = asnConfigLoader;
        this.vpnDetector = vpnDetector;
        this.reputationService = reputationService;

        // Initialize Spring Cache instance
        this.locationHistoryCacheInstance = cacheManager.getCache(ValkeyCacheConfig.GEO_HISTORY_CACHE);
        Objects.requireNonNull(this.locationHistoryCacheInstance, "Cache '" + ValkeyCacheConfig.GEO_HISTORY_CACHE + "' not configured.");

        log.info("AdvancedGeoServiceImpl initialized.");
        // Log injected config for verification during startup (optional)
        log.debug("Known VPN ASNs count: {}", knownVpnAsns != null ? knownVpnAsns.size() : 0);
        log.debug("High Risk Countries count: {}", highRiskCountries != null ? highRiskCountries.size() : 0);
    }

    @Override
    public GeoVerificationResult verifyLocation(String ip, String userId) {
        // Input validation
        if (ip == null || ip.isBlank() || userId == null || userId.isBlank()) {
            log.warn("Invalid input for verifyLocation: ip='{}', userId='{}'", ip, userId);
            GeoVerificationResult invalidResult = new GeoVerificationResult();
            invalidResult.setRisk(RiskLevel.HIGH);
            invalidResult.addAlert("Invalid input provided for verification.");
            return invalidResult;
        }

        try (Timer.Context timer = metrics.timer("geo.verification").time()) {
            GeoLocation currentLocation = maxMindGeoService.getGeoLocation(ip); // Can return GeoLocation.unknown()
            GeoLocationHistory history = getOrCreateHistory(userId);

            // Pass ASN from GeoLocation if available
            if (currentLocation != null && currentLocation.getAsn() == null) {
                try {
                    String asn = String.valueOf(geoIp2Service.lookupAsn(ip));
                    // Create a new GeoLocation object with ASN if needed, or modify if mutable
                    // Assuming GeoLocation is immutable (record/builder pattern)
                    currentLocation = GeoLocation.builder()
                            .countryCode(currentLocation.getCountryCode())
                            .countryName(currentLocation.getCountryName())
                            .city(currentLocation.getCity())
                            .postalCode(currentLocation.getPostalCode())
                            .latitude(currentLocation.getLatitude())
                            .longitude(currentLocation.getLongitude())
                            .timeZone(currentLocation.getTimeZone())
                            .subdivisionName(currentLocation.getSubdivisionName())
                            .subdivisionCode(currentLocation.getSubdivisionCode())
                            .asn(asn) // Add ASN here
                            .build();
                } catch (GeoIp2Exception e) {
                    log.warn("Could not look up ASN for IP: {}", ip, e);
                    // Proceed without ASN
                }
            }

            return analyzeLocation(currentLocation, history, userId);
        } catch (Exception e) {
            // Catch unexpected errors during verification
            log.error("Unexpected error during geo verification for ip='{}', userId='{}'", ip, userId, e);
            GeoVerificationResult errorResult = new GeoVerificationResult();
            errorResult.setRisk(RiskLevel.HIGH); // Fail closed on unexpected error
            errorResult.addAlert("Internal error during location verification.");
            return errorResult;
        }
    }

    @Override
    public GeoLocationHistory getOrCreateHistory(String userId) {
        try {
            // Use Spring Cache's get method with a loader lambda
            // This retrieves from cache or calls the lambda if not found, then caches the result.
            return locationHistoryCacheInstance.get(userId, GeoLocationHistory::new);
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

    @Override
    public GeoVerificationResult analyzeLocation(GeoLocation currentLocation,
                                                 GeoLocationHistory history,
                                                 String userId) {
        GeoVerificationResult result = new GeoVerificationResult();

        // Handle case where MaxMind lookup failed but returned an "unknown" object
        if (currentLocation == null || GeoLocation.DEFAULT_COUNTRY_CODE.equals(currentLocation.getCountryCode())) {
            return handleUnknownLocation(result);
        }

        // Perform checks and update result
        checkImpossibleTravel(result, currentLocation, history, userId);
        checkVpnRisk(result, currentLocation);
        checkCountryRisk(result, currentLocation);

        // --- Update History and Cache ---
        // Decide policy: Always update, or only if not high risk?
        // Let's always update history for now, as even risky logins are part of the history.
        history.addLocation(currentLocation);
        // Explicitly put the modified history object back into the Spring Cache
        try {
            locationHistoryCacheInstance.put(userId, history);
        } catch (Exception e) {
            log.error("Failed to update location history cache for user {}", userId, e);
            // Decide how to handle cache write failure - potentially log and continue
        }

        return result;
    }

    @Override
    public GeoVerificationResult handleUnknownLocation(GeoVerificationResult result) {
        result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy for unknown locations
        result.addAlert("Unable to determine location from IP address.");
        metrics.counter("geo.unknown.location").inc();
        return result;
    }

    @Override
    public GeoVerificationResult checkImpossibleTravel(GeoVerificationResult result,
                                                       GeoLocation currentLocation,
                                                       GeoLocationHistory history,
                                                       String userId) {
        GeoLocation lastLocation = history.getLastLocation();
        if (lastLocation != null && currentLocation.getLatitude() != null && currentLocation.getLongitude() != null) {
            double distance = calculateDistance(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    currentLocation.getLatitude(), currentLocation.getLongitude()
            );

            Instant lastTimestamp = history.getLastTimestamp();
            if (lastTimestamp != null) {
                Duration timeDiff = Duration.between(lastTimestamp, Instant.now());
                // Avoid division by zero or near-zero time differences
                if (!timeDiff.isNegative() && !timeDiff.isZero() && timeDiff.toSeconds() > 1) { // Add a small threshold
                    double speedKmH = calculateSpeed(distance, timeDiff);

                    // Use the configured distance threshold indirectly via speed calculation
                    // Or directly check distance if timeDiff is very small?
                    // Let's stick to speed check for now.
                    if (isImpossibleTravel(speedKmH)) {
                        result.setRisk(RiskLevel.HIGH); // Impossible travel is usually high risk
                        result.addAlert(String.format("Impossible travel detected (%.0f km/h)", speedKmH));
                        metrics.counter("geo.impossible.travel").inc();
                        alertService.sendAlert(new SecurityAlert(
                                userId,
                                "Impossible travel detected",
                                Map.of(
                                        "distanceKm", String.format("%.2f", distance),
                                        "speedKmH", String.format("%.2f", speedKmH),
                                        "timeDiffSeconds", timeDiff.toSeconds(),
                                        "fromCity", lastLocation.getCity() != null ? lastLocation.getCity() : "Unknown",
                                        "fromCountry", lastLocation.getCountryCode() != null ? lastLocation.getCountryCode() : "XX",
                                        "toCity", currentLocation.getCity() != null ? currentLocation.getCity() : "Unknown",
                                        "toCountry", currentLocation.getCountryCode() != null ? currentLocation.getCountryCode() : "XX"
                                ),
                                AlertSeverityEnum.HIGH, // High severity for impossible travel
                                Instant.now(),
                                "production" // Consider making environment configurable
                        ));
                    }
                } else {
                    log.debug("Time difference too small or negative for speed calculation for user {}", userId);
                }
            }
        }
        return result;
    }

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Ensure coordinates are valid before calculation
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null ||
                lat1 < -90 || lat1 > 90 || lon1 < -180 || lon1 > 180 ||
                lat2 < -90 || lat2 > 90 || lon2 < -180 || lon2 > 180) {
            log.warn("Invalid coordinates for distance calculation: ({}, {}), ({}, {})", lat1, lon1, lat2, lon2);
            return 0.0; // Or throw an exception? Returning 0 prevents impossible travel detection here.
        }

        // Haversine formula
        final int R = 6371; // Earth's radius in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateSpeed(double distanceKm, Duration time) {
        if (time == null || time.isZero() || time.isNegative()) {
            return Double.POSITIVE_INFINITY; // Treat zero/negative time as infinite speed
        }
        // Use higher precision for hours calculation
        double hours = (double) time.toMillis() / (3600.0 * 1000.0);
        if (hours == 0) {
            return Double.POSITIVE_INFINITY; // Avoid division by zero if millis is too small
        }
        return distanceKm / hours;
    }

    // Speed threshold based on typical commercial flight speeds + buffer
    private static final double IMPOSSIBLE_SPEED_KMH = 1100.0;
    private boolean isImpossibleTravel(double speedKmH) {
        return speedKmH > IMPOSSIBLE_SPEED_KMH;
    }

    // --- highRiskCountries ---
    @Override
    public boolean isHighRiskCountry(String countryCode) {
        if (countryCode == null || highRiskCountries == null) {
            return false;
        }
        boolean isHighRisk = highRiskCountries.contains(countryCode);
        if (isHighRisk) {
            metrics.counter("geo.high.risk.country").inc();
            log.debug("High-risk country detected: {}", countryCode);
        }
        return isHighRisk;
    }

    // --- knownVpnAsns ---
    @Override
    public boolean isLikelyVpn(GeoLocation location) {
        if (location == null || location.getAsn() == null || knownVpnAsns == null) {
            return false;
        }
        boolean isKnownVpn = knownVpnAsns.contains(location.getAsn());
        if (isKnownVpn) {
            metrics.counter("geo.vpn.detected.asn").inc();
            log.debug("Potential VPN detected based on known ASN: {}", location.getAsn());
        }
        return isKnownVpn;
    }

    // --- Helper methods used by analyzeLocation ---
    private void checkVpnRisk(GeoVerificationResult result, GeoLocation location) {
        // Simple check based on ASN list
        if (isLikelyVpn(location)) {
            result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy
            result.addAlert("Potential VPN/Proxy detected based on ASN: " + location.getAsn());
        }

        // Optional: Add more advanced check using external services if needed
        // try {
        //     if (isVpnConnection(location.getIpAddress())) { // Assuming GeoLocation has IP
        //         result.setRisk(RiskLevel.MEDIUM); // Or HIGH
        //         result.addAlert("Potential VPN/Proxy detected based on reputation/external check.");
        //         metrics.counter("geo.vpn.detected.external").inc();
        //     }
        // } catch (GeoIp2Exception e) {
        //     log.warn("Error during advanced VPN check for IP: {}", location.getIpAddress(), e);
        // } catch (Exception e) {
        //     log.error("Unexpected error during advanced VPN check for IP: {}", location.getIpAddress(), e);
        // }
    }

    private void checkCountryRisk(GeoVerificationResult result, GeoLocation location) {
        if (location.getCountryCode() != null && isHighRiskCountry(location.getCountryCode())) {
            // Only set risk if it's not already HIGH from impossible travel
            if (result.getRisk() != RiskLevel.HIGH) {
                result.setRisk(RiskLevel.MEDIUM); // Or HIGH depending on policy
            }
            result.addAlert("Connection from high-risk country: " + location.getCountryCode());
        }
    }

    // --- Advanced VPN check using external services (remains available but not used in default analyzeLocation) ---
    public boolean isVpnConnection(String ip) throws GeoIp2Exception {
        if (ip == null || ip.isBlank()) return false;

        String asn = String.valueOf(geoIp2Service.lookupAsn(ip)); // Can throw GeoIp2Exception
        boolean isVpn = vpnDetector.isLikelyVpn(ip, asn); // Assuming vpnDetector handles null ASN

        // Update reputation based on detection result
        if (asn != null) {
            reputationService.recordActivity(asn, isVpn);
            // Consider reputation in final decision
            double reputation = reputationService.getReputationScore(asn);
            log.debug("ASN {} reputation score: {}", asn, reputation);
            // Example: Lower reputation significantly increases likelihood of VPN being flagged
            return isVpn || reputation < 0.3; // Threshold can be adjusted
        } else {
            return isVpn; // If ASN lookup failed, rely solely on vpnDetector's result
        }
    }
}
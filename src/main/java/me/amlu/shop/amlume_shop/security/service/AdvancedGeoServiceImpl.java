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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.config.AsnConfigLoader;
import me.amlu.shop.amlume_shop.security.enums.RiskLevel;
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import me.amlu.shop.amlume_shop.security.model.GeoLocationHistory;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AdvancedGeoServiceImpl implements AdvancedGeoService {
    private final MaxMindGeoService maxMindGeoService;
    private final LoadingCache<String, GeoLocationHistory> locationHistoryCache;
    private final AlertService alertService;
    private final MetricRegistry metrics;
    private final AsnConfigLoader asnConfigLoader;
    private final EnhancedVpnDetectorService vpnDetector;
    private final AsnReputationService reputationService;

    private final Set<String> knownVpnAsns;

    @Value("${security.geo.suspicious-distance-km}")
    private static final double SUSPICIOUS_DISTANCE_KM = 500;

    @Value("${security.geo.time-window-hours}")
    private static final int TIME_WINDOW_HOURS = 24;

    public AdvancedGeoServiceImpl(MaxMindGeoService maxMindGeoService,
                                  AlertService alertService,
                                  MetricRegistry metrics, AsnConfigLoader asnConfigLoader, EnhancedVpnDetectorService vpnDetector, AsnReputationService reputationService) {
        this.maxMindGeoService = maxMindGeoService;
        this.alertService = alertService;
        this.metrics = metrics;
        this.asnConfigLoader = asnConfigLoader;
        this.vpnDetector = vpnDetector;
        this.reputationService = reputationService;

        // Initialize known VPN ASNs
        this.knownVpnAsns = initializeKnownVpnAsns();

        this.locationHistoryCache = CacheBuilder.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(TIME_WINDOW_HOURS, TimeUnit.HOURS)
                .recordStats()
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public GeoLocationHistory load(@NotNull String key) {
                        return new GeoLocationHistory();
                    }
                });
    }

    @Override
    public GeoVerificationResult verifyLocation(String ip, String userId) {
        try (Timer.Context timer = metrics.timer("geo.verification").time()) {
            GeoLocation currentLocation = maxMindGeoService.getGeoLocation(ip);
            GeoLocationHistory history = getOrCreateHistory(userId);

            return analyzeLocation(currentLocation, history, userId);
        }
    }

    @Override
    public GeoLocationHistory getOrCreateHistory(String userId) {
        try {
            return locationHistoryCache.get(userId);
        } catch (ExecutionException e) {
            log.error("Error getting location history for user {}", userId, e);
            return new GeoLocationHistory(); // Fallback to new history if cache fails
        }
    }

    @Override
    public GeoVerificationResult analyzeLocation(GeoLocation currentLocation,
                                                 GeoLocationHistory history,
                                                 String userId) {
        GeoVerificationResult result = new GeoVerificationResult();

        if (currentLocation == null) {
            return handleUnknownLocation(result);
        }

        // Check for impossible travel
        if (history.hasRecentLocation()) {
            return checkImpossibleTravel(result, currentLocation, history, userId);
        }

        // Safe to update history after validation
        history.addLocation(currentLocation);

        return result;
    }

    @Override
    public GeoVerificationResult handleUnknownLocation(GeoVerificationResult result) {
        result.setRisk(RiskLevel.HIGH);
        result.addAlert("Unable to determine location");
        return result;
    }

    /**
     * Check if the user has traveled an impossible distance in a given time
     * window. If so, mark the request as high risk and send an alert.
     * <p>
     * The checkImpossibleTravel method takes in a GeoVerificationResult object, a GeoLocation object, a GeoLocationHistory object, and a String object as parameters. It checks if the lastLocation in the history is not null. If it's not null, it calculates the distance between the lastLocation and the currentLocation using the calculateDistance method.
     * <p>
     * Then, it checks if the lastTimestamp in the history is not null. If it's not null, it calculates the time difference between the lastTimestamp and the current time using the Duration.between method. It then calculates the speed in kilometers per hour using the calculateSpeed method.
     * <p>
     * Finally, it checks if the calculated speed is considered impossible travel (greater than 1000 km/h) using the isImpossibleTravel method. If it is, it sets the risk of the result object to RiskLevel.HIGH, adds an alert message to the result, and sends an alert using the alertService.sendAlert method.
     * <p>
     * The method then returns the result object.
     *
     * @param result the result to update
     * @param currentLocation the current location
     * @param history the history of locations
     * @param userId the ID of the user
     * @return the updated result
     */
    @Override
    public GeoVerificationResult checkImpossibleTravel(GeoVerificationResult result,
                                                       GeoLocation currentLocation,
                                                       GeoLocationHistory history,
                                                       String userId) {
        GeoLocation lastLocation = history.getLastLocation();
        if (lastLocation != null) {
            double distance = calculateDistance(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    currentLocation.getLatitude(), currentLocation.getLongitude()
            );

            Instant lastTimestamp = history.getLastTimestamp();
            if (lastTimestamp != null) {
                Duration timeDiff = Duration.between(lastTimestamp, Instant.now());
                double speedKmH = calculateSpeed(distance, timeDiff);

                if (isImpossibleTravel(speedKmH)) {
                    result.setRisk(RiskLevel.HIGH);
                    result.addAlert("Impossible travel detected");
                    alertService.sendAlert(new SecurityAlert(
                            userId,
                            "Impossible travel detected",
                            Map.of(
                                    "distance", distance,
                                    "speed", speedKmH,
                                    "from", lastLocation.getCity() != null ? lastLocation.getCity() : "Unknown",
                                    "to", currentLocation.getCity() != null ? currentLocation.getCity() : "Unknown"
                            )
                    ));
                }
            }
        }
        return result;
    }

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            log.warn("Invalid coordinates for distance calculation");
            return 0.0;
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
        if (time == null || time.isZero()) {
            return Double.POSITIVE_INFINITY;
        }
        double hours = time.toSeconds() / 3600.0;
        return distanceKm / hours;
    }

    private boolean isImpossibleTravel(double speedKmH) {
        // Average commercial flight speed is ~800 km/h
        return speedKmH > 1000;
    }

    @Override
    public boolean isHighRiskCountry(String countryCode) {
        // Implement your high-risk country logic
        Set<String> highRiskCountries = Set.of("XX", "YY", "ZZ");
        return highRiskCountries.contains(countryCode);
    }

//    private boolean isLikelyVpn(GeoLocation location) {
//        // Implement VPN detection logic
//        return location.getAsn() != null &&
//                knownVpnAsns.contains(location.getAsn());
//    }

    public boolean isVpnConnection(String ip) {
        String asn = lookupAsn(ip);
        boolean isVpn = vpnDetector.isLikelyVpn(ip, asn);

        // Update reputation based on detection result
        reputationService.recordActivity(asn, isVpn);

        // Consider reputation in final decision
        double reputation = reputationService.getReputationScore(asn);
        return isVpn || reputation < 0.3; // Threshold can be adjusted
    }
    @Override
    public boolean isLikelyVpn(GeoLocation location) {
        if (location == null || location.getAsn() == null) {
            return false;
        }

        boolean isKnownVpn = knownVpnAsns.contains(location.getAsn());
        if (isKnownVpn) {
            metrics.counter("geo.vpn.detected").inc();
            log.debug("VPN detected for ASN: {}", location.getAsn());
        }

        return isKnownVpn;
    }

    private Set<String> initializeKnownVpnAsns() {
        // Common VPN providers' ASNs
        return Set.of(
                "AS9009",  // M247
                "AS12876", // ONLINE S.A.S.
                "AS16276", // OVH SAS
                "AS14061", // DigitalOcean
                "AS45102", // Alibaba
                "AS7552",  // Viettel
                "AS4766",  // Korea Telecom
                "AS9299",  // Philippine Long Distance Telephone
                "AS4134",  // Chinanet
                "AS3356",  // Level3
                "AS3257",  // GTT Communications
                "AS6939",  // Hurricane Electric
                "AS174",   // Cogent Communications
                "AS2914",  // NTT America
                "AS3491",  // PCCW Global
                "AS1299",  // Telia Carrier
                "AS7018",  // AT&T
                "AS3320",  // Deutsche Telekom
                "AS6461",  // Zayo Bandwidth
                "AS6453",  // TATA Communications
                "AS20473", // Choopa, LLC
                "AS51167", // Contabo GmbH
                "AS24940", // Hetzner Online GmbH
                "AS14618", // Amazon
                "AS16509", // Amazon AWS
                "AS8075",  // Microsoft
                "AS15169", // Google
                "AS396982", // Google Cloud
                "AS13335", // Cloudflare
                "AS45090", // Tencent Cloud
                "AS37963", // Alibaba Cloud
                "AS4837",  // China Unicom
                "AS9808",  // China Mobile
                "AS4538",  // China Education and Research Network
                "AS17621", // China Unicom Shanghai
                "AS4812",  // China Telecom
                "AS9394",  // China TieTong
                "AS9929",  // China Netcom
                "AS58593", // Microsoft Mobile
                "AS132203", // Tencent Building, Kejizhongyi Avenue
                "AS45102", // Alibaba (China) Technology Co., Ltd.
                "AS55967", // Beijing Baidu Netcom Science and Technology Co., Ltd.
                "AS137971", // Perfect Online Technology
                "AS134105", // KATECH
                "AS9269",  // Hong Kong Broadband Network
                "AS4760",  // HKT Limited
                "AS9304",  // HGC Global Communications Limited
                "AS4515",  // ERX-STAR HKT Limited
                "AS7713",  // PT Telekomunikasi Indonesia
                "AS7473",  // Singapore Telecommunications Limited
                "AS4657",  // StarHub Ltd
                "AS9892",  // Mobile One Ltd
                "AS9443",  // Link Broadband
                "AS9583",  // Sify Limited
                "AS55410", // VnCloud
                "AS38001", // NewMedia Express
                "AS45899", // VNPT Corp
                "AS131429", // Megatron
                "AS135905", // VNPT
                "AS45543", // SingNet
                "AS56308", // Telin
                "AS24203", // NAPXLNET
                "AS24378", // ENGTAC
                "AS133752", // LEASEWEB-APAC-HKG-10
                "AS133480", // INTERGRID
                "AS132816", // SIMPLERCLOUD-AS-AP
                "AS132787", // MNSPL
                "AS132203", // TENCENT
                "AS131584", // TAIFO
                "AS45102", // CNNIC-ALIBABA-CN-NET-AP
                "AS38283", // CHINANET
                "AS23724", // CHINANET-IDC
                "AS17621", // CNCGROUP-SH
                "AS4808",  // CHINA169-BJ
                "AS4134"   // CHINANET-BACKBONE
        );
    }


}


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

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class IpMetadata {
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int TTL_VARIANCE_THRESHOLD = 5;
    private static final Duration MAX_CHANGE_THRESHOLD_INTERVAL = Duration.ofMinutes(30);

    private final AtomicInteger suspiciousCount;
    private final Queue<GeoLocation> geoHistory;
    private final Queue<Integer> ttlHistory;
    private Instant firstSeen;
    private Instant lastSeen;
    private String lastGeolocation;
    private final Set<String> previousGeolocations = new HashSet<>();
    private int lastTtl;

    public IpMetadata() {
        this.suspiciousCount = new AtomicInteger(0);
        this.geoHistory = new LinkedList<>();
        this.ttlHistory = new LinkedList<>();
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
    }

    public void incrementSuspiciousCount() {
        suspiciousCount.incrementAndGet();
    }

    public int getSuspiciousCount() {
        return suspiciousCount.get();
    }

    public boolean hasGeolocationChanged(String currentGeo) {
        if (StringUtils.isBlank(currentGeo)) {
            return false;
        }

        if (lastGeolocation == null) {
            lastGeolocation = currentGeo;
            previousGeolocations.add(currentGeo);
            return false;
        }

        boolean changed = !currentGeo.equals(lastGeolocation);
        if (!lastGeolocation.equals(currentGeo)) {
            geoHistory.offer(new GeoLocation(currentGeo, Instant.now()));
            while (geoHistory.size() > MAX_HISTORY_SIZE) {
                geoHistory.poll();
            }

            previousGeolocations.add(currentGeo);

            // Check for rapid changes
            if (isRapidGeoChange()) {
                return true;
            }

            lastGeolocation = currentGeo;
        }

        return changed && previousGeolocations.size() > 2; // Suspicious if more than 2 different locations
//        return false;
    }

    /**
     * Set the last geolocation for this IP and add it to the history.
     * Used when restoring the IP metadata from a persisted state.
     *
     * @param geolocation the geolocation to set
     */
    public void setLastGeolocation(String geolocation) {
        this.lastGeolocation = geolocation;
        this.previousGeolocations.add(geolocation);
    }

    public boolean hasTTLAnomaly(int currentTtl) {
        if (lastTtl == 0) {
            lastTtl = currentTtl;
            return false;
        }

        ttlHistory.offer(currentTtl);
        while (ttlHistory.size() > MAX_HISTORY_SIZE) {
            ttlHistory.poll();
        }

        // Check for TTL anomalies
        if (Math.abs(currentTtl - lastTtl) > TTL_VARIANCE_THRESHOLD) {
            return true;
        }

        lastTtl = currentTtl;
        return false;
    }

    private boolean isRapidGeoChange() {
        if (geoHistory.size() < 2) {
            return false;
        }

        GeoLocation oldest = ((LinkedList<GeoLocation>) geoHistory).getFirst();
        GeoLocation newest = ((LinkedList<GeoLocation>) geoHistory).getLast();

        // Check if there are multiple changes within a short time period
        return !oldest.location.equals(newest.location) &&
                newest.timestamp.minus(MAX_CHANGE_THRESHOLD_INTERVAL).isBefore(oldest.timestamp);
    }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }

    //    @Data
    public record GeoLocation(String location, Instant timestamp) {
    }
}

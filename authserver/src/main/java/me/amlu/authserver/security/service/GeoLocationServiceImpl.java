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

import org.slf4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static me.amlu.authserver.common.CacheKeys.GEO_LOCATION_CACHE;

@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GeoLocationServiceImpl.class);

    private final Cache geoCacheInstance; // Spring Cache instance
    private final MaxMindGeoService maxMindGeoService;

    public GeoLocationServiceImpl(MaxMindGeoService maxMindGeoService, CacheManager cacheManager) {
        this.maxMindGeoService = maxMindGeoService;
        // Initialize Spring Cache instance
        this.geoCacheInstance = cacheManager.getCache(GEO_LOCATION_CACHE);
        Objects.requireNonNull(this.geoCacheInstance, "Cache '" + GEO_LOCATION_CACHE + "' not configured.");
        log.debug("GeoLocationService initialized with cache: {}", GEO_LOCATION_CACHE);
    }

    @Override
    public String getGeolocation(String ip) {
        try {
            // Use Spring Cache get with loader function
            return geoCacheInstance.get(ip, () -> lookupGeolocation(ip));
        } catch (Cache.ValueRetrievalException e) {
            log.error("Error retrieving geolocation for IP: {} from cache or loader. Returning 'unknown'.", ip, e);
            return "unknown"; // Default value on error
        } catch (Exception e) {
            // Catch unexpected errors
            log.error("Unexpected error getting geolocation for IP: {}. Returning 'unknown'.", ip, e);
            return "unknown";
        }
    }

    // This is the loader function for the geoCacheInstance
    private String lookupGeolocation(String ip) {
        log.debug("Cache miss or loading required for geolocation: {}", ip);
        try {
            // Delegate to the actual lookup service
            return maxMindGeoService.getCountryCode(ip);
        } catch (Exception e) {
            // Log error during lookup and return default
            log.error("Error looking up geolocation for IP: {}. Returning 'unknown'.", ip, e);
            return "unknown";
        }
    }
}

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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeoLocationServiceImpl implements GeoLocationService {
    private final LoadingCache<String, String> geoCache;
    private final MaxMindGeoService maxMindGeoService;

    public GeoLocationServiceImpl(MaxMindGeoService maxMindGeoService) {
        this.maxMindGeoService = maxMindGeoService;
        this.geoCache = CacheBuilder.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public String load(@NotNull String ip) {
                    return lookupGeolocation(ip);
                }
            });
    }

    @Override
    public String getGeolocation(String ip) {
        try {
            return geoCache.get(ip);
        } catch (ExecutionException e) {
            log.error("Error getting geolocation for IP: {}", ip, e);
            return "unknown";
        }
    }

//    @Override
//    public GeoLocation getGeolocation(String ip) {
//        try {
//            return geoCache.get(ip);
//        } catch (ExecutionException e) {
//            log.error("Error getting geolocation for IP: {}", ip, e);
//            return "unknown";
//        }
//    }

    private String lookupGeolocation(String ip) {
        try {
            return maxMindGeoService.getCountryCode(ip);
        } catch (Exception e) {
            log.error("Error looking up geolocation for IP: {}", ip, e);
            return "unknown";
        }
    }
}

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
import me.amlu.shop.amlume_shop.security.model.GeoLocation;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SimpleGeoService implements GeoLocationService {
    private final RestTemplate restTemplate;
    private final LoadingCache<String, GeoLocation> geoCache;

    public SimpleGeoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.geoCache = CacheBuilder.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public GeoLocation load(@NotNull String ip) {
                        return lookupGeolocation(ip);
                    }
                });
    }

    //    @Override
    public String getGeolocation(String ip) {
        try {
            return String.valueOf(geoCache.get(ip));
        } catch (ExecutionException e) {
            log.error("Error getting geolocation for IP: {}", ip, e);
            return String.valueOf(GeoLocation.unknown());
        }
    }

    private GeoLocation lookupGeolocation(String ip) {
        try {
            String url = String.format("http://ip-api.com/json/%s", ip);
            ParameterizedTypeReference<Map<String, String>> typeRef = new ParameterizedTypeReference<Map<String, String>>() {
            };
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(url, HttpMethod.GET, null, typeRef);
//            ResponseEntity<Map<String, String>> response = restTemplate.getForEntity(url, typeRef);
//            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, String> data = response.getBody();
                return GeoLocation.builder()
                        .countryCode(data.get("countryCode"))
                        .countryName(data.get("country"))
//                        .continent(data.get("continent"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error looking up geolocation for IP: {}", ip, e);
        }
        return GeoLocation.unknown();
    }
}

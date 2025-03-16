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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for caching ASN lookups for a given IP address.
 * It uses a time-based cache with a maximum size of 10,000 entries and an expiration time of 24 hours.
 * If the cache is full, the least recently used entry will be evicted.
 * If the cache is empty, a new entry will be created.
 * If the cache is not empty, the entry will be refreshed if it is older than 24 hours.
 * If the cache is not empty, the entry will be returned if it is not older than 24 hours.
 *
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 2025-02-16
 * @see AsnLookupServiceImpl
 * @see CachedAsn
 */

@Slf4j
@Service
public class AsnTimeBasedCacheServiceImpl implements AsnTimeBasedCacheService {
    private final LoadingCache<String, CachedAsn> cache;

    public AsnTimeBasedCacheServiceImpl(AsnLookupService lookupService) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build(new CacheLoader<String, CachedAsn>() {
                @NotNull
                @Override
                public CachedAsn load(@NotNull String ip) {
                    return new CachedAsn(lookupService.lookupAsn(ip));
                }
            });
    }

    @Override
    public String getAsn(String ip) {
        try {
            CachedAsn cachedAsn = cache.get(ip);
            if (Duration.between(cachedAsn.getTimestamp(), Instant.now()).toHours() > 24) {
                cache.refresh(ip);
            }
            return cachedAsn.getAsn();
        } catch (ExecutionException e) {
            log.error("Cache retrieval failed for IP: {}", ip, e);
            return null;
        }
    }

    // Periodic backgroud refresh strategy to maintain cache consistency
    // Peridodically refresh the cache to ensure that the ASN information is up to date
    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    @Override
    public void refreshCache() {
        cache.asMap().forEach((ip, cachedAsn) -> {
            if (Duration.between(cachedAsn.getTimestamp(), Instant.now()).toHours() > 24) {
                cache.refresh(ip);
            }
        });
    }
}

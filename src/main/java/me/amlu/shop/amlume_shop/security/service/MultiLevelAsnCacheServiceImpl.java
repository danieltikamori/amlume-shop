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

import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 2023-09-13
 * This class is responsible for caching ASN (Autonomous System Number) lookups.
 * Multi-level caching is used to improve performance and reduce load on the ASN lookup service.
 * It uses a local cache and a Redis cache to improve performance and reduce load on the ASN lookup service.
 * The local cache is used to store ASN lookups for a short period of time, while the Redis cache is used to store ASN lookups for a longer period of time.
 * If an ASN lookup is not found in either cache, the ASN lookup service is used to perform the lookup.
 * The ASN lookup is then stored in both caches for future use.
 * This class is thread-safe and can be used in a multi-threaded environment.
 * The local cache is implemented using the Guava Cache library, while the Redis cache is implemented using the Spring Data Redis library.
 * The ASN lookup service is implemented using the MaxMind GeoIP2 library.
 * The ASN lookup service is configured to use the MaxMind GeoIP2 database, which is downloaded from the MaxMind website.
 */

@Slf4j
@Service
public class MultiLevelAsnCacheServiceImpl implements MultiLevelAsnCacheService {
    private final Cache<String, String> localCache;
    private final RedisTemplate<String, String> redisTemplate;
    private final AsnLookupService lookupService;

    public MultiLevelAsnCacheServiceImpl(Cache<String, String> localCache, RedisTemplate<String, String> redisTemplate, AsnLookupService lookupService) {
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.lookupService = lookupService;
    }

    @Override
    public String getAsn(String ip) {
        String asn = localCache.getIfPresent(ip);
        if (asn != null) {
            return asn;
        }

        asn = redisTemplate.opsForValue().get(ip);
        if (asn != null) {
            localCache.put(ip, asn);
            return asn;
        }

        asn = lookupService.lookupAsn(ip);
        if (asn != null) {
            localCache.put(ip, asn);
            redisTemplate.opsForValue().set(ip, asn, Duration.ofHours(24));
        }
        return asn;
    }
}

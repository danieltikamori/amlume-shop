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

@Slf4j
@Service
public class AsnSimpleCacheServiceImpl implements AsnSimpleCacheService {
    private final LoadingCache<String, String> cache;
    private final AsnLookupService lookupService;

    // TOFINISH
    public AsnSimpleCacheServiceImpl(AsnLookupService lookupService) {
        this.lookupService = lookupService;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<String, String>() {
                    @NotNull
                    @Override
                    public String load(@NotNull String ip) {
                        return lookupService.lookupAsn(ip);
                    }
                });
    }

    @Override
    public String getAsn(String ip) {
        try {
            return cache.get(ip);
        } catch (ExecutionException e) {
            log.error("Cache retrieval failed for IP: {}", ip, e);
            return null;
        }
    }

}

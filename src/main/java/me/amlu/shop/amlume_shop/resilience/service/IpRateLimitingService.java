/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Transactional
@Slf4j
@Service
public class IpRateLimitingService {
    private final LoadingCache<String, Integer> ipCache;
    private static final int MAX_ATTEMPTS_PER_IP = 100;
    private static final int BLOCK_DURATION_MINUTES = 30;
    
    public IpRateLimitingService() {
        ipCache = CacheBuilder.newBuilder()
            .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public Integer load(@NotNull String key) {
                    return 0;
                }
            });
    }
    
    public void checkIpRate(String ipAddress) throws TooManyAttemptsException {
        int attempts = ipCache.getUnchecked(ipAddress);
        if (attempts >= MAX_ATTEMPTS_PER_IP) {
            throw new TooManyAttemptsException(
                "Too many requests from this IP. Please try again later." +
                    " You can try again in " + BLOCK_DURATION_MINUTES + " minutes."
            );
        }
        ipCache.put(ipAddress, attempts + 1);
    }
}

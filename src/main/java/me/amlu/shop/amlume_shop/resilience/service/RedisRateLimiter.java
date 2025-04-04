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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;
    
    public RedisRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${rate.limit.max-requests}") int maxRequests,
            @Value("${rate.limit.window-seconds}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }
    
    public boolean tryAcquire(String key) {
        String redisKey = "ratelimit:" + key;

        return Boolean.TRUE.equals(redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            public Boolean execute(@NotNull RedisOperations operations) throws DataAccessException {
                operations.multi();

                operations.opsForValue().increment(redisKey, 1);
                operations.expire(redisKey, windowSeconds, TimeUnit.SECONDS);

                List<Object> results = operations.exec();
                Long count = (Long) results.getFirst();

                return count <= maxRequests;
            }
        }));
    }

//    public boolean tryAcquire(String key) {
//        String redisKey = "ratelimit:" + key;
//
//        return redisTemplate.execute(new SessionCallback<Boolean>() {
//            @Override
//            public Boolean execute(RedisOperations operations) throws DataAccessException {
//                operations.multi();
//
//                operations.opsForValue().increment(redisKey, 1);
//                operations.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
//
//                List<Object> results = operations.exec();
//                Long count = (Long) results.get(0);
//
//                return count <= maxRequests;
//            }
//        });
//    }
}

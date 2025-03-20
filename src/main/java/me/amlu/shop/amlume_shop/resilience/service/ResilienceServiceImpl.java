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

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import org.redisson.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Transactional
@Slf4j
@Service
public class ResilienceServiceImpl implements ResilienceService {

    private static final int IP_MAX_REQUESTS = 100; // Permissions limit for refresh period
    private static final Duration ipRefreshPeriod = java.time.Duration.ofMinutes(1); //After each period rate limiter sets its permissions count to RateLimiterConfig
    private static final Duration ipLockTimeoutDuration = java.time.Duration.ofSeconds(5); // Default wait for permission duration. Default value is 5 seconds.
    private static final int USER_MAX_REQUESTS = 5;
    private static final Duration userRefreshPeriod = java.time.Duration.ofMinutes(1);
    private static final Duration userLockTimeoutDuration = Duration.ofSeconds(5);


    private final RRateLimiter ipRateLimiter;
    private final RRateLimiter userRateLimiter;
    private final RedissonClient redissonClient;

    // RateType.OVERALL or RateType.PER_CLIENT

    public ResilienceServiceImpl(RedissonClient redissonClient) {
        this.ipRateLimiter = redissonClient.getRateLimiter("ipRateLimiter");
        this.userRateLimiter = redissonClient.getRateLimiter("userRateLimiter");
        this.redissonClient = redissonClient;

        // Initialize or update rate limits
        setRateLimiter(ipRateLimiter, IP_MAX_REQUESTS, ipRefreshPeriod);
        setRateLimiter(userRateLimiter, USER_MAX_REQUESTS, userRefreshPeriod);
    }

    @Override
    public void setRateLimiter(RRateLimiter rateLimiter, int maxRequests, Duration refreshPeriod) {
        // Set the rate limit (overwrites existing configuration)
        rateLimiter.setRate(RateType.OVERALL, maxRequests, refreshPeriod);
    }

    // Per instance of application
    @Override
    public boolean allowRequestByIpPerInstance(String ipAddress) throws TooManyAttemptsException {
        if (!tryAcquireWithTimeout(ipRateLimiter, ipLockTimeoutDuration)) {
            throw new TooManyAttemptsException("Too many requests from this IP address."); // Throw exception
        }
        return true; // Return true if permit acquired
    }

    // Distributed environment. Use the RAtomicLong and RLock (or RSemaphore) approach
    @Override
    public boolean allowRequestByIp(String ipAddress) throws TooManyAttemptsException {
        String counterKey = "rateLimit:ip:" + ipAddress; // Unique key per IP
        String lockKey = counterKey + ":lock"; // Lock key associated with the counter

        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(ipLockTimeoutDuration.toMillis(), TimeUnit.MILLISECONDS)) { // Acquire lock with timeout
                try {
                    long count = counter.get();
                    long now = System.currentTimeMillis();

                    if (count == 0 || (now - count) > ipRefreshPeriod.toMillis()) {
                        counter.set(1); // Reset and set to 1
                        return true;
                    } else if (count < IP_MAX_REQUESTS) {
                        counter.incrementAndGet();
                        return true;
                    } else {
                        throw new TooManyAttemptsException("Too many requests from this IP.");
                    }
                } finally {
                    lock.unlock(); // Always release the lock
                }
            } else {
                // Return false if lock cannot be acquired
                return false;
//                throw new TooManyAttemptsException("Could not acquire lock for IP rate limiting.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TooManyAttemptsException("IP rate limiting operation interrupted.");
        }
    }

    // Distributed environment
    @Override
    public boolean allowRequestByUsername(String username) throws TooManyAttemptsException {
        String counterKey = "rateLimit:user:" + username; // Unique key per user
        String lockKey = counterKey + ":lock"; // Lock key associated with the counter

        RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(userLockTimeoutDuration.toMillis(), TimeUnit.MILLISECONDS)) { // Acquire lock with timeout
                try {
                    long count = counter.get();
                    long now = System.currentTimeMillis();

                    if (count == 0 || (now - counter.get()) > userRefreshPeriod.toMillis()) {
                        counter.set(1); // Reset and set to 1
                        return true;
                    } else if (count < USER_MAX_REQUESTS) {
                        counter.incrementAndGet();
                        return true;
                    } else {
                        log.warn("Too many requests from user: {}", username);
                        return false; // Return false when count is equal to USER_MAX_REQUESTS
//                        throw new TooManyAttemptsException("Too many requests from this user.");
                    }
                } finally {
                    lock.unlock(); // Always release the lock
                }
            } else {
                throw new TooManyAttemptsException("Could not acquire lock for rate limiting."); // Lock timeout
//                return false; // Return false when lock cannot be acquired
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new TooManyAttemptsException("Rate limiting operation interrupted.");
        }
    }

    // Per instance of application
    @Override
    public boolean allowRequestByUserPerInstance(String username) throws TooManyAttemptsException {
        if (!tryAcquireWithTimeout(userRateLimiter, userLockTimeoutDuration)) {
            throw new TooManyAttemptsException("Too many requests from this user.");
        }
        return true; // Return true if permit acquired
    }

    @Override
    public boolean tryAcquireWithTimeout(RRateLimiter rateLimiter, Duration timeout) {
            return rateLimiter.tryAcquire(timeout);
    }

    public boolean isRateLimitExceeded(String username) {
        return allowRequestByUsername(username);
    }
}
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
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class ResilienceServiceImpl implements ResilienceService {

    // --- Configuration Properties ---
    @Value("${rate-limiting.ip.limit:100}") // Default 100 requests
    private long ipRequestLimit;

    @Value("${rate-limiting.ip.window-seconds:60}") // Default 60 second window
    private long ipWindowSeconds;

    @Value("${rate-limiting.username.limit:20}") // Default 20 requests per user
    private long usernameRequestLimit;

    @Value("${rate-limiting.username.window-seconds:60}") // Default 60 second window
    private long usernameWindowSeconds;

    // --- Dependencies ---
    private static final Logger log = LoggerFactory.getLogger(ResilienceServiceImpl.class);

    private final StringRedisTemplate redisTemplate;

    // --- Constructor ---
    public ResilienceServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- IP Rate Limiting (Fixed Window Counter) ---
    @Override
    public boolean allowRequestByIp(String ipAddress) throws RateLimitExceededException {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Attempted IP rate limiting with null or blank IP.");
            return false; // Deny requests with no IP
        }

        String redisKey = "rate_limit:ip:" + ipAddress;
        Long currentCount = null;

        try {
            // Increment the count for the IP address. This is atomic.
            currentCount = redisTemplate.opsForValue().increment(redisKey);

            // If it's the first request in the window (count is 1), set the expiry.
            if (currentCount != null && currentCount == 1) {
                // Set the key to expire after the window duration.
                redisTemplate.expire(redisKey, ipWindowSeconds, TimeUnit.SECONDS);
                log.trace("Set expiry for IP rate limit key: {} to {} seconds", redisKey, ipWindowSeconds);
            }

        } catch (DataAccessException e) { // Catch specific Redis exceptions
            // Log Redis errors,
            // would potentially allow the request in case of failure?
            // (Fail-open)
            log.error("Redis error during IP rate limiting check for IP [{}]: {}", ipAddress, e.getMessage());
            // Fail-open strategy: Allow request if Redis is down. Adjust if fail-closed is needed.
            return true;
        }

        if (currentCount == null) {
            // Should not happen if Redis is working, but handle defensively
            log.warn("Redis increment returned null for IP rate limiting key: {}", redisKey);
            return true; // Fail-open
        }

        // Check if the count exceeds the limit
        boolean allowed = currentCount <= ipRequestLimit;
        if (!allowed) {
            log.warn("IP Rate limit exceeded for IP [{}]. Count: {}/{}, Window: {}s", ipAddress, currentCount, ipRequestLimit, ipWindowSeconds);
            throw new RateLimitExceededException("Too many requests from this IP address."); // Throw exception as per previous logic
            // return false; // Alternative: just return false
        }

        log.trace("IP Rate limit check passed for IP [{}]. Count: {}/{}", ipAddress, currentCount, ipRequestLimit);
        return true; // Request allowed
    }

    // --- Username Rate Limiting (Fixed Window Counter) ---
    @Override
    public boolean allowRequestByUsername(String username) throws RateLimitExceededException {
        if (username == null || username.isBlank()) {
            log.warn("Attempted Username rate limiting with null or blank username.");
            // Allow anonymous actions? Or deny? Assuming allow for now, adjust if needed.
//            return true;
            return false; // Deny requests with no username
        }

        String redisKey = "rate_limit:user:" + username;
        Long currentCount = null;

        try {
            currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, usernameWindowSeconds, TimeUnit.SECONDS);
                log.trace("Set expiry for Username rate limit key: {} to {} seconds", redisKey, usernameWindowSeconds);
            }

        } catch (DataAccessException e) {
            log.error("Redis error during Username rate limiting check for User [{}]: {}", username, e.getMessage());
            // Fail-open strategy
            return true;
        }

        if (currentCount == null) {
            log.warn("Redis increment returned null for Username rate limiting key: {}", redisKey);
            return true; // Fail-open
        }

        boolean allowed = currentCount <= usernameRequestLimit;
        if (!allowed) {
            log.warn("Username Rate limit exceeded for User [{}]. Count: {}/{}, Window: {}s", username, currentCount, usernameRequestLimit, usernameWindowSeconds);
            throw new RateLimitExceededException("Too many requests for this user."); // Throw exception
            // return false; // Alternative: just return false
        }

        log.trace("Username Rate limit check passed for User [{}]. Count: {}/{}", username, currentCount, usernameRequestLimit);
        return true; // Request allowed
    }

}
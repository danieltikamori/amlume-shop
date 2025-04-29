/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import me.amlu.shop.amlume_shop.config.properties.TokenCacheProperties;
import org.slf4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Service
public class TokenCacheServiceImpl implements TokenCacheService {

    private static final String TOKEN_CACHE_PREFIX = "token_cache:"; // Prefix for individual token keys
    private static final String TOKEN_CACHE_KEYS_SET = "token_cache_keys"; // Key for the Set tracking all cache keys
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TokenCacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenCacheProperties tokenCacheProperties;

    public TokenCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                 TokenCacheProperties tokenCacheProperties) {
        this.redisTemplate = redisTemplate;
        this.tokenCacheProperties = tokenCacheProperties;
    }

    @Override
    public void putToken(String token, Map<String, Object> claims) {
        if (token == null || token.isBlank() || claims == null) {
            log.warn("Attempted to cache token with null/blank token string or null claims.");
            return;
        }
        String redisKey = TOKEN_CACHE_PREFIX + token;
        try {
            Duration ttl = Duration.ofMinutes(tokenCacheProperties.getExpirationMinutes());
            // Store the claims map in Redis with TTL
            redisTemplate.opsForValue().set(redisKey, claims, ttl);
            // Add the key to our tracking Set
            redisTemplate.opsForSet().add(TOKEN_CACHE_KEYS_SET, redisKey);
            log.trace("Cached claims for token prefix: {}", token.substring(0, Math.min(token.length(), 10))); // Log prefix for brevity
        } catch (Exception e) {
            log.error("Failed to cache token claims for key: {}", redisKey, e);
            // Optionally remove from tracking set if put failed? Depends on desired consistency.
            // redisTemplate.opsForSet().remove(TOKEN_CACHE_KEYS_SET, redisKey);
        }
    }

    @Override
    @SuppressWarnings("unchecked") // Suppress warning for casting Object to Map
    public Map<String, Object> getTokenClaims(String token) {
        if (token == null || token.isBlank()) {
            log.trace("Attempted to get claims for null/blank token string.");
            return null;
        }
        String redisKey = TOKEN_CACHE_PREFIX + token;
        try {
            Object cachedObject = redisTemplate.opsForValue().get(redisKey);
            if (cachedObject instanceof Map) {
                log.trace("Retrieved claims from cache for token prefix: {}", token.substring(0, Math.min(token.length(), 10)));
                return (Map<String, Object>) cachedObject;
            } else if (cachedObject != null) {
                // Log if we get something unexpected
                log.warn("Unexpected data type found in token cache for key {}. Expected Map, got {}. Returning null.",
                        redisKey, cachedObject.getClass().getName());
                return null;
            } else {
                log.trace("Claims not found in cache for token prefix: {}", token.substring(0, Math.min(token.length(), 10)));
                return null; // Not in cache or expired
            }
        } catch (Exception e) {
            log.error("Failed to retrieve token claims from cache for key: {}", redisKey, e);
            return null; // Return null on error
        }
    }

    @Override
    public void removeToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Attempted to remove token with null/blank token string.");
            return;
        }
        String redisKey = TOKEN_CACHE_PREFIX + token;
        try {
            // Delete the token key itself
            Boolean deleted = redisTemplate.delete(redisKey);
            // Remove the key from our tracking Set
            Long removedFromSet = redisTemplate.opsForSet().remove(TOKEN_CACHE_KEYS_SET, redisKey);
            if (Boolean.TRUE.equals(deleted) || (removedFromSet != null && removedFromSet > 0)) {
                log.trace("Removed token from cache (and tracking set) for prefix: {}", token.substring(0, Math.min(token.length(), 10)));
            } else {
                log.trace("Attempted to remove token not found in cache/tracking set for prefix: {}", token.substring(0, Math.min(token.length(), 10)));
            }
        } catch (Exception e) {
            log.error("Failed to remove token from cache for key: {}", redisKey, e);
        }
    }

    @Override
    public void clearCache() {
        log.warn("Clearing entire token cache from Redis!"); // Log as warning as this can be impactful
        try {
            // Get all keys tracked in our Set
            Set<Object> keysObject = redisTemplate.opsForSet().members(TOKEN_CACHE_KEYS_SET);

            if (keysObject != null && !keysObject.isEmpty()) {
                // Convert Set<Object> to Set<String> - necessary because RedisTemplate is <String, Object>
                // but the keys themselves are Strings.
                Set<String> keysToDelete = keysObject.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(java.util.stream.Collectors.toSet());

                if (!keysToDelete.isEmpty()) {
                    // Delete all the token keys
                    Long deletedKeysCount = redisTemplate.delete(keysToDelete);
                    log.debug("Deleted {} token entries from Redis.", deletedKeysCount);
                } else {
                    log.debug("Tracking set contained non-string keys, no token entries deleted based on set content.");
                }
            } else {
                log.debug("Token cache tracking set is empty or null. No entries to delete based on set.");
            }

            // Delete the tracking Set itself
            redisTemplate.delete(TOKEN_CACHE_KEYS_SET);
            log.info("Token cache cleared (including tracking set).");

        } catch (Exception e) {
            log.error("Failed to clear token cache from Redis.", e);
        }
    }

}

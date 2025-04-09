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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.*;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.config.properties.TokenCacheProperties;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.exceptions.TokenProcessingException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.security.model.TokenData;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static me.amlu.shop.amlume_shop.config.CacheConfig.CACHE_REFRESH_MINUTES;
import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.MAX_PAYLOAD_SIZE;
import static me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilServiceImpl.INSTANT_FORMATTER;

@Slf4j
@Service
public class TokenCacheServiceImpl implements TokenCacheService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final UserRepository userRepository;
    private final TokenCacheProperties cacheProperties;
    private final ObjectMapper objectMapper;
    private final TokenValidationService tokenValidationService;
    private final MetricRegistry metricRegistry;

    // Add caching
    private final LoadingCache<String, TokenData> tokenCache;
    private final LoadingCache<String, Optional<User>> userCache;
    private final Cache<String, Boolean> revokedTokensCache;

    public TokenCacheServiceImpl(RevokedTokenRepository revokedTokenRepository, UserRepository userRepository, TokenCacheProperties cacheProperties, ObjectMapper objectMapper, TokenValidationService tokenValidationService, MetricRegistry metricRegistry, LoadingCache<String, TokenData> tokenCache, LoadingCache<String, Optional<User>> userCache, Cache<String, Boolean> revokedTokensCache) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.userRepository = userRepository;
        this.cacheProperties = cacheProperties;
        this.objectMapper = objectMapper;
        this.tokenValidationService = tokenValidationService;
        this.metricRegistry = metricRegistry;

        // Initialize token cache
        this.tokenCache = CacheBuilder.newBuilder()
                .initialCapacity(cacheProperties.getInitialCapacity())
                .concurrencyLevel(TokenCacheProperties.CACHE_CONCURRENCY_LEVEL)
                .maximumSize(getConfigurableCacheSize())
                .expireAfterWrite(cacheProperties.getExpirationMinutes(), TimeUnit.MINUTES)
                .refreshAfterWrite(CACHE_REFRESH_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build(new CacheLoader<String, TokenData>() {
                    @NotNull
                    @Override
                    public TokenData load(@NotNull String token) {
                        return loadTokenData(token);
                    }

                    @NotNull
                    @Override
                    public Map<String, TokenData> loadAll(@NotNull Iterable<? extends String> keys) {
                        return batchLoadTokens(keys);
                    }
                });

        // Initialize user cache
        this.userCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Optional<User>>() {
                    @NotNull
                    @Override
                    public Optional<User> load(@NotNull String userId) {
                        return userRepository.findById(Long.valueOf(userId));
                    }
                });

        // Initialize revoked tokens cache
        this.revokedTokensCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(cacheProperties.getExpirationMinutes(), TimeUnit.MINUTES)
                .build();
    }


    private static final Cache<String, Instant> timeCache = CacheBuilder.newBuilder()
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public Optional<TokenData> getTokenData(String token) {
        try {
            return Optional.of(tokenCache.get(token));
//            return tokenCache.get(token, () -> loadTokenData(token));
        } catch (ExecutionException e) {
            log.error("Error retrieving token data from cache", e);
            return Optional.empty();
//            throw new TokenProcessingException("Failed to load token data", e);
        }
    }

    public TokenData getTokenWithFallback(String token) {
        try {
            return tokenCache.get(token, () -> loadTokenData(token));
        } catch (ExecutionException e) {
            log.warn("Cache load failed, falling back to direct load", e);
            return loadTokenData(token);
        }
    }

    private TokenData loadTokenData(String token) {
        Timer.Context timer = metricRegistry.timer("token.load.time").time();
        try {
            // Token loading logic here
            return parseAndValidateToken(token);
        } finally {
            timer.stop();
        }
    }

//    public TokenData loadTokenData(String token) {
//        return new TokenData(token);
//    }


    private Instant parseAndCacheIssuanceTime(String iatString) throws ExecutionException {
        return timeCache.get(iatString, () ->
                Instant.from(INSTANT_FORMATTER.parse(iatString)));
    }

    public boolean isTokenRevoked(String tokenId) {
        return revokedTokensCache.getIfPresent(tokenId) != null || revokedTokenRepository.existsByTokenId(tokenId);
    }

    public void revokeToken(String token) {
        revokedTokensCache.put(token, Boolean.TRUE);
        tokenCache.invalidate(token);
    }

    public Map<String, TokenData> batchLoadTokens(Iterable<? extends String> keys) {
        // Implement token loading logic here
        // This could involve querying the database or an external service
        // Replace the following with actual logic if needed
        Map<String, TokenData> loadedTokens = new HashMap<>();
        for (String key : keys) {
            try {
                loadedTokens.put(key, loadTokenData(key));
            } catch (Exception e) {
                log.error("Failed to load token data for key: {}", key, e);
            }
        }
        return loadedTokens;
    }

    public TokenData parseAndValidateToken(String token) {
        // Implement token loading logic here
        // This could involve querying the database or an external service
        // Replace the following with actual logic if needed
        Map<String, Object> tokenClaims = null;
        try {
            tokenClaims = tokenValidationService.validatePublicAccessToken(token);
        } catch (TokenValidationFailureException e) {
            log.error("Token validation failed for token: {}", token, e);
            throw new TokenProcessingException(e);
        } catch (Exception e) {
            log.error("Unexpected error during token validation for token: {}", token, e);
            throw new TokenValidationException(e);
        }
        return new TokenData(token, tokenClaims, (Instant) tokenClaims.get(PasetoClaims.EXPIRATION)); // Dummy data, replace with real logic
//        return new TokenData(tokenClaims.get(PasetoClaims.PASETO_ID).toString()); // Dummy data, replace with real logic
    }

    @Override
    public void validatePayload(String payload) throws TokenGenerationFailureException {
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_SIZE) {
            throw new TokenGenerationFailureException("Payload size exceeds maximum allowed size.");
        }
    }


    public int getConfigurableCacheSize() {
        return cacheProperties.getMaximumSize() +
                ThreadLocalRandom.current().nextInt(TokenCacheProperties.CACHE_WEIGHTED_MAXIMUM_WEIGHT_JITTER);
//        return (int) (Runtime.getRuntime().maxMemory() * 0.1 / ESTIMATED_ENTRY_SIZE); // Adjust ratio as needed

    }

    private boolean neverNeedsRefresh(String key) {
        // Add your logic here to determine if a refresh is never needed for the given key
        return key.startsWith("immutable:");
//        return false; // Default: always allow refresh
    }

    public Optional<User> getUser(String userId) {
        try {
            return userCache.get(userId);
        } catch (ExecutionException e) {
            log.error("Error retrieving user from cache", e);
            return Optional.empty();
        }
    }

    public CacheStats getTokenCacheStats() {
        return tokenCache.stats();
    }

}

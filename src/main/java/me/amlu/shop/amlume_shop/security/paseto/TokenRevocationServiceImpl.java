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


import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.shop.amlume_shop.exceptions.TokenRevocationException;
import me.amlu.shop.amlume_shop.exceptions.TokenRevokedException;
import me.amlu.shop.amlume_shop.security.model.RevokedToken;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import org.paseto4j.commons.PasetoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;


@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationServiceImpl.class);

    private static final String REVOKED_TOKEN_PREFIX = "revoked_tokens:"; // Prefix for Valkey keys

    private final MeterRegistry meterRegistry;
    private final RevokedTokenRepository revokedTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenRevocationService self;

    @Value("${cache.revoked-token.ttl-seconds:3600}") // Default to 1 hour
    private long revokedTokenCacheTtlSeconds;

    // Modify constructor
    public TokenRevocationServiceImpl(
            MeterRegistry meterRegistry, RevokedTokenRepository revokedTokenRepository,
            RedisTemplate<String, Object> redisTemplate, // Inject RedisTemplate
            @Lazy TokenRevocationService self
    ) {
        this.meterRegistry = meterRegistry;
        this.revokedTokenRepository = revokedTokenRepository;
        this.redisTemplate = redisTemplate; // Assign injected RedisTemplate
        this.self = self;
    }

    @Transactional
    @Override
    public void revokeToken(String tokenId, String reason) throws TokenRevocationException {
        log.info("Revoking token with ID: {}, Reason: {}", tokenId, reason);
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("Attempted to revoke token with null or blank ID.");
            // Depending on requirements, either return or throw an exception
            // throw new TokenRevocationException("Token ID cannot be null or blank");
            return;
        }
        String redisKey = REVOKED_TOKEN_PREFIX + tokenId;

        try {
            // Check if token is already revoked (using the isTokenRevoked method)
            if (isTokenRevoked(tokenId)) { // isTokenRevoked now handles cache check
                // Verify the database consistency
                // (optional but good practice)
                if (revokedTokenRepository.existsByTokenId(tokenId)) {
                    log.info("Token already revoked (verified in DB): {}", tokenId);
                    return;
                } else {
                    log.warn("Token was considered revoked (possibly cached), but not in database. Attempting to revoke again. TokenId: {}", tokenId);
                    // Proceed to save to DB and update cache anyway
                }
            }

            // Save revoked token to database
            saveRevokedTokenToDatabaseAndCache(tokenId, reason, redisKey);
            log.info("Token with ID {} successfully revoked and cached.", tokenId);

        } catch (PasetoException e) { // Catch specific exceptions if possible
            log.error("Failed to revoke token with ID: {}", tokenId, e);
            // Consider throwing a more specific exception if PasetoException isn't right
            throw new TokenRevocationException("Could not revoke token due to PASETO error", e);
        } catch (Exception e) { // Catch broader exceptions
            log.error("Unexpected error during token revocation for ID: {}", tokenId, e);
            throw new TokenRevocationException("Unexpected error revoking token", e);
        }
    }

    // --- Async Persistence (Optional) ---
    // If revokeToken performance is critical, you could make DB persistence async.
    // Note: This adds complexity regarding transactionality and cache consistency.

    @Async
    @Transactional
    @Override
    public void revokeTokenAsync(String tokenId, String reason) {
        // This method is intended to be called internally if async persistence is desired.
        // The primary revokeToken method would handle the cache update immediately
        // and then call this async method.

        if (tokenId == null || tokenId.isBlank()) {
            log.warn("[Async] Attempted to revoke a null or blank token ID.");
            return;
        }
        Instant now = Instant.now();
        try {
            // Check if already exists in DB before saving (optional, save handles upsert if ID exists)
            if (!revokedTokenRepository.existsByTokenId(tokenId)) {
                RevokedToken revokedToken = new RevokedToken(tokenId, reason);
                revokedTokenRepository.save(revokedToken);
                log.info("[Async] Persisted revocation record for token ID: {}", tokenId);
                meterRegistry.counter("paseto.token.revoked.async").increment();
            } else {
                log.debug("[Async] Revocation record for token ID {} already exists in DB.", tokenId);
            }
        } catch (Exception e) {
            log.error("[Async] Failed to persist revocation record for token ID: {}", tokenId, e);
            // Error handling for async operation (e.g., logging, metrics)
        }
    }

    // Method to check revocation status using RedisTemplate
    // This method now encapsulates cache checking, DB fallback, and cache updates (including negative caching)
    private boolean isTokenRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("Attempted to check revocation for null or blank token ID.");
            return true; // Fail-safe: Treat invalid ID as potentially revoked
        }
        String redisKey = REVOKED_TOKEN_PREFIX + tokenId;
        try {
            // 1. Check Redis cache
            Object cachedResult = redisTemplate.opsForValue().get(redisKey);

            if (cachedResult != null) {
                // Found in cache, return the cached value
                // Ensure it's actually a Boolean before casting
                if (cachedResult instanceof Boolean) {
                    log.trace("Revocation status for token {} found in cache: {}", tokenId, cachedResult);
                    meterRegistry.counter("paseto.token.revocation.check.cache.hit").increment();
                    return (Boolean) cachedResult;
                } else {
                    // Data in cache is corrupted or unexpected type
                    log.warn("Unexpected data type found in cache for key {}. Expected Boolean, got {}. Re-fetching from DB.", redisKey, cachedResult.getClass().getName());
                    // Proceed to DB check, cache will be overwritten
                }
            }

            meterRegistry.counter("paseto.token.revocation.check.cache.miss").increment();
            log.trace("Revocation status for token {} not found in cache. Checking database.", tokenId);

            // 2. If not in cache, check the database
            boolean isRevokedInDb = revokedTokenRepository.existsByTokenId(tokenId);
            Duration ttl = Duration.ofSeconds(revokedTokenCacheTtlSeconds);

            // 3. Update cache based on DB result
            if (isRevokedInDb) {
                log.debug("Token {} found revoked in database. Caching status.", tokenId);
                // Store 'true' in cache indicating it IS revoked
                redisTemplate.opsForValue().set(redisKey, Boolean.TRUE, ttl);
                return true;
            } else {
                log.debug("Token {} not found revoked in database. Caching negative status.", tokenId);
                // Store 'false' in cache indicating it is NOT revoked (negative caching)
                redisTemplate.opsForValue().set(redisKey, Boolean.FALSE, ttl);
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking token revocation status for tokenId: {}. Assuming revoked as fail-safe.", tokenId, e);
            // Fail-safe: In case of error accessing cache/DB, assume token might be revoked.
            return true;
        }
    }

    @Override
    public void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException {
        Object tokenIdClaim = claims.get("jti"); // Get claim as Object first
        if (tokenIdClaim == null) {
            log.warn("Attempted to validate revocation for token without 'jti' claim.");
            throw new TokenRevokedException("Token invalid: Missing 'jti' claim."); // Or a different exception type
        }
        String tokenId = tokenIdClaim.toString();

        // Delegate check to the isTokenRevoked method which handles caching
        if (isTokenRevoked(tokenId)) {
            log.warn("Token validation failed: Token with ID {} has been revoked.", tokenId);
            throw new TokenRevokedException("Token has been revoked");
        }
        log.trace("Token validation successful: Token with ID {} is not revoked.", tokenId);
        // No need to explicitly put 'false' here anymore, isTokenRevoked handles caching.
    }

    @Override
    public void revokeAccessToken(String accessToken, Duration accessTokenDuration, String reason) {
        log.info("Revoking access token with ID: {}, Reason: {}", accessToken, reason);
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Attempted to revoke access token with null or blank ID.");
            // Depending on requirements, either return or throw an exception
            // throw new TokenRevocationException("Token ID cannot be null or blank");
            return;
        }
        String redisKey = REVOKED_TOKEN_PREFIX + accessToken;

        try {
            // Check if token is already revoked (using the isTokenRevoked method)
            if (isTokenRevoked(accessToken)) { // isTokenRevoked now handles cache check
                // Verify the database consistency
                // (optional but good practice)
                if (revokedTokenRepository.existsByTokenId(accessToken)) {
                    log.info("Access Token already revoked (verified in DB): {}", accessToken);
                    return;
                } else {
                    log.warn("Access Token was considered revoked (possibly cached), but not in database. Attempting to revoke again. TokenId: {}", accessToken);
                    // Proceed to save to DB and update cache anyway
                }
            }

            // Save revoked token to database
            saveRevokedTokenToDatabaseAndCache(accessToken, reason, redisKey);
            log.info("Access Token with ID {} successfully revoked and cached.", accessToken);

        } catch (PasetoException e) { // Catch specific exceptions if possible
            log.error("Failed to revoke access token with ID: {}", accessToken, e);
            // Consider throwing a more specific exception if PasetoException isn't right
            throw new TokenRevocationException("Could not revoke access token due to PASETO error", e);
        } catch (Exception e) { // Catch broader exceptions
            log.error("Unexpected error during access token revocation for ID: {}", accessToken, e);
            throw new TokenRevocationException("Unexpected error revoking access token", e);
        }
    }

    @Override
    public void revokeRefreshToken(String refreshToken, Duration refreshTokenDuration, String reason) {
        log.info("Revoking refresh token with ID: {}, Reason: {}", refreshToken, reason);
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Attempted to revoke refresh token with null or blank ID.");
            // Depending on requirements, either return or throw an exception
            // throw new TokenRevocationException("Token ID cannot be null or blank");
            return;
        }
        String redisKey = REVOKED_TOKEN_PREFIX + refreshToken;

        try {
            // Check if token is already revoked (using the isTokenRevoked method)
            if (isTokenRevoked(refreshToken)) { // isTokenRevoked now handles cache check
                // Verify the database consistency
                // (optional but good practice)
                if (revokedTokenRepository.existsByTokenId(refreshToken)) {
                    log.info("Refresh Token already revoked (verified in DB): {}", refreshToken);
                    return;
                } else {
                    log.warn("Refresh Token was considered revoked (possibly cached), but not in database. Attempting to revoke again. TokenId: {}", refreshToken);
                    // Proceed to save to DB and update cache anyway
                }
            }

            // Save revoked token to database
            saveRevokedTokenToDatabaseAndCache(refreshToken, reason, redisKey);
            log.info("Refresh Token with ID {} successfully revoked and cached.", refreshToken);

        } catch (PasetoException e) { // Catch specific exceptions if possible
            log.error("Failed to revoke refresh token with ID: {}", refreshToken, e);
            // Consider throwing a more specific exception if PasetoException isn't right
            throw new TokenRevocationException("Could not revoke refresh token due to PASETO error", e);
        } catch (Exception e) { // Catch broader exceptions
            log.error("Unexpected error during refresh token revocation for ID: {}", refreshToken, e);
            throw new TokenRevocationException("Unexpected error revoking refresh token", e);
        }

    }

    private void saveRevokedTokenToDatabaseAndCache(String refreshToken, String reason, String redisKey) {
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setTokenId(refreshToken);
        revokedToken.setRevokedAt(Instant.now());
        revokedToken.setReason(reason);
        revokedTokenRepository.save(revokedToken);

        // Add to revoked tokens cache using RedisTemplate with TTL
        Duration ttl = Duration.ofSeconds(revokedTokenCacheTtlSeconds);
        redisTemplate.opsForValue().set(redisKey, Boolean.TRUE, ttl); // Store TRUE
    }

}

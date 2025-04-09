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


import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TokenRevocationException;
import me.amlu.shop.amlume_shop.exceptions.TokenRevokedException;
import me.amlu.shop.amlume_shop.security.model.RevokedToken;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import org.paseto4j.commons.PasetoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;


@Slf4j
@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {


    private static final String REVOKED_TOKEN_PREFIX = "revoked_tokens:"; // Prefix for Valkey keys

    private final RevokedTokenRepository revokedTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenRevocationService self;

    @Value("${cache.revoked-token.ttl-seconds:3600}") // Default to 1 hour
    private long revokedTokenCacheTtlSeconds;

    // Modify constructor
    public TokenRevocationServiceImpl(
            RevokedTokenRepository revokedTokenRepository,
            RedisTemplate<String, Object> redisTemplate, // Inject RedisTemplate
            TokenRevocationService self
    ) {
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
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenId(tokenId);
            revokedToken.setRevokedAt(Instant.now());
            revokedToken.setReason(reason);
            revokedTokenRepository.save(revokedToken);

            // Add to revoked tokens cache using RedisTemplate with TTL
            Duration ttl = Duration.ofSeconds(revokedTokenCacheTtlSeconds);
            redisTemplate.opsForValue().set(redisKey, Boolean.TRUE, ttl); // Store TRUE
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
                    return (Boolean) cachedResult;
                } else {
                    // Data in cache is corrupted or unexpected type
                    log.warn("Unexpected data type found in cache for key {}. Expected Boolean, got {}. Re-fetching from DB.", redisKey, cachedResult.getClass().getName());
                    // Proceed to DB check, cache will be overwritten
                }
            }
            log.trace("Revocation status for token {} not found in cache. Checking database.", tokenId);

            // 2. If not in cache, check database
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
}
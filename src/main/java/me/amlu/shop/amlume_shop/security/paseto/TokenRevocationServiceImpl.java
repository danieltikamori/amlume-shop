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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;


@Slf4j
@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {

//    private static final long TOKEN_CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
//    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    //    private final Executor tokenBackgroundTasksExecutor;
    private final RevokedTokenRepository revokedTokenRepository;
    private final Cache<String, Boolean> revokedTokensCache;
    private final Cache<String, Object> userCache;
    private final TokenRevocationService self;
    private final TokenValidationService tokenValidationService;

    //    Async token revocation implementation, use the tokenBackgroundTasksExecutor. The code below should be in the constructor.
//    @Qualifier("tokenBackgroundTasksExecutor") Executor tokenBackgroundTasksExecutor
    public TokenRevocationServiceImpl(RevokedTokenRepository revokedTokenRepository, Cache<String, Boolean> revokedTokensCache, Cache<String, Object> userCache, TokenRevocationService self, TokenValidationService tokenValidationService) {
//        this.tokenBackgroundTasksExecutor = tokenBackgroundTasksExecutor;
        this.revokedTokenRepository = revokedTokenRepository;
        this.revokedTokensCache = revokedTokensCache;
        this.userCache = userCache;
        this.self = self;
        this.tokenValidationService = tokenValidationService;
    }

    // Methods to revoke tokens

    @Transactional
    @Override
    public void revokeToken(String tokenId, String reason) throws TokenRevocationException {
        log.info("Revoking token with ID: {}, Reason: {}", tokenId, reason);
        try {
//            // Parse token to get its ID and expiration
//            Map<String, Object> claims = tokenValidationService.extractClaimsFromPublicAccessToken(token);
//            String tokenId = (String) claims.get("jti");
//            if (tokenId == null) {
//                throw new SecurityException("Token ID missing");
//            }
//
//            String username = (String) claims.get("sub");
//            if (username == null) {
//                throw new SecurityException("Subject missing");
//            }
//            // Calculate remaining time until token expiration(Get)
//            Instant expiration = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse((String) claims.get("exp")));

            // Check if token is already revoked
            if (isTokenRevoked(tokenId)) {
                //Verify the database.
                if (revokedTokenRepository.existsByTokenId(tokenId)) {
                    log.info("Token already revoked in database: {}", tokenId);
                    return;
                } else {
                    log.warn("Token was in cache, but not in database, attempting to revoke again. TokenId: {}", tokenId);
                }
            }

            // Save revoked token to database
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenId(tokenId);
//            revokedToken.setUsername(username);
//            revokedToken.setExpirationDate(expiration);
            revokedToken.setRevokedAt(Instant.now());
            revokedToken.setReason(reason);

            revokedTokenRepository.save(revokedToken);

            // Add to revoked tokens cache with remaining time until expiration
            revokedTokensCache.put(tokenId, true);
            log.info("Token with ID {} successfully revoked.", tokenId);

        } catch (PasetoException e) {
            log.error("Failed to revoke token with ID: {}", tokenId, e);
            throw new SecurityException("Could not revoke token", e);
        }
    }

//    @Transactional
//    @Override
//    public void revokeAllUserTokens(String username, String reason) {
//        try {
//            List<RevokedToken> userTokens = revokedTokenRepository.findByUsername(username);
//            for (RevokedToken token : userTokens) {
//                token.setRevokedAt(Instant.now());
//                token.setReason(reason);
//                revokedTokensCache.put(token.getTokenId(), true);
//            }
//            revokedTokenRepository.saveAll(userTokens);
//            log.info("Revoked all tokens for user: {}", username);
//        } catch (Exception e) {
//            log.error("Error revoking tokens for user: {}", username, e);
//            throw new SecurityException("Could not revoke tokens for user", e);
//        }
//    }

    // TODO: Async revokeAllUserTokens with AuthController class
//    @Async("tokenBackgroundTasksExecutor")
//    @Transactional
//    @Override
//    public CompletableFuture<Void> revokeAllUserTokens(String username, String reason) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                String tokenId =
//                revokedTokenRepository.updateRevokedAtAndReasonByUsernameAndRevokedAtIsNull(username, Instant.now(), reason);
//                List<RevokedToken> userTokens = revokedTokenRepository.findByUsername(username);
//                for (RevokedToken token : userTokens) {
//                    revokedTokensCache.put(token.getTokenId(), true); // Consider bulk updates, although the put method overhead is minimal. Guava cache does not support bulk operations.
//                }
//                log.info("Revoked all tokens for user: {}", username);
//            } catch (Exception e) {
//                log.error("Error revoking tokens for user: {}", username, e);
//                throw new SecurityException("Could not revoke tokens for user", e);
//            }
//        }, tokenBackgroundTasksExecutor).exceptionally(ex -> {
//            log.error("Error revoking tokens for user: {}", username, ex);
//            return null;
//        });
//    }


    private boolean isTokenRevoked(String tokenId) {
        try {
            // First check cache
            Boolean cachedResult = revokedTokensCache.getIfPresent(tokenId);
            if (cachedResult != null) {
                return cachedResult;
            }

            // If not in cache, check database
            boolean isRevoked = revokedTokenRepository.existsByTokenId(tokenId);
            if (isRevoked) {
                revokedTokensCache.put(tokenId, true);
            }
            return isRevoked;
        } catch (Exception e) {
            log.error("Error checking token revocation status for tokenId: {}", tokenId, e);
            return true; // Fail-safe: assume token is revoked if there's an error
        }
    }

    @Override
    public void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException {

        // Extract the token ID from the claims
        String tokenId = claims.get("jti").toString();
        // First check the cache
        Boolean isRevoked = revokedTokensCache.getIfPresent(tokenId);
        if (Boolean.TRUE.equals(isRevoked)) {
            throw new TokenRevokedException("Token has been revoked");
        }

        // If not in cache, check the database
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            // Add to cache for future checks
            revokedTokensCache.put(tokenId, true);
            throw new TokenRevokedException("Token has been revoked");
        }

        // Add to negative cache if not revoked
        revokedTokensCache.put(tokenId, false);
    }
}
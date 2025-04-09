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

import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class TokenCleanupServiceImpl implements TokenCleanupService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final Cache<String, Boolean> revokedTokensCache;
    private final Cache<String, Object> userCache;

    private final TokenCleanupService self;

    public TokenCleanupServiceImpl(RevokedTokenRepository revokedTokenRepository, Cache<String, Boolean> revokedTokensCache, Cache<String, Object> userCache, TokenCleanupService self) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.revokedTokensCache = revokedTokensCache;
        this.userCache = userCache;
        this.self = self;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void cleanupExpiredTokens() {
        self.cleanupExpiredTokensAsync();
    }
    @Async
    @Transactional
    public void cleanupExpiredTokensAsync() {
        try {
            Instant now = Instant.now();
            revokedTokenRepository.deleteByExpirationDateBefore(now);
            revokedTokenRepository.deleteByRevokedAtBefore(now.minusSeconds(60 * 60 * 24 * 30)); // 30 days
            revokedTokensCache.cleanUp();
            userCache.cleanUp();
            log.debug("Cleaned up expired tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }

    // Use CompletableFuture if the asynchronous cleanup operations become more complex
//    @Async
//    @Transactional
//    public CompletableFuture<Void> cleanupExpiredTokensAsync() {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                Instant now = Instant.now();
//                revokedTokenRepository.deleteByExpirationDateBefore(now);
//                revokedTokensCache.cleanUp();
//                userCache.cleanUp();
//                log.debug("Cleaned up expired tokens");
//            } catch (Exception e) {
//                log.error("Error cleaning up expired tokens", e);
//                throw new RuntimeException("Error cleaning up expired tokens", e); // Re-throw for CompletableFuture
//            }
//        }).exceptionally(ex -> {
//            log.error("Async cleanup failed: {}", ex.getMessage());
//            return null; // Handle the exception
//        });
//    }
}

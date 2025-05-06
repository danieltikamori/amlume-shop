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

import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenCleanupServiceImpl implements TokenCleanupService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TokenCleanupServiceImpl.class);
    private final RevokedTokenRepository revokedTokenRepository;
    private final TokenCleanupService self; // Keep for self-injection for @Async/@Transactional

    public TokenCleanupServiceImpl(RevokedTokenRepository revokedTokenRepository,
                                   @Lazy TokenCleanupService self) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.self = self;
    }

    /**
     * Scheduled task to trigger the cleanup of old revoked token database records.
     * Runs every hour at the start of the hour.
     */
    @Scheduled(cron = "0 0 * * * *") // Runs hourly
    @Override
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of old revoked token database records.");
        // Call the async, transactional method
        self.cleanupExpiredTokensAsync();
    }

    /**
     * Asynchronously cleans up old revoked token records from the database.
     * Records older than 30 days (based on revocation timestamp) are deleted.
     * Cache cleanup for Redis/Valkey is handled automatically by TTL.
     */
    @Async // Consider specifying a task executor bean name if you have one configured: @Async("taskExecutor")
    @Transactional
    public void cleanupExpiredTokensAsync() {
        try {
            // Define the cutoff timestamp for old records (e.g., 30 days ago)
            Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
            log.debug("Deleting revoked token records from database where revokedAt is before {}", cutoff);

            // Delete old revocation records from the database based on when they were revoked
            int deletedCount = revokedTokenRepository.deleteByRevokedAtBefore(cutoff);

            // NOTE: The Redis/Valkey entries for revoked tokens expire automatically
            // based on the TTL set in TokenRevocationServiceImpl.
            // No explicit cache cleanup (like cache.cleanUp()) is needed here.

            // Optional: If RevokedToken had an 'expirationDate' field representing the *original*
            // token's expiry, you might want to delete based on that too, but it seems less likely.
            // The current logic focuses on removing *old revocation records*.
            // Commenting out the potentially incorrect/unnecessary line:
            // revokedTokenRepository.deleteByExpirationDateBefore(Instant.now());

            if (deletedCount > 0) {
                log.info("Finished cleanup task. Deleted {} revoked token records older than 30 days from the database.", deletedCount);
            } else {
                log.info("Finished cleanup task. No old revoked token records found to delete from the database.");
            }

        } catch (Exception e) {
            log.error("Error during asynchronous cleanup of revoked token database records", e);
            // Consider adding monitoring/alerting here if the cleanup fails repeatedly
        }
    }
}

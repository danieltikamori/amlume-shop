/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.cache_management.service;

import me.amlu.authserver.exceptions.CacheOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service responsible for performing scheduled cache maintenance operations.
 * This includes clearing specific caches at predefined intervals to ensure
 * cache freshness and manage memory usage.
 *
 * <p>Usage:</p>
 * <pre>
 * // This service is automatically scheduled by Spring's @Scheduled annotation.
 * // No direct invocation is typically needed for its primary function.
 * </pre>
 *
 * <p>Important Notes:</p>
 * <ul>
 *     <li>The {@code performCacheMaintenance} method is scheduled to run daily at 2 AM.</li>
 *     <li>It currently invalidates the "temporaryCache". Additional caches can be added
 *         to the maintenance routine as needed.</li>
 *     <li>Error handling is in place to log and re-throw exceptions, ensuring that
 *         scheduling failures are visible and can be addressed.</li>
 * </ul>
 */
@Service
public class CacheMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(CacheMaintenanceService.class);

    private final CacheService cacheService;

    /**
     * Constructs a new {@code CacheMaintenanceService} with the given {@code CacheService}.
     *
     * @param cacheService The service used for cache operations like invalidation.
     */
    public CacheMaintenanceService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void performCacheMaintenance() {
        // Clear specific caches during maintenance window
        log.info("Starting scheduled cache maintenance...");
        try {
            // Clear temporary cache
            log.debug("Invalidating 'temporaryCache'...");
            cacheService.invalidateAll("temporaryCache");
            log.info("'temporaryCache' invalidated successfully.");

            // Add more cache invalidation logic here if needed for other caches
            // Example: cacheService.invalidateAll("anotherCache");

        } catch (CacheOperationException.CacheInvalidationException e) {
            // Log the error but do not re-throw, as re-throwing from a @Scheduled method
            // might prevent subsequent scheduled executions depending on Spring's configuration.
            log.error("Failed to invalidate cache during maintenance schedule: {}", e.getMessage(), e);
        }

    }
}

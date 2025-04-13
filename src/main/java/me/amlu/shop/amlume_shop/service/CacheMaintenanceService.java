/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.exceptions.CacheOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(CacheMaintenanceService.class);

    private final CacheService cacheService;

    public CacheMaintenanceService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void performCacheMaintenance() {
        // Clear specific caches during maintenance window
        try {
            // Clear temporary cache
            cacheService.invalidateAll("temporaryCache");
        } catch (CacheOperationException.CacheInvalidationException e) {
            log.error("Cache invalidation in maintenance schedule failed: {}", e.getMessage(), e);
            throw new CacheOperationException.CacheInvalidationException(e.getMessage(), e);
        }

    }
}

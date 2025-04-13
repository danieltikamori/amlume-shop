/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;


/**
 * General configuration related to caching (now primarily enabling it).
 * Specific CacheManager and Redis configurations are in ValkeyCacheConfig.
 * In-memory Guava caches and custom metrics have been removed in favor of Redis and Actuator.
 */
@EnableCaching
@Configuration
public class CacheConfig {

    // --- Logging/Comments about removed features ---
    /*
     * NOTE: Custom cache metrics (CacheMetrics, CacheMonitoringAspect, Dropwizard/Codahale metrics)
     * and Guava-based in-memory caches (rolesCache, rateLimitCache, asnCache, tokenCache)
     * have been removed.
     *
     * Caching is now handled by the RedisCacheManager configured in ValkeyCacheConfig.
     * Metrics are provided automatically by Spring Boot Actuator and Micrometer
     * when dependencies are included and properties are set (see recommendations).
     *
     * Logic previously using Guava caches should be migrated:
     * - Use @Cacheable/@CacheEvict on service methods (for roles, asn, tokens).
     * - Use direct Redis atomic operations (e.g., StringRedisTemplate) for rate limiting.
     */

}

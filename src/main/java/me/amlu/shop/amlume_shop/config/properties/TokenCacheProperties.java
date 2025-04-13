/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import static me.amlu.shop.amlume_shop.commons.Constants.ESTIMATED_ENTRY_SIZE;

@ConfigurationProperties(prefix = "cache.token")
@Component
@Validated
@Getter
@Setter
public class TokenCacheProperties {

    // Cache configuration
//    private static final int CACHE_INITIAL_CAPACITY = Math.min(100, Runtime.getRuntime().availableProcessors() * 10);
    public static final double MAX_HEAP_RATIO = 0.1; // Use max 10% of heap
    public static final long CACHE_MAXIMUM_SIZE =
            (long) (Runtime.getRuntime().maxMemory() * MAX_HEAP_RATIO / ESTIMATED_ENTRY_SIZE);
    public static final int CACHE_CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors();
//    private static final long CACHE_EXPIRATION_AFTER_WRITE = 60 * 60 * 1000; // 1 hour
//    private static final long CACHE_EXPIRATION_AFTER_ACCESS = 30 * 60 * 1000;
//    private static final long CACHE_REFRESH_AFTER_WRITE = 30 * 60 * 1000;
//    private static final long CACHE_REFRESH_AFTER_ACCESS = 15 * 60 * 1000;
//    private static final int CACHE_MAXIMUM_WEIGHT = 100;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE = 60 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS = 30 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE = 30 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS = 15 * 60 * 1000;
//    private static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT = 100;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE_JITTER = 5 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS_JITTER = 2 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE_JITTER = 2 * 60 * 1000;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS_JITTER = 1 * 60 * 1000;
    public static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT_JITTER = 10;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE_JITTER_RATIO = 10;
//    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS_JITTER_RATIO = 5;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE_JITTER_RATIO = 5;
//    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS_JITTER_RATIO = 2;
//    private static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT_JITTER_RATIO = 20;

    @Min(10)
    private int initialCapacity = Math.min(100, Runtime.getRuntime().availableProcessors() * 10); // or 100

    @Min(100)
    private int maximumSize = (int) CACHE_MAXIMUM_SIZE;

    @Min(5)
    private int expirationMinutes = 30;

}


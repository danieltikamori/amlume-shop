/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RScoredSortedSet;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class TokenJtiServiceImpl implements TokenJtiService {

    private final RBloomFilter<String> jtiBloomFilter;
    private final RScoredSortedSet<String> jtiExpirations;
    private final ExecutorService virtualThreadExecutor;

    public TokenJtiServiceImpl(RBloomFilter<String> jtiBloomFilter, RScoredSortedSet<String> jtiExpirations, ExecutorService virtualThreadExecutor) {
        this.jtiBloomFilter = jtiBloomFilter;
        this.jtiExpirations = jtiExpirations;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    public boolean isJtiValid(String jti) {
        return CompletableFuture.supplyAsync(() -> { // Use CompletableFuture with virtual threads
            if (!jtiBloomFilter.contains(jti)) { // Check Bloom filter first
                return false; // Not in Bloom filter, definitely not valid
            }

            // Only check Sorted Set if it's in the Bloom filter (potentially valid)
            long now = System.currentTimeMillis();

            // Remove expired entries (using rank, most efficient)
            Integer rank = jtiExpirations.rank(String.valueOf(now)); // Get the rank of the current time. This is the number of entries that are less than or equal to the current time
            if (rank != null) {
                jtiExpirations.removeRangeByRank(0, rank);
            }

            return jtiExpirations.contains(jti);
        }, virtualThreadExecutor).join(); // Wait for result
    }

    @Override
    public void storeJti(String jti, Duration duration) {
        CompletableFuture.runAsync(() -> {
            jtiBloomFilter.add(jti);
            long expiry = System.currentTimeMillis() + duration.toMillis();
            jtiExpirations.add(expiry, jti); // Add to Sorted Set with expiration time as score
        }, virtualThreadExecutor).join(); // Wait for completion
    }

    @Override
    public void initialize() {
        jtiBloomFilter.tryInit(1000000, 0.01); // Initialize (adjust size and false-positive rate)
    }
}

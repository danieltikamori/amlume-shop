/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import me.amlu.authserver.security.config.properties.AsnProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Service
public class AsnReputationServiceImpl implements AsnReputationService {

    private static final Logger log = LoggerFactory.getLogger(AsnReputationServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final AsnProperties asnProperties; // Inject properties

    private static final String REPUTATION_KEY_PREFIX = "reputation:asn:";
    private static final String FIELD_SUSPICIOUS = "suspicious_count";
    private static final String FIELD_LEGITIMATE = "legitimate_count";
    private static final String FIELD_LAST_UPDATED = "last_updated";

    // Default reputation score for ASNs not yet seen
    private static final double DEFAULT_SCORE = 0.5;
    // Default TTL for reputation entries (can align with staleThreshold or be separate)
    // Let's use staleThreshold + a buffer
    private final Duration reputationEntryTtl;


    public AsnReputationServiceImpl(StringRedisTemplate redisTemplate, AsnProperties asnProperties) {
        this.redisTemplate = redisTemplate;
        this.asnProperties = asnProperties;
        // Calculate TTL based on stale threshold, add a buffer (e.g., 1 day)
        this.reputationEntryTtl = asnProperties.getReputationTtl();
        log.info("AsnReputationService initialized using Redis. Reputation entry TTL: {}", this.reputationEntryTtl);
    }

    // REMOVE AsnReputation inner class

    @Override
    public void recordActivity(String asn, boolean isSuspicious) {
        if (asn == null || asn.isBlank()) {
            log.warn("Attempted to record activity for null or blank ASN.");
            return;
        }
        String redisKey = REPUTATION_KEY_PREFIX + asn;
        String fieldToIncrement = isSuspicious ? FIELD_SUSPICIOUS : FIELD_LEGITIMATE;
        long nowMillis = Instant.now().toEpochMilli();

        try {
            // Atomically increment the relevant counter
            redisTemplate.opsForHash().increment(redisKey, fieldToIncrement, 1L);
            // Update the last updated timestamp
            redisTemplate.opsForHash().put(redisKey, FIELD_LAST_UPDATED, String.valueOf(nowMillis));
            // Set/Update the TTL for the entire hash key
            redisTemplate.expire(redisKey, reputationEntryTtl);

            log.trace("Recorded {} activity for ASN: {}", isSuspicious ? "suspicious" : "legitimate", asn);

        } catch (DataAccessException e) {
            log.error("Redis error recording activity for ASN {}: {}", asn, e.getMessage());
            // Decide how critical this is. Usually logging is sufficient.
        } catch (Exception e) {
            log.error("Unexpected error recording activity for ASN {}: {}", asn, e.getMessage(), e);
        }
    }

    @Override
    public double getReputationScore(String asn) {
        if (asn == null || asn.isBlank()) {
            log.warn("Attempted to get reputation score for null or blank ASN.");
            return DEFAULT_SCORE; // Return default for invalid input
        }
        String redisKey = REPUTATION_KEY_PREFIX + asn;

        try {
            // Get both counts in one call
            List<Object> values = redisTemplate.opsForHash().multiGet(redisKey, List.of(FIELD_SUSPICIOUS, FIELD_LEGITIMATE));

            Object suspiciousValue = values.get(0);
            Object legitimateValue = values.get(1);

            // If the key doesn't exist or fields are missing, multiGet returns nulls in the list
            if (suspiciousValue == null && legitimateValue == null) {
                log.trace("No reputation data found in Redis for ASN: {}. Returning default score.", asn);
                return DEFAULT_SCORE; // ASN not seen yet or expired
            }

            long suspiciousCount = parseLongOrDefault(suspiciousValue, 0L);
            long legitimateCount = parseLongOrDefault(legitimateValue, 0L);

            long total = suspiciousCount + legitimateCount;
            if (total == 0) {
                // This case should be rare if counts exist but are zero, but handle defensively
                return DEFAULT_SCORE;
            }

            double score = (double) legitimateCount / total;
            log.trace("Calculated reputation score for ASN {}: {} (L:{}, S:{})", asn, String.format("%.2f", score), legitimateCount, suspiciousCount);
            return score;

        } catch (DataAccessException e) {
            log.error("Redis error getting reputation score for ASN {}: {}", asn, e.getMessage());
            return DEFAULT_SCORE; // Return default score on Redis error
        } catch (Exception e) {
            log.error("Unexpected error getting reputation score for ASN {}: {}", asn, e.getMessage(), e);
            return DEFAULT_SCORE; // Return default score on unexpected error
        }
    }

    private long parseLongOrDefault(Object value, long defaultValue) {
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse reputation count '{}' to long, using default {}", s, defaultValue);
                return defaultValue;
            }
        }
        // Handle cases where Redis might return other types unexpectedly, or null
        return defaultValue;
    }


    /**
     * Scheduled task to clean up stale ASN entries.
     * NOTE: This implementation relies on Redis TTL expiration for cleanup.
     * If a more complex, gradual decay logic is needed, this method
     * would need to implement SCAN + HGET + HMSET logic, which is significantly
     * more complex and potentially resource-intensive.
     * Keeping this method as a placeholder or for potential future use
     * with the complex logic. For now, it does nothing as TTL handles cleanup.
     */
    @Scheduled(cron = "${asn.reputation.decay.schedule:0 1 4 * * *}") // Default: 4:01 AM daily
    public void applyReputationDecay() {
        // Option A: Rely on Redis TTL (set in recordActivity). This method does nothing.
        log.info("ASN reputation cleanup relies on Redis TTL ({}). Scheduled decay task is currently a no-op.", reputationEntryTtl);

        // Option B: Implement complex SCAN + Update logic if TTL is not sufficient
        /*
        log.info("Starting scheduled reputation decay process...");
        long decayThresholdMillis = Instant.now().minus(asnProperties.getStaleThreshold()).toEpochMilli(); // Or a different decay period
        String cursor = "0";
        int scanCount = 100; // How many keys to check per SCAN iteration
        long processedCount = 0;
        long decayedCount = 0;

        try {
            do {
                ScanResult<String> scanResult = redisTemplate.execute((RedisCallback<ScanResult<String>>) connection -> {
                    ScanOptions options = ScanOptions.scanOptions().match(REPUTATION_KEY_PREFIX + "*").count(scanCount).build();
                    Cursor<byte[]> cursorBytes = connection.keyCommands().scan(ScanCursor.of(cursor), options);
                    List<String> keys = new ArrayList<>();
                    while (cursorBytes.hasNext()) {
                        keys.add(new String(cursorBytes.next()));
                    }
                    return new ScanResult<>(cursorBytes.getCursorId() == 0 ? "0" : String.valueOf(cursorBytes.getCursorId()), keys);
                });

                if (scanResult != null) {
                    cursor = scanResult.getCursor();
                    List<String> keys = scanResult.getResult();
                    processedCount += keys.size();

                    for (String key : keys) {
                        try {
                            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                            Object lastUpdatedObj = entries.get(FIELD_LAST_UPDATED);

                            if (lastUpdatedObj instanceof String lastUpdatedStr) {
                                long lastUpdatedMillis = parseLongOrDefault(lastUpdatedStr, 0L);
                                if (lastUpdatedMillis > 0 && lastUpdatedMillis < decayThresholdMillis) {
                                    // Apply decay
                                    long suspicious = parseLongOrDefault(entries.get(FIELD_SUSPICIOUS), 0L);
                                    long legitimate = parseLongOrDefault(entries.get(FIELD_LEGITIMATE), 0L);

                                    long newSuspicious = (long) (suspicious * 0.9); // Example decay factor
                                    long newLegitimate = (long) (legitimate * 0.9);

                                    if (newSuspicious != suspicious || newLegitimate != legitimate) {
                                        Map<String, String> updates = new HashMap<>();
                                        updates.put(FIELD_SUSPICIOUS, String.valueOf(newSuspicious));
                                        updates.put(FIELD_LEGITIMATE, String.valueOf(newLegitimate));
                                        // Optionally update last_updated timestamp here too?
                                        redisTemplate.opsForHash().putAll(key, updates);
                                        decayedCount++;
                                        log.trace("Decayed reputation for key: {}", key);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing decay for key {}: {}", key, e.getMessage());
                        }
                    }
                }
            } while (!"0".equals(cursor));

            log.info("Finished scheduled reputation decay. Processed approximately {} keys, decayed {}.", processedCount, decayedCount);

        } catch (Exception e) {
            log.error("Error during scheduled reputation decay process", e);
        }
        */
    }
}

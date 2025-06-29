    /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

    package me.amlu.shop.amlume_shop.security.failedlogin;

    import me.amlu.shop.amlume_shop.exceptions.AccountLockedException;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.data.redis.core.StringRedisTemplate;
    import org.springframework.stereotype.Service;

    @Service
    public class FailedLoginAttemptService {

        private static final Logger log = LoggerFactory.getLogger(FailedLoginAttemptService.class);

        private final StringRedisTemplate redisTemplate;
        private final FailedLoginProperties properties;

        public FailedLoginAttemptService(StringRedisTemplate redisTemplate, FailedLoginProperties properties) {
            this.redisTemplate = redisTemplate;
            this.properties = properties;
        }

        private String getKey(String identifier) {
            // Identifier could be username, IP, or combination
            return properties.getRedisKeyPrefix() + identifier;
        }

        /**
         * Records a failed login attempt for the identifier.
         *
         * @param identifier Typically username or IP address.
         * @return The new count of failed attempts.
         */
        public long recordFailure(String identifier) {
            String key = getKey(identifier);
            try {
                // Increment the counter. If the key doesn't exist, it's created with value 1.
                Long currentAttempts = redisTemplate.opsForValue().increment(key);
                if (currentAttempts != null) {
                    // Set/update the expiration time only if increment was successful
                    redisTemplate.expire(key, properties.getCounterWindow());
                    log.debug("Recorded failed login attempt for '{}'. Count: {}", identifier, currentAttempts);
                    return currentAttempts;
                } else {
                    log.error("Failed to increment failed login counter for '{}'", identifier);
                    return -1; // Indicate error
                }
            } catch (Exception e) {
                log.error("Redis error recording failed login for '{}'", identifier, e);
                return -1; // Indicate error
            }
        }

        /**
         * Checks if the identifier has exceeded the maximum allowed failed attempts.
         *
         * @param identifier Typically username or IP address.
         * @return true if blocked, false otherwise.
         */
        public boolean isBlocked(String identifier) {
            String key = getKey(identifier);
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long attempts = Long.parseLong(value);
                    boolean blocked = attempts >= properties.getMaxAttempts();
                    if (blocked) {
                        log.warn("Identifier '{}' is blocked due to {} failed attempts.", identifier, attempts);
                    }
                    return blocked;
                }
                return false; // Not blocked if no record exists
            } catch (NumberFormatException e) {
                log.error("Invalid attempt count format in Redis for key '{}'", key, e);
                // Consider deleting the invalid key
                // redisTemplate.delete(key);
                return false; // Treat as not blocked, but log error
            } catch (Exception e) {
                log.error("Redis error checking block status for '{}'", identifier, e);
                return false; // Fail open (treat as not blocked) on Redis error? Or fail closed?
            }
        }

        /**
         * Resets the failed login attempt counter for the identifier.
         *
         * @param identifier Typically username or IP address.
         */
        public void resetAttempts(String identifier) {
            String key = getKey(identifier);
            try {
                redisTemplate.delete(key);
                log.debug("Reset failed login attempts for '{}'", identifier);
            } catch (Exception e) {
                log.error("Redis error resetting attempts for '{}'", identifier, e);
            }
        }

        /**
         * Checks if blocked and throws AccountLockedException if true.
         *
         * @param identifier Typically username or IP address.
         * @throws AccountLockedException if the account/identifier is blocked.
         */
        public void checkAndThrowIfBlocked(String identifier) throws AccountLockedException {
            if (isBlocked(identifier)) {
                // Optionally, retrieve the remaining lockout time if you store it separately
                throw new AccountLockedException("Account or IP is temporarily locked due to too many failed login attempts.");
            }
        }
    }

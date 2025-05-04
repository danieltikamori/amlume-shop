/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.cache_management.service;

import org.slf4j.Logger;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

// Removed import java.util.Collection;

@Component
public class ValkeyHealthIndicator implements HealthIndicator {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ValkeyHealthIndicator.class);
    private final RedisConnectionFactory connectionFactory;

    public ValkeyHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            if (checkValkeyConnection()) {
                return Health.up()
                        .withDetail("status", "Connected")
                        .withDetail("client", "Valkey (via Lettuce)")
                        .build();
            }
            return Health.down()
                    .withDetail("error", "Valkey connection failed")
                    .build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down(e).build();
        }
    }

    private boolean checkValkeyConnection() {
        // Use try-with-resources for the connection
        try (RedisConnection connection = connectionFactory.getConnection()) {
            // Perform a simple Valkey/Redis operation like PING
            String pingResponse = connection.ping();
            // Check if the response is "PONG" (case-insensitive)
            return "PONG".equalsIgnoreCase(pingResponse);
        } catch (Exception e) {
            log.error("Failed to connect to Valkey/Redis for health check", e);
            return false;
        }
    }
}

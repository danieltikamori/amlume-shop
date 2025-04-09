/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.Node;
import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValkeyHealthIndicator implements HealthIndicator {
    private final RedissonClient valkeyClient; // Will be injected with Valkey client

    @Override
    public Health health() {
        try {
            if (checkValkeyConnection()) {
                return Health.up()
                        .withDetail("status", "Connected")
                        .withDetail("client", "Valkey")
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
        try {
            // Perform a simple Valkey operation to check connection
            valkeyClient.getBucket("XXXXXXXXXXXX").isExists();
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to Valkey", e);
            return false;
        }
    }
}

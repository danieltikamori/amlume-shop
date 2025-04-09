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

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ValkeyHealthIndicatorTest {

    @Test
    void shouldReturnHealthyWhenValkeyIsUp() {
        // Arrange
        RedissonClient valkeyClient = mock(RedissonClient.class);
        RBucket<Object> bucket = mock(RBucket.class);

        when(valkeyClient.getBucket(anyString())).thenReturn(bucket);
        when(bucket.isExists()).thenReturn(false);

        ValkeyHealthIndicator healthIndicator = new ValkeyHealthIndicator(valkeyClient);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Connected", health.getDetails().get("status"));
        assertEquals("Valkey", health.getDetails().get("client"));
    }

    @Test
    void shouldReturnUnhealthyWhenValkeyIsDown() {
        // Arrange
        RedissonClient valkeyClient = mock(RedissonClient.class);
        when(valkeyClient.getBucket(anyString())).thenThrow(new RuntimeException("XXXXXXXXXXXXXXXXX"));

        ValkeyHealthIndicator healthIndicator = new ValkeyHealthIndicator(valkeyClient);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }

    @Test
    void shouldReturnUnhealthyWhenExceptionOccurs() {
        // Arrange
        RedissonClient valkeyClient = mock(RedissonClient.class);
        when(valkeyClient.getBucket(anyString())).thenThrow(new RuntimeException("XXXXXXXXXXXXXXXX"));

        ValkeyHealthIndicator healthIndicator = new ValkeyHealthIndicator(valkeyClient);

        // Act
        Health health = healthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }
}

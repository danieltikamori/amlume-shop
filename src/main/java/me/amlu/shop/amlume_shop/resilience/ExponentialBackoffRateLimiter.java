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

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.MaxRetriesExceeded;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.function.Supplier;

@Component
public class ExponentialBackoffRateLimiter {
    private static final int INITIAL_BACKOFF_MS = 1000; // 1 second
    private static final int MAX_BACKOFF_MS = 60000;    // 1 minute
    private static final int MAX_RETRIES = 5;
    private static final double MULTIPLIER = 2.0;
    private static final double JITTER_FACTOR = 0.1;    // 10% random jitter

    private final Random random = new Random();

    public <T> T executeWithBackoff(final Supplier<T> operation) throws InterruptedException {
        return executeWithRetries(operation, 0);
    }

    private <T> T executeWithRetries(final Supplier<T> operation, final int retryCount) throws InterruptedException {
        try {
            return operation.get();
        } catch (RequestNotPermitted e) {
            if (retryCount >= MAX_RETRIES - 1) {
                throw new MaxRetriesExceeded("Max retries exceeded" + e);
            }

            final long currentBackoff = calculateBackoffTime(retryCount);
            Thread.sleep(currentBackoff);

            return executeWithRetries(operation, retryCount + 1);
        }
    }

    private long calculateBackoffTime(final int retryCount) {
        final long baseBackoff = (long) (INITIAL_BACKOFF_MS * Math.pow(MULTIPLIER, retryCount));
        final long jitter = (long) (baseBackoff * JITTER_FACTOR * random.nextLong()); // nextDouble(?)
        return Math.min(baseBackoff + jitter, MAX_BACKOFF_MS);
    }

    // Void operation version
    public void executeWithBackoff(final Runnable operation) throws InterruptedException {
        executeWithBackoff(() -> {
            operation.run();
            return null;
        });
    }
}

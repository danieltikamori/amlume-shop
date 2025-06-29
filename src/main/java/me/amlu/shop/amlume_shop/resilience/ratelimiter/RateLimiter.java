/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.ratelimiter;

/**
 * Interface for generic rate limiting operations.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire a permit for the given key.
     *
     * @param key A unique key identifying the resource or client being limited (e.g., IP address, user ID, API endpoint).
     * @return true if the permit was acquired (request allowed), false otherwise (rate limit exceeded).
     */
    boolean tryAcquire(String key);

    /**
     * Attempts to acquire a specified number of permits for the given key.
     * (Optional, useful for token bucket, less common for sliding window check-only)
     *
     * @param key       The unique key.
     * @param numPermits The number of permits to acquire.
     * @return true if the permits were acquired, false otherwise.
     */
    // boolean tryAcquire(String key, int numPermits); // Add if needed

    /**
     * Gets the approximate remaining permits for the key within the current window.
     * Note: there may be an estimate depending on the algorithm.
     *
     * @param key The unique key.
     * @return Approximate remaining permits, or -1 if not easily determined.
     */
    long getRemainingPermits(String key);
}

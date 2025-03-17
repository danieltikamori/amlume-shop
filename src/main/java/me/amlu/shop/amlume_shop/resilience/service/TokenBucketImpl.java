/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Daniel Tikamori
 * @version 1.0
 * @since 2025-03-16
 * <p>
 * Implementation of the Token Bucket algorithm for rate limiting.
 * <p>
 * The Token Bucket algorithm is a simple, efficient way to limit the rate at which
 * a certain action can be performed. It works by maintaining a "bucket" of tokens,
 * where each token represents a single unit of the action. When the action is
 * performed, a token is removed from the bucket. If the bucket is empty, the action
 * is blocked until more tokens are added.
 * <p>
 * This implementation provides a flexible way to configure the token bucket's
 * parameters, such as the initial number of tokens, the rate at which tokens are
 * added, and the maximum size of the bucket.
 * <p>
 * Attributes:
 * capacity (int): The maximum number of tokens in the bucket.
 * refill_rate (int): The rate at which tokens are added to the bucket.
 * tokens (int): The current number of tokens in the bucket.
 * <p>
 * Methods:
 * consume(token_count): Removes the specified number of tokens from the bucket.
 * refill(): Adds tokens to the bucket at the specified refill rate.
 * is_empty(): Returns True if the bucket is empty, False otherwise.
 */

@Slf4j
@Service
public class TokenBucketImpl implements TokenBucket {
    private final int capacity;
    private final double refillRate;
    private final AtomicReference<Instant> lastRefillTime;
    private final AtomicInteger tokens;

    public TokenBucketImpl(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicInteger(capacity);
        this.lastRefillTime = new AtomicReference<>(Instant.now());
    }

    /**
     * Attempts to consume a specified number of tokens from the bucket.
     *
     * @param numTokens the number of tokens to consume
     * @return true if the specified number of tokens was successfully consumed, false if there were not enough tokens available
     */
    @Override
    public boolean tryConsume(int numTokens) {
        refill();

        while (true) {
            int currentTokens = tokens.get();
            if (currentTokens < numTokens) {
                return false;
            }

            if (tokens.compareAndSet(currentTokens, currentTokens - numTokens)) {
                return true;
            }
        }
    }

    /**
     * Refills the token bucket based on the elapsed time since the last refill.
     * This method is called automatically when tryConsume is called.
     * It is not necessary to call this method manually.
     * The method is thread-safe and can be called concurrently.
     */
    private void refill() {
        Instant now = Instant.now();
        Instant last = lastRefillTime.get();

        double elapsedSeconds = Duration.between(last, now).toNanos() / 1_000_000_000.0;
        int tokensToAdd = (int) (elapsedSeconds * refillRate);

        // Add tokens to the bucket
        if (tokensToAdd > 0 && lastRefillTime.compareAndSet(last, now)) {
            while (true) {
                int currentTokens = tokens.get();
                int newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    break;
                }
            }
        }

    }

    /**
     * Returns the number of currently available tokens in the bucket.
     * This method will refill the bucket if necessary, so it can be used
     * to poll the number of available tokens.
     *
     * @return the number of available tokens
     */
    @Override
    public int getAvailableTokens() {
        refill();
        return tokens.get();
    }
}

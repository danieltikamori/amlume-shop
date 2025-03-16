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


import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.resilience.service.TokenBucket;
import me.amlu.shop.amlume_shop.resilience.service.TokenBucketImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 1.0
 * <p>
 * Handling rate limiting or throttling for the ASN lookup service to prevent abuse and ensure fair usage.
 * A service that wraps an {@link AsnLookupServiceImpl} with a token bucket rate limiter.
 * This implementation uses the TokenBucket class from the AWS SDK to manage the rate limiting.
 * The rate limiting is configured with a maximum number of tokens (100) and a refill rate (10 tokens per second).
 * If the rate limit is exceeded, a RateLimitExceededException is thrown.
 * The delegate ASN lookup service is called only if the rate limit is not exceeded.
 * <p>
 * The TokenBucket implementation provides:
 * - Thread-safe operation using atomic operations
 * - Accurate token refill based on time elapsed
 * - Configurable capacity and refill rate
 * - No external dependencies
 * - Efficient token consumption
 */

@Slf4j
@Service
public class TokenBucketAsnLookupServiceImpl implements TokenBucketAsnLookupService {
    private final TokenBucket tokenBucket;
    private final AsnLookupService delegate;

    public TokenBucketAsnLookupServiceImpl(
            @Value("${asn.ratelimit.capacity:100}") int capacity,
            @Value("${asn.ratelimit.refill-rate:10}") double refillRate,
            AsnLookupService delegate) {
        this.tokenBucket = new TokenBucketImpl(capacity, refillRate);
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }
        return delegate.lookupAsn(ip);
    }
}

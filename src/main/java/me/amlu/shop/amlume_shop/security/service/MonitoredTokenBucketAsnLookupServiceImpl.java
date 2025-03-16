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

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.resilience.service.TokenBucket;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @see TokenBucketAsnLookupServiceImpl
 * @see AsnLookupService
 * @see TokenBucket
 * @see MeterRegistry
 * <p>
 * A monitored token bucket implementation of the {@link AsnLookupService} interface.
 * This provides metrics for:
 * - Available tokens
 * - Rate limit exceeded events
 * - Total requests
 * @since 2025-02-16
 */

@Slf4j
@Service
public class MonitoredTokenBucketAsnLookupServiceImpl implements AsnLookupService {
    private final TokenBucket tokenBucket;
    private final AsnLookupService delegate;
    private final MeterRegistry meterRegistry;

    public MonitoredTokenBucketAsnLookupServiceImpl(
            TokenBucket tokenBucket,
            AsnLookupService delegate,
            MeterRegistry meterRegistry) {
        this.tokenBucket = tokenBucket;
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;

        // Register gauges for monitoring
        meterRegistry.gauge("asn.lookup.tokens.available",
                tokenBucket, TokenBucket::getAvailableTokens);
    }

    @Override
    public String lookupAsn(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }

        meterRegistry.counter("asn.lookup.requests").increment();
        return delegate.lookupAsn(ip);
    }

    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }

        meterRegistry.counter("asn.lookup.requests").increment();
        return delegate.lookupAsnWithGeoIp2(ip);

    }

    @Override
    public String lookupAsnUncached(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }

        meterRegistry.counter("asn.lookup.requests").increment();
        return delegate.lookupAsnUncached(ip);
    }

    @Override
    public String lookupAsnViaDns(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }

        meterRegistry.counter("asn.lookup.requests").increment();
        return delegate.lookupAsnViaDns(ip);
    }

    @Override
    public String lookupAsnViaWhois(String ip) {
        if (!tokenBucket.tryConsume(1)) {
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }

        meterRegistry.counter("asn.lookup.requests").increment();
        return delegate.lookupAsnViaWhois(ip);
    }
}

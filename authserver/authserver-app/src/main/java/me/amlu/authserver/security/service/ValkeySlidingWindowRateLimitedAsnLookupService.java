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

import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.authserver.exceptions.RateLimitExceededException;
import me.amlu.authserver.resilience.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("valkeyRateLimitedAsnLookup")
public class ValkeySlidingWindowRateLimitedAsnLookupService implements AsnLookupService {

    private static final Logger log = LoggerFactory.getLogger(ValkeySlidingWindowRateLimitedAsnLookupService.class);

    private final RateLimiter rateLimiter;
    private final AsnLookupService delegate;
    private final MeterRegistry meterRegistry;

    // Constructor updated to inject the rate limiter bean
    public ValkeySlidingWindowRateLimitedAsnLookupService(
            @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter,
            @Qualifier("coreAsnLookup") AsnLookupService delegate, MeterRegistry meterRegistry) {
        this.rateLimiter = rateLimiter;
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String lookupAsn(String ip) {
        String rateLimitKey = "asnLookup:" + ip; // Construct the key
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            log.warn("Rate limit exceeded for ASN lookup for IP: {}", ip);
            meterRegistry.counter("asn.lookup.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("ASN lookup rate limit exceeded for IP: " + ip);
        }
        log.trace("Rate limit check passed for ASN lookup for IP: {}", ip);
        return delegate.lookupAsn(ip);
    }

    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        return delegate.lookupAsnWithGeoIp2(ip);
    }

    @Override
    public String lookupAsnUncached(String ip) {
        return delegate.lookupAsnUncached(ip);
    }

    @Override
    public String lookupAsnViaDns(String ip) {
        return delegate.lookupAsnViaDns(ip);
    }

    @Override
    public String lookupAsnViaWhois(String ip) {
        return delegate.lookupAsnViaWhois(ip);
    }
}

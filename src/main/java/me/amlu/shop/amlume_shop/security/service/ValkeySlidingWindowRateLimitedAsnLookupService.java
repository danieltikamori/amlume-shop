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

import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.resilience.service.SlidingWindowValkeyRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("valkeyRateLimitedAsnLookup")
public class ValkeySlidingWindowRateLimitedAsnLookupService implements AsnLookupService {

    private static final Logger log = LoggerFactory.getLogger(ValkeySlidingWindowRateLimitedAsnLookupService.class);

    private final SlidingWindowValkeyRateLimiter rateLimiter; // Inject the bean
    private final AsnLookupService delegate;

    // Constructor updated to inject the rate limiter bean
    public ValkeySlidingWindowRateLimitedAsnLookupService(
            SlidingWindowValkeyRateLimiter rateLimiter,
            @Qualifier("coreAsnLookup") AsnLookupService delegate) {
        this.rateLimiter = rateLimiter;
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        // Use the injected rate limiter instance
        if (!rateLimiter.tryAcquire(ip)) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            throw new RateLimitExceededException("ASN lookup rate limit exceeded for IP: " + ip); // Add IP to message
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

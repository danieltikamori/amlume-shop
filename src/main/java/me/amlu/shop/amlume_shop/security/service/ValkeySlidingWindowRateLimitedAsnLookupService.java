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
import me.amlu.shop.amlume_shop.resilience.service.SlidingWindowValkeyRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ValkeySlidingWindowRateLimitedAsnLookupService implements AsnLookupService {
    private final SlidingWindowValkeyRateLimiter rateLimiter;
    private final AsnLookupService delegate;

    public ValkeySlidingWindowRateLimitedAsnLookupService(
            @Value("${asn.ratelimit.window-seconds:60}") int windowSeconds,
            @Value("${asn.ratelimit.max-requests:100}") int maxRequests,
            AsnLookupService delegate) {
        this.rateLimiter = new SlidingWindowValkeyRateLimiter(windowSeconds, maxRequests);
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        if (!rateLimiter.tryAcquire(ip)) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }
        return delegate.lookupAsn(ip);
    }

    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        return "";
    }

    @Override
    public String lookupAsnUncached(String ip) {
        return "";
    }

    @Override
    public String lookupAsnViaDns(String ip) {
        return "";
    }

    @Override
    public String lookupAsnViaWhois(String ip) {
        return "";
    }
}

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

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @see AsnLookupServiceImpl
 * @see AsnSimpleCacheServiceImpl
 * <p>
 * A service that wraps the {@link AsnLookupServiceImpl} to add rate limiting.
 * This implementation uses the Guava RateLimiter to manage the rate limiting.
 * The rate limiting is configured with a maximum of 10 requests per second.
 * If the rate limit is exceeded, a RateLimitExceededException is thrown.
 * The delegate ASN lookup service is called only if the rate limit is not exceeded.
 * @since 2025-02-16
 */

@Slf4j
@Service
public class RateLimitedAsnLookupServiceImpl implements RateLimitedAsnLookupService {
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests per second
    private final AsnLookupService delegate;

    public RateLimitedAsnLookupServiceImpl(AsnLookupService delegate) {
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        if (!rateLimiter.tryAcquire()) {
            throw new RateLimitExceededException("ASN lookup rate limit exceeded");
        }
        return delegate.lookupAsn(ip);
    }
}

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

import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 2025-02-16
 * Implementing backoff and retry mechanism for ASN lookup service
 */
@Primary
@Service
@Qualifier("retryingAsnLookup")
public class RetryingAsnLookupServiceImpl implements AsnLookupService {

    private static final Logger log = LoggerFactory.getLogger(RetryingAsnLookupServiceImpl.class);

    private final AsnLookupService delegate;

    public RetryingAsnLookupServiceImpl(@Qualifier("valkeyRateLimitedAsnLookup") AsnLookupService delegate) {
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsn(ip)).get();
    }

    @Override
    public String lookupAsnWithGeoIp2(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsnWithGeoIp2(ip)).get();
    }

    @Override
    public String lookupAsnUncached(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsnUncached(ip)).get();
    }

    @Override
    public String lookupAsnViaDns(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsnViaDns(ip)).get();
    }

    @Override
    public String lookupAsnViaWhois(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsnViaWhois(ip)).get();
    }
}

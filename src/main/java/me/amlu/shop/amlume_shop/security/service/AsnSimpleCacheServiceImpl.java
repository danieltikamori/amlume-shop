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

import me.amlu.shop.amlume_shop.commons.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AsnSimpleCacheServiceImpl implements AsnSimpleCacheService {

    private static final Logger log = LoggerFactory.getLogger(AsnSimpleCacheServiceImpl.class);

    private final AsnLookupService lookupService;

    public AsnSimpleCacheServiceImpl(@Qualifier("retryingAsnLookup") AsnLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Override
    // Use Spring's @Cacheable with the cache name defined in Constants
    @Cacheable(value = Constants.ASN_CACHE, key = "#ip")
    public String getAsn(String ip) {
        log.debug("Cache miss for ASN lookup for IP: {}. Calling lookup service.", ip);
        try {
            // Directly call the lookup service. Spring handles caching.
            return lookupService.lookupAsn(ip);
        } catch (Exception e) {
            // Log the error from the underlying service
            log.error("ASN lookup failed for IP: {}", ip, e);
            // Depending on requirements, return null, empty string, or rethrow a specific exception
            return null; // Or "" or throw new AsnLookupFailedException(...)
        }
    }
}

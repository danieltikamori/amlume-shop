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

import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @since 2025-02-16
 * Implementing backoff and retry mechanism for ASN lookup service
 */
@Slf4j
@Service
public class RetryingAsnLookupServiceImpl implements RetryingAsnLookupService {
    private final AsnLookupService delegate;

    public RetryingAsnLookupServiceImpl(AsnLookupService delegate) {
        this.delegate = delegate;
    }

    @Override
    public String lookupAsn(String ip) {
        return Retry.decorateSupplier(Retry.ofDefaults("asnLookup"),
                () -> delegate.lookupAsn(ip)).get();
    }
}

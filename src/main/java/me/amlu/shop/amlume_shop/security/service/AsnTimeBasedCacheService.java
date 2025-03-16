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

import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public interface AsnTimeBasedCacheService {
    String getAsn(String ip);

    // Periodic backgroud refresh strategy to maintain cache consistency
    // Peridodically refresh the cache to ensure that the ASN information is up to date
    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    void refreshCache();

    @Data
    public static class CachedAsn {
        private final String asn;
        private final Instant timestamp;

        CachedAsn(String asn) {
            this.asn = asn;
            this.timestamp = Instant.now();
        }
    }
}

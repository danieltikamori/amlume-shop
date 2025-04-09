/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ValkeyRateLimiter {
    private final ValKeyClient valkey;
    private final int maxRequests;
    private final int windowSeconds;

    public ValkeyRateLimiter(
            @Value("${rate.limit.max-requests:100}") int maxRequests,
            @Value("${rate.limit.window-seconds:60}") int windowSeconds) {
        this.valkey = new ValKeyClient();
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean tryAcquire(String key) {
        String valkeyKey = "ratelimit:" + key;
        
        // Atomic increment and get operation
        long count = valkey.incr(valkeyKey);
        
        // Set expiry if this is the first request in the window
        if (count == 1) {
            valkey.expire(valkeyKey, windowSeconds);
        }
        
        return count <= maxRequests;
    }
}

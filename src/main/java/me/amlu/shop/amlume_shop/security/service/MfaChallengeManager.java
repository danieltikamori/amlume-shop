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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.user_management.User;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static me.amlu.shop.amlume_shop.security.service.SecureIdGenerator.generateSecureId;

public class MfaChallengeManager {
    private final Cache<String, MfaChallenge> challengeCache;
    private static final int CHALLENGE_TIMEOUT_MINUTES = 5;

    public MfaChallengeManager() {
        challengeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CHALLENGE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    public String generateChallenge(User user) {
        String challengeId = generateSecureId();
        MfaChallenge challenge = new MfaChallenge(
            user.getUsername(),
            LocalDateTime.now(),
            challengeId
        );
        challengeCache.put(challengeId, challenge);
        return challengeId;
    }

    public boolean validateChallenge(String challengeId) {
        MfaChallenge challenge = challengeCache.getIfPresent(challengeId);
        if (challenge == null) {
            throw new MfaException(MfaException.MfaErrorType.CHALLENGE_FAILED,"Challenge expired or invalid");
        }
        // Remove challenge after use to prevent replay attacks
        challengeCache.invalidate(challengeId);
        return true;
    }
}

@Data
@AllArgsConstructor
class MfaChallenge {
    private String username;
    private LocalDateTime creationTime;
    private String challengeId;
}

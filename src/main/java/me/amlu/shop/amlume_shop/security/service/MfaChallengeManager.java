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
import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.user_management.User;

import java.time.LocalDateTime;
import java.util.Objects;
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

class MfaChallenge {
    private String username;
    private LocalDateTime creationTime;
    private String challengeId;

    public MfaChallenge(String username, LocalDateTime creationTime, String challengeId) {
        this.username = username;
        this.creationTime = creationTime;
        this.challengeId = challengeId;
    }

    public String getUsername() {
        return this.username;
    }

    public LocalDateTime getCreationTime() {
        return this.creationTime;
    }

    public String getChallengeId() {
        return this.challengeId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MfaChallenge other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (!Objects.equals(this$username, other$username)) return false;
        final Object this$creationTime = this.getCreationTime();
        final Object other$creationTime = other.getCreationTime();
        if (!Objects.equals(this$creationTime, other$creationTime))
            return false;
        final Object this$challengeId = this.getChallengeId();
        final Object other$challengeId = other.getChallengeId();
        return Objects.equals(this$challengeId, other$challengeId);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MfaChallenge;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $creationTime = this.getCreationTime();
        result = result * PRIME + ($creationTime == null ? 43 : $creationTime.hashCode());
        final Object $challengeId = this.getChallengeId();
        result = result * PRIME + ($challengeId == null ? 43 : $challengeId.hashCode());
        return result;
    }

    public String toString() {
        return "MfaChallenge(username=" + this.getUsername() + ", creationTime=" + this.getCreationTime() + ", challengeId=" + this.getChallengeId() + ")";
    }
}

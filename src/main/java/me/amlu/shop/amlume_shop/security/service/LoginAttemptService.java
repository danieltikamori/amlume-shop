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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.IsUserBlockedCheckStatusException;
import me.amlu.shop.amlume_shop.exceptions.LoginAttemptsCacheException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {
    private final LoadingCache<String, Integer> attemptsCache;
    private LoadingCache<String, LocalDateTime> lastAttemptCache;

    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public Integer load(@NotNull String key) {
                        return 0;
                    }
                });


        lastAttemptCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public LocalDateTime load(@NotNull String key) {
                        return LocalDateTime.now();
                    }
                });
    }

    public void waitIfRequired(String username) throws TooManyAttemptsException {
        int attempts = attemptsCache.getUnchecked(username);
        if (attempts > 0) {
            LocalDateTime lastAttempt = lastAttemptCache.getUnchecked(username);
            int waitSeconds = (int) Math.pow(2, attempts - 1.0);
            LocalDateTime nextAllowedAttempt = lastAttempt.plusSeconds(waitSeconds);

            if (LocalDateTime.now().isBefore(nextAllowedAttempt)) {
                throw new TooManyAttemptsException(
                        "Please wait " + waitSeconds + " seconds before trying again"
                );
            }
        }
    }

    public void loginFailed(String username) {
        try {
            int attempts = attemptsCache.get(username) + 1;
            attemptsCache.put(username, attempts);
        } catch (ExecutionException e) {
            throw new LoginAttemptsCacheException("Error accessing login attempts cache", e);
        }
    }

    public boolean isBlocked(String username) {
        try {
            return attemptsCache.get(username) >= Constants.MAX_FAILED_ATTEMPTS;
        } catch (ExecutionException e) {
            throw new IsUserBlockedCheckStatusException("Error checking blocked status", e);
        }
    }
}
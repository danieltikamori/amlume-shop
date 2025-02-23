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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.KeyManagementException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KeyManagementFacade {
    private final HCPSecretsService hcpSecretsService;
    private final Cache<String, String> keyCache;

    public record KeyPair(String privateKey, String publicKey) {
    }

    // Maximum number of keys to cache
    private static final int MAX_CACHE_SIZE = 100;
    // Cache duration
    private static final int CACHE_DURATION_HOURS = 1;

    public KeyManagementFacade(
            HCPSecretsService hcpSecretsService) {
        this.hcpSecretsService = hcpSecretsService;
        this.keyCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_DURATION_HOURS, TimeUnit.HOURS)
                .maximumSize(MAX_CACHE_SIZE)
                .recordStats()
                .build();
    }

    @PostConstruct
    public void initialize() {
        loadSecrets();
    }

    private void loadSecrets() {
        try {
            Map<String, String> secrets = hcpSecretsService.getSecrets();
            updateKeys(secrets);
        } catch (Exception e) {
            log.error("Failed to initialize keys from HCP", e);
            throw new KeyManagementException("Key initialization failed", e);
        }
    }

    public KeyPair getAsymmetricKeys(String purpose) {
        try {
            String privateKey = getCachedKey("private_" + purpose);
            String publicKey = getCachedKey("public_" + purpose);
            return new KeyPair(privateKey, publicKey);
        } catch (Exception e) {
            log.error("Failed to retrieve asymmetric keys for purpose: {}", purpose);
            throw new KeyManagementException("Key retrieval failed", e);
        }
    }

    public String getSymmetricKey(String purpose) {
        try {
            return getCachedKey("symmetric_" + purpose);
        } catch (Exception e) {
            log.error("Failed to retrieve symmetric key for purpose: {}", purpose);
            throw new KeyManagementException("Key retrieval failed", e);
        }
    }

    private String getCachedKey(String keyIdentifier) throws ExecutionException {
        return keyCache.get(keyIdentifier, () -> {
            Map<String, String> secrets = hcpSecretsService.getSecrets();
            return secrets.get(keyIdentifier);
        });
    }

    private void updateKeys(Map<String, String> secrets) {
        secrets.forEach((key, value) -> keyCache.put(key, value));
    }

    @Scheduled(fixedRate = 3600000) // Refresh every hour
    public void refreshKeys() {
        log.info("Starting scheduled key refresh");
        loadSecrets();
    }

}

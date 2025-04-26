/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/**
 * Usage Example:
 *
 * @Service public class YourService {
 * private final HCPSecretsService secretsService;
 * <p>
 * public YourService(HCPSecretsService secretsService) {
 * this.secretsService = secretsService;
 * }
 * <p>
 * public void someMethod() {
 * Map<String, String> secrets = secretsService.getSecrets();
 * // Use your secrets here
 * String apiKey = secrets.get("API_KEY");
 * }
 * }
 */

package me.amlu.shop.amlume_shop.security.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.FetchSecretsException;
import me.amlu.shop.amlume_shop.payload.GetSecretsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.*;

@Service
@Slf4j
public class HCPSecretsService {
    private final RestTemplate restTemplate;
    private final HCPTokenService tokenService;
    private final String organizationId;
    private final String projectId;
    private final String appName;
    private final RetryTemplate retryTemplate;
    private final Cache<String, Map<String, String>> secretsCache;

    private static final int CACHE_DURATION_MINUTES = 30;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_INTERVAL = 1000;
    private static final int MAX_RETRY_INTERVAL = 5000;

    public HCPSecretsService(
            RestTemplate restTemplate,
            HCPTokenService tokenService,
            @Value("${hcp.organization-id}") String organizationId,
            @Value("${hcp.project-id}") String projectId,
            @Value("${hcp.app-name}") String appName) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.appName = appName;
        this.retryTemplate = createRetryTemplate();
        this.secretsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    private RetryTemplate createRetryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_RETRY_INTERVAL);
        backOffPolicy.setMaxInterval(MAX_RETRY_INTERVAL);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    public static final String PASETO_ACCESS_PRIVATE_KEY = "PASETO_ACCESS_PRIVATE_KEY";
    public static final String PASETO_ACCESS_PUBLIC_KEY = "PASETO_ACCESS_PUBLIC_KEY";
    public static final String PASETO_ACCESS_SECRET_KEY = "PASETO_ACCESS_SECRET_KEY";
    public static final String PASETO_REFRESH_SECRET_KEY = "PASETO_REFRESH_SECRET_KEY";

    // To be used with the constants above
    public String getSecret(String key) {
        Map<String, String> secrets = getSecrets();
        String secret = secrets.get(key);
        if (secret == null) {
            throw new FetchSecretsException("Secret not found: " + key);
        }
        return secret;
    }

    public Map<String, String> getSecrets() {
        try {
            return secretsCache.get("secrets", this::fetchSecrets);
        } catch (ExecutionException e) {
            log.error("Failed to get secrets from cache", e);
            throw new FetchSecretsException("Failed to fetch secrets", e);
        }
    }

    private Map<String, String> fetchSecrets() {
        return retryTemplate.execute(context -> {
            try {
                String url = buildSecretsUrl();
                HttpHeaders headers = createHeaders();

                log.debug("Fetching secrets from HCP");
                ResponseEntity<GetSecretsResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        GetSecretsResponse.class
                );

//                if (response.getBody() != null) {
//                    log.debug("Successfully fetched secrets from HCP");
//                    return response.getBody().getSecrets();

                // For debugging purposes, log the keys retrieved from HCP
                if (response.getBody() != null) {
                    Map<String, String> secrets = response.getBody().getSecrets();
                    log.debug("Retrieved keys from HCP: {}", secrets.keySet());
                    return secrets;
                } else {
                    log.warn("Received empty response from HCP");
                    return Collections.emptyMap();
                }

            } catch (HttpClientErrorException e) {
                handleClientError(e);
                throw e; // Will be caught by retry template
            } catch (HttpServerErrorException e) {
                handleServerError(e);
                throw e; // Will be caught by retry template
            } catch (RestClientException e) {
                log.error("Failed to fetch secrets from HCP", e);
                throw new FetchSecretsException("Failed to fetch secrets", e);
            }
        });
    }

    private String buildSecretsUrl() {
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets:open",
                organizationId, projectId, appName
        );
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        return headers;
    }

    public String getPasetoAccessPrivateKey() {
        return getSecret(PASETO_ACCESS_PRIVATE_KEY);
    }

    public String getPasetoAccessPublicKey() {
        return getSecret(PASETO_ACCESS_PUBLIC_KEY);
    }

    public String getPasetoAccessSecretKey() {
        return getSecret(PASETO_ACCESS_SECRET_KEY);
    }

    public String getPasetoRefreshSecretKey() {
        return getSecret(PASETO_REFRESH_SECRET_KEY);
    }

    private void handleClientError(HttpClientErrorException e) {
        log.error("Client error while fetching secrets: {}", e.getStatusCode(), e);
        switch (e.getStatusCode()) {
            case UNAUTHORIZED:
                throw new FetchSecretsException("Unauthorized to fetch secrets", e);
            case FORBIDDEN:
                throw new FetchSecretsException("Forbidden to fetch secrets", e);
            case NOT_FOUND:
                throw new FetchSecretsException("Secrets not found", e);
            default:
                throw new FetchSecretsException("Client error while fetching secrets", e);
        }
    }

    private void handleServerError(HttpServerErrorException e) {
        log.error("Server error while fetching secrets: {}", e.getStatusCode(), e);
        throw new FetchSecretsException("Server error while fetching secrets", e);
    }

    @Scheduled(fixedRateString = "${hcp.secrets.refresh-interval:1800000}") // Default 30 minutes
    public void refreshSecrets() {
        try {
            log.debug("Starting scheduled secrets refresh");
            secretsCache.invalidate("secrets");
            getSecrets();
            log.debug("Completed scheduled secrets refresh");
        } catch (Exception e) {
            log.error("Failed to refresh secrets", e);
        }
    }

}

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

import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.FetchSecretsException;
import me.amlu.shop.amlume_shop.security.dto.GetAppSecretResponse;
import me.amlu.shop.amlume_shop.security.dto.GetSecretsResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.TimeUnit;

import static me.amlu.shop.amlume_shop.security.dto.GetSecretsResponse.SecretDetail;
import static org.springframework.http.HttpStatus.*;

/**
 * IMPORTANT: NOT COMPLETELY FUNCTIONAL
 * IT DOES WORK FOR THE FIRST PAGE OF SECRETS. WE CANNOT GET OTHER PAGES SECRETS.
 * TODO: SOLVE PAGINATION ISSUE, LIKELY PROVIDER SIDE
 *
 * IMPORTANT 2: DO NOT USE CACHING AS IT IS NOT SECURE. THIS WAY WE DO NOT INCREASE ATTACK SURFACE.
 *
 * This service handles fetching secrets from HCP (HashiCorp Cloud Platform) using the HCP API.
 * It manages token generation, caching, and pagination for retrieving secrets.
 * <p>
 * The service is designed to be used in a Spring application and is not intended for local or test profiles.
 */

@Profile({"!local", "!prod", "!docker", "!kubernetes"}) // Only active in profiles other than "local" and others listed
//@Profile({"!local","!test"}) // Only active in profiles other than "local" and "test"
@Service
//@CacheConfig(cacheNames = Constants.HCP_SECRETS_CACHE) // Set default cache name for this class
public class HCPSecretsService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HCPSecretsService.class);
    // Removed CACHE_DURATION_MINUTES as TTL is now managed by ValkeyCacheConfig
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_INTERVAL = 1000;
    private static final int MAX_RETRY_INTERVAL = 5000;

    private final RestTemplate restTemplate;
    private final HCPTokenService tokenService;
    private final String organizationId;
    private final String projectId;
    private final String appName;
    private final RetryTemplate retryTemplate;
    private final CacheManager cacheManager; // Inject Spring CacheManager


    public HCPSecretsService(
            RestTemplate restTemplate,
            HCPTokenService tokenService,
            @Value("${hcp.organization-id}") String organizationId,
            @Value("${hcp.project-id}") String projectId,
            @Value("${hcp.app-name}") String appName,
            CacheManager cacheManager) { // Inject CacheManager
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.appName = appName;
        this.cacheManager = cacheManager; // Assign CacheManager
        this.retryTemplate = createRetryTemplate();
    }

    private RetryTemplate createRetryTemplate() {
        // ... (retry template creation remains the same) ...
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

    // --- Constants for commonly used keys ---
    public static final String PASETO_ACCESS_PRIVATE_KEY = "PASETO_ACCESS_PRIVATE_KEY";
    public static final String PASETO_ACCESS_PUBLIC_KEY = "PASETO_ACCESS_PUBLIC_KEY";
    public static final String PASETO_ACCESS_SECRET_KEY = "PASETO_ACCESS_SECRET_KEY";
    public static final String PASETO_REFRESH_SECRET_KEY = "PASETO_REFRESH_SECRET_KEY";
    public static final String MFA_ENCRYPTION_PASSWORD = "MFA_ENCRYPTION_PASSWORD";
    public static final String MFA_ENCRYPTION_SALT = "MFA_ENCRYPTION_SALT";

    /**
     * Retrieves a specific secret value by its key using Spring Cache.
     * If the secret is not in the cache (Constants.HCP_SECRETS_CACHE),
     * this method's body will be executed to fetch it directly from HCP.
     * The result (or exception) will be cached.
     *
     * @param key The name of the secret to retrieve.
     * @return The secret value.
     * @throws FetchSecretsException if the secret cannot be found or fetched after retries.
     */
//    @Cacheable(key = "#key", unless = "#result == null") // Cache based on key, don't cache null results
    public String getSecret(String key) throws FetchSecretsException {
        log.info("Cache miss for secret '{}'. Fetching directly from HCP.", key); // Log cache miss
        String url = buildSingleSecretUrl(key);
        HttpHeaders headers = createHeaders();

        // Use retryTemplate for resilience on single fetch
        return retryTemplate.execute(context -> {
            log.debug("Attempting to fetch single secret '{}' (Retry attempt {})", key, context.getRetryCount() + 1);
            try {
                ResponseEntity<GetAppSecretResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), GetAppSecretResponse.class
                );

                // Extract value based on GetAppSecretResponse structure
                if (response.getBody() != null && response.getBody().secret() != null &&
                        response.getBody().secret().staticVersion() != null &&
                        response.getBody().secret().staticVersion().value() != null)
                {
                    String value = response.getBody().secret().staticVersion().value();
                    log.info("Successfully fetched single secret '{}' from HCP.", key);
                    return value; // This value will be cached by Spring
                } else {
                    log.error("Received unexpected response structure when fetching single secret '{}'. Body or nested fields null.", key);
                    // Throwing exception prevents caching null/bad response
                    throw new FetchSecretsException("Unexpected response structure for secret: " + key);
                }

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.error("Secret '{}' not found in HCP (404).", key);
                    // Throw specific exception - Spring Cache might cache this exception depending on config
                    throw new FetchSecretsException("Secret not found: " + key, e);
                } else {
                    log.error("Client error fetching single secret '{}': {}", key, e.getStatusCode(), e);
                    handleClientError(e); // This might wrap and throw FetchSecretsException
                    throw e; // Rethrow for retryTemplate
                }
            } catch (HttpServerErrorException e) {
                log.error("Server error fetching single secret '{}': {}", key, e.getStatusCode(), e);
                handleServerError(e); // This throws FetchSecretsException
                throw e; // Rethrow for retryTemplate
            } catch (RestClientException e) {
                log.error("RestClientException fetching single secret '{}': {}", key, e.getMessage(), e);
                throw new FetchSecretsException("Failed to fetch single secret '" + key + "' from HCP", e);
            }
        });
    }

    // --- REMOVED private fetchSingleSecret method - logic moved into getSecret ---

    /**
     * Fetches all secrets from HCP via pagination and populates the Spring Cache.
     * Intended for scheduled refresh.
     *
     * @throws FetchSecretsException if fetching fails after retries, or if pagination gets stuck.
     */
    private void fetchSecretsWithPagination() throws FetchSecretsException {
        // Get the target cache instance
        Cache cache = cacheManager.getCache(Constants.HCP_SECRETS_CACHE);
        if (cache == null) {
            log.error("Could not find cache named '{}'. Cannot populate secrets.", Constants.HCP_SECRETS_CACHE);
            throw new FetchSecretsException("Cache '" + Constants.HCP_SECRETS_CACHE + "' not configured.");
        }

        retryTemplate.execute(context -> {
            String nextPageToken = null;
            int pageNum = 1;
            int totalSecretsProcessed = 0;
            log.info("Starting full secrets fetch to populate cache (Retry attempt {})", context.getRetryCount() + 1);

            String previousPageToken = null;
            int stuckTokenCounter = 0;
            final int MAX_STUCK_ATTEMPTS = 3;

            do {
                final int currentPageNum = pageNum;
                String currentRequestToken = nextPageToken;
                String url = buildPaginatedListUrl(currentRequestToken);
                HttpHeaders headers = createHeaders();

                log.debug("Fetching secrets page {} from HCP. URL: {}", currentPageNum, url);
                String tokenUsed = headers.getFirst(HttpHeaders.AUTHORIZATION);
                log.debug("Authorization header being sent: {}", (tokenUsed != null ? tokenUsed.substring(0, Math.min(tokenUsed.length(), 15)) + "..." : "null"));

                try {
                    ResponseEntity<GetSecretsResponse> response = restTemplate.exchange(
                            url, HttpMethod.GET, new HttpEntity<>(headers), GetSecretsResponse.class
                    );

                    if (response.getBody() != null && response.getBody().secrets() != null) {
                        int secretsOnPage = 0;
                        for (SecretDetail detail : response.getBody().secrets()) {
                            if (detail != null && detail.name() != null && detail.staticVersion() != null && detail.staticVersion().value() != null) {
                                // --- Use Spring Cache API to put entries ---
                                cache.put(detail.name(), detail.staticVersion().value());
                                // --- End Change ---
                                secretsOnPage++;
                            } else {
                                log.warn("Skipping invalid secret entry on page {}: {}", currentPageNum, detail);
                            }
                        }
                        totalSecretsProcessed += secretsOnPage;
                        log.debug("Processed and cached {} secrets from page {}. Total secrets processed in this run: {}", secretsOnPage, currentPageNum, totalSecretsProcessed);

                        // --- Pagination Logic ---
                        if (response.getBody().pagination() != null) {
                            String receivedToken = response.getBody().pagination().nextPageToken();
                            log.debug("Page {}: Received next page token: '{}'", currentPageNum, receivedToken);

                            if (receivedToken != null && receivedToken.equals(currentRequestToken)) {
                                stuckTokenCounter++;
                                log.warn("Received the same page token ('{}') again. Stuck count: {}", receivedToken, stuckTokenCounter);
                                if (stuckTokenCounter >= MAX_STUCK_ATTEMPTS) {
                                    log.error("Page token appears stuck after {} attempts. Aborting pagination.", stuckTokenCounter);
                                    throw new FetchSecretsException("Pagination failed: Page token stuck after page " + currentPageNum);
                                }
                            } else {
                                stuckTokenCounter = 0; // Reset counter if token changes
                            }
                            nextPageToken = receivedToken;
                            if (nextPageToken != null && nextPageToken.isBlank()) {
                                nextPageToken = null; // Treat blank token as end of pagination
                            }
                        } else {
                            log.debug("Page {}: No pagination object found in response. Assuming end of list.", currentPageNum);
                            nextPageToken = null;
                            stuckTokenCounter = 0;
                        }
                        // --- End Pagination Logic ---

                    } else {
                        log.warn("Received null body or null secrets list from HCP on page {}. Stopping pagination.", currentPageNum);
                        nextPageToken = null;
                        stuckTokenCounter = 0;
                    }

                } catch (HttpClientErrorException.TooManyRequests e429) {
                    // --- 429 Handling ---
                    log.error("Client error fetching page {} from HCP: {}", currentPageNum, e429.getStatusCode(), e429);
                    long delaySeconds = 120;
                    try {
                        String responseBody = e429.getResponseBodyAsString();
                        if (responseBody != null && responseBody.contains("try again in ")) {
                            String secondsStr = responseBody.substring(responseBody.indexOf("try again in ") + "try again in ".length()).split(" ")[0];
                            secondsStr = secondsStr.replaceAll("[^\\d]", "");
                            if (!secondsStr.isEmpty()) {
                                delaySeconds = Long.parseLong(secondsStr);
                                delaySeconds = Math.max(1, Math.min(delaySeconds + 2, 300));
                            }
                        }
                    } catch (Exception parseEx) {
                        log.warn("Could not parse delay from 429 message, defaulting to {} seconds.", delaySeconds, parseEx);
                    }
                    log.warn("Rate limit hit on page {}. Waiting for {} seconds before allowing retry...", currentPageNum, delaySeconds);
                    try {
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for rate limit cooldown on page {}", currentPageNum);
                        throw new FetchSecretsException("Interrupted while waiting for rate limit cooldown", ie);
                    }
                    handleClientError(e429);
                    throw e429;
                    // --- End 429 Handling ---
                } catch (HttpClientErrorException e) {
                    log.error("Client error fetching page {} from HCP: {}", currentPageNum, e.getStatusCode(), e);
                    handleClientError(e);
                    throw e;
                } catch (HttpServerErrorException e) {
                    log.error("Server error fetching page {} from HCP: {}", currentPageNum, e.getStatusCode(), e);
                    handleServerError(e);
                    throw e;
                } catch (RestClientException e) {
                    log.error("RestClientException fetching page {} from HCP: {}", currentPageNum, e.getMessage(), e);
                    throw new FetchSecretsException("Failed to fetch secrets from HCP on page " + currentPageNum, e);
                }

                pageNum++;
                previousPageToken = currentRequestToken;

            } while (nextPageToken != null);

            log.info("Finished fetching all secrets via pagination. Total secrets processed and cached in this run: {}", totalSecretsProcessed);
            return null; // Return type of execute is Void
        });
    }

    // --- URL Builders ---
    private String buildBaseListUrl() {
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets:open",
                organizationId, projectId, appName
        );
    }

    private String buildPaginatedListUrl(String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(buildBaseListUrl());
        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken);
        }
        return builder.toUriString();
    }

    private String buildSingleSecretUrl(String secretName) {
        // VERIFY THIS PATH with HCP Docs for fetching a single secret by name
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets/%s",
                organizationId, projectId, appName, secretName
        );
    }

    // --- createHeaders ---
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        return headers;
    }

    // --- Convenience getters (remain the same, they now use the @Cacheable getSecret) ---
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
    public String getMfaEncryptionPassword() {
        return getSecret(MFA_ENCRYPTION_PASSWORD);
    }
    public String getMfaEncryptionSalt() {
        return getSecret(MFA_ENCRYPTION_SALT);
    }

    // --- Error Handling ---
    private void handleClientError(HttpClientErrorException e) {
        switch (e.getStatusCode()) {
            case UNAUTHORIZED: throw new FetchSecretsException("Unauthorized to fetch secrets", e);
            case FORBIDDEN: throw new FetchSecretsException("Forbidden to fetch secrets", e);
            case NOT_FOUND: throw new FetchSecretsException("Secrets not found (or invalid path/app/secret name)", e);
            default: throw new FetchSecretsException("Client error (" + e.getStatusCode() + ") while fetching secrets", e);
        }
    }
    private void handleServerError(HttpServerErrorException e) {
        throw new FetchSecretsException("Server error (" + e.getStatusCode() + ") while fetching secrets", e);
    }

    // AVOID CACHING TO NOT INCREASE ATTACK SURFACE
//    /**
//     * Scheduled task to refresh all secrets by invalidating the cache
//     * and triggering a full fetch via pagination.
//     * Uses @CacheEvict to clear the cache before execution.
//     */
//    @Scheduled(fixedRateString = "${hcp.secrets.refresh-interval:1800000}") // Default 30 minutes
////    @CacheEvict(allEntries = true, beforeInvocation = true) // Evict all entries in HCP_SECRETS_CACHE before method runs
//    public void refreshSecrets() {
//        try {
//            log.info("Starting scheduled secrets refresh (cache evicted)...");
//            // Trigger reload by calling the pagination fetch method directly
//            // The @CacheEvict annotation handles the cache clearing.
//            fetchSecretsWithPagination();
//            log.info("Completed scheduled secrets refresh. Cache repopulated.");
//        } catch (Exception e) {
//            log.error("Scheduled secrets refresh failed after cache eviction.", e);
//            // Allow scheduler to continue
//        }
//    }
}
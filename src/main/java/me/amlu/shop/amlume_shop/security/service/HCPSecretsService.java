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
import me.amlu.shop.amlume_shop.exceptions.FetchSecretsException;
import me.amlu.shop.amlume_shop.payload.GetAppSecretResponse;
import me.amlu.shop.amlume_shop.payload.GetSecretsResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static me.amlu.shop.amlume_shop.payload.GetSecretsResponse.SecretDetail;
import static org.springframework.http.HttpStatus.*;

/**
 * IMPORTANT: NOT COMPLETELY FUNCTIONAL
 * IT DOES WORK FOR THE FIRST PAGE OF SECRETS. WE CANNOT GET OTHER PAGES SECRETS.
 * TODO: SOLVE PAGINATION ISSUE, LIKELY PROVIDER SIDE
 * This service handles fetching secrets from HCP (HashiCorp Cloud Platform) using the HCP API.
 * It manages token generation, caching, and pagination for retrieving secrets.
 * <p>
 * The service is designed to be used in a Spring application and is not intended for local or test profiles.
 */

@Profile({"!local", "!prod", "!docker", "!kubernetes"}) // Only active in profiles other than "local" and others listed
//@Profile({"!local","!test"}) // Only active in profiles other than "local" and "test"


@Service
public class HCPSecretsService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HCPSecretsService.class);
    private static final int CACHE_DURATION_MINUTES = 30;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_INTERVAL = 1000;
    private static final int MAX_RETRY_INTERVAL = 5000;

    private final RestTemplate restTemplate;
    private final HCPTokenService tokenService;
    private final String organizationId;
    private final String projectId;
    private final String appName;
    private final RetryTemplate retryTemplate;
    // --- Cache Change: Key is secret name, Value is secret value ---
    private final Cache<String, String> secretsCache;
    // --- End Cache Change ---

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
        // --- Cache Change: Build Cache<String, String> ---
        this.secretsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(1000) // Optional: Add a size limit
                .build();
        // --- End Cache Change ---
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

    // --- Constants for commonly used keys ---
    public static final String PASETO_ACCESS_PRIVATE_KEY = "PASETO_ACCESS_PRIVATE_KEY";
    public static final String PASETO_ACCESS_PUBLIC_KEY = "PASETO_ACCESS_PUBLIC_KEY";
    public static final String PASETO_ACCESS_SECRET_KEY = "PASETO_ACCESS_SECRET_KEY";
    public static final String PASETO_REFRESH_SECRET_KEY = "PASETO_REFRESH_SECRET_KEY";
    // Add other constants like MFA_ENCRYPTION_PASSWORD etc. if fetched from HCP
    public static final String MFA_ENCRYPTION_PASSWORD = "MFA_ENCRYPTION_PASSWORD";
    public static final String MFA_ENCRYPTION_SALT = "MFA_ENCRYPTION_SALT";


    /**
     * Retrieves a specific secret value by its key.
     * Attempts to get the secret from the cache. If not found,
     * it triggers fetching the single secret from HCP via fetchSingleSecret.
     *
     * @param key The name of the secret to retrieve.
     * @return The secret value.
     * @throws FetchSecretsException if the secret cannot be found or fetched.
     */
    public String getSecret(String key) {
        try {
            // Use cache's get method with a Callable loader
            return secretsCache.get(key, () -> fetchSingleSecret(key));
        } catch (ExecutionException e) {
            log.error("Failed to get secret '{}' from cache loader. Cause: {}.", key, e.getCause() != null ? e.getCause() : e);
            Throwable cause = e.getCause();
            // Handle specific exceptions thrown by fetchSingleSecret
            switch (cause) {
                case FetchSecretsException fetchSecretsException -> throw fetchSecretsException;
                case HttpClientErrorException.NotFound notFound ->
                    // Specifically handle 404 from fetchSingleSecret
                        throw new FetchSecretsException("Secret not found: " + key, cause);
                case RuntimeException runtimeException -> throw runtimeException;
                case null, default ->
                        throw new FetchSecretsException("Failed to fetch secret '" + key + "' due to cache execution error", e);
            }
        }
    }

    /**
     * Fetches a single secret directly from HCP by its name.
     * This method is intended to be called by the cache loader in getSecret.
     *
     * @param secretName The name of the secret to fetch.
     * @return The secret value.
     * @throws FetchSecretsException if the secret cannot be fetched or is not found (404).
     */
    private String fetchSingleSecret(String secretName) throws FetchSecretsException {
        log.info("Cache miss for secret '{}'. Fetching directly from HCP.", secretName);
        String url = buildSingleSecretUrl(secretName);
        HttpHeaders headers = createHeaders();

        // Use retryTemplate for resilience on single fetch as well
        return retryTemplate.execute(context -> {
            log.debug("Attempting to fetch single secret '{}' (Retry attempt {})", secretName, context.getRetryCount() + 1);
            try {
                ResponseEntity<GetAppSecretResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), GetAppSecretResponse.class
                );

                // --- Extract value based on GetAppSecretResponse structure ---
                if (response.getBody() != null && response.getBody().secret() != null &&
                        response.getBody().secret().staticVersion() != null &&
                        response.getBody().secret().staticVersion().value() != null)
                {
                    String value = response.getBody().secret().staticVersion().value();
                    log.info("Successfully fetched single secret '{}'.", secretName);
                    return value;
                } else {
                    log.error("Received unexpected response structure when fetching single secret '{}'. Body or nested fields null.", secretName);
                    throw new FetchSecretsException("Unexpected response structure for secret: " + secretName);
                }
                // --- End Extraction ---

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.error("Secret '{}' not found in HCP (404).", secretName);
                    // Throw specific exception that getSecret can catch
                    throw new FetchSecretsException("Secret not found: " + secretName, e);
                } else {
                    log.error("Client error fetching single secret '{}': {}", secretName, e.getStatusCode(), e);
                    handleClientError(e); // This might wrap and throw FetchSecretsException
                    throw e; // Rethrow for retryTemplate
                }
            } catch (HttpServerErrorException e) {
                log.error("Server error fetching single secret '{}': {}", secretName, e.getStatusCode(), e);
                handleServerError(e); // This throws FetchSecretsException
                throw e; // Rethrow for retryTemplate
            } catch (RestClientException e) {
                log.error("RestClientException fetching single secret '{}': {}", secretName, e.getMessage(), e);
                throw new FetchSecretsException("Failed to fetch single secret '" + secretName + "' from HCP", e);
            }
        });
    }


    /**
     * Fetches all secrets from HCP, handling pagination, and populates the cache.
     * This method is primarily intended for scheduled refresh or initial population.
     *
     * @throws FetchSecretsException if fetching fails after retries, or if pagination gets stuck.
     */
    private void fetchSecretsWithPagination() throws FetchSecretsException {
        // This method now returns void and populates the cache directly.
        retryTemplate.execute(context -> {
            String nextPageToken = null;
            int pageNum = 1;
            int totalSecretsProcessed = 0; // Track total secrets processed in this run
            log.info("Starting full secrets fetch process (Retry attempt {})", context.getRetryCount() + 1);

            String previousPageToken = null;
            int stuckTokenCounter = 0;
            final int MAX_STUCK_ATTEMPTS = 3;

            do {
                final int currentPageNum = pageNum;
                String currentRequestToken = nextPageToken;
                String url = buildPaginatedListUrl(currentRequestToken); // Use the list URL
                HttpHeaders headers = createHeaders();

                log.debug("Fetching secrets page {} from HCP. URL: {}", currentPageNum, url);
                String tokenUsed = headers.getFirst(HttpHeaders.AUTHORIZATION);
                log.debug("Authorization header being sent: {}", (tokenUsed != null ? tokenUsed.substring(0, Math.min(tokenUsed.length(), 15)) + "..." : "null"));

                try {
                    // Use GetSecretsResponse for the list endpoint
                    ResponseEntity<GetSecretsResponse> response = restTemplate.exchange(
                            url, HttpMethod.GET, new HttpEntity<>(headers), GetSecretsResponse.class
                    );

                    if (response.getBody() != null && response.getBody().secrets() != null) {
                        int secretsOnPage = 0;
                        for (SecretDetail detail : response.getBody().secrets()) {
                            if (detail != null && detail.name() != null && detail.staticVersion() != null && detail.staticVersion().value() != null) {
                                // --- Cache Change: Put individual secrets ---
                                secretsCache.put(detail.name(), detail.staticVersion().value());
                                secretsOnPage++;
                                // --- End Cache Change ---
                            } else {
                                log.warn("Skipping invalid secret entry on page {}: {}", currentPageNum, detail);
                            }
                        }
                        totalSecretsProcessed += secretsOnPage;
                        log.debug("Processed {} secrets from page {}. Total secrets processed in this run: {}", secretsOnPage, currentPageNum, totalSecretsProcessed);

                        // --- Pagination Logic (remains mostly the same) ---
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
                            secondsStr = secondsStr.replaceAll("\\D", "");
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
            return null; // Return type of execute is Void now
        });
    }


    /**
     * Builds the base URL for fetching the list of secrets (secrets:open).
     */
    private String buildBaseListUrl() {
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets:open",
                organizationId, projectId, appName
        );
    }

    /**
     * Builds the URL for fetching the list of secrets, including pagination token.
     */
    private String buildPaginatedListUrl(String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(buildBaseListUrl());
        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken);
        }
        return builder.toUriString();
    }

    /**
     * Builds the URL for fetching a single secret by name.
     */
    private String buildSingleSecretUrl(String secretName) {
        // IMPORTANT: Use the correct endpoint path for fetching a single secret
        // This assumes the path is /secrets/{secret_name} - VERIFY THIS with HCP Docs
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets/%s",
                organizationId, projectId, appName, secretName
        );
        // If the single secret endpoint is different (e.g., uses :open with a filter), adjust accordingly.
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        return headers;
    }

    // --- Convenience getters ---
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
    // --- End Convenience getters ---


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
    // --- End Error Handling ---


    /**
     * Scheduled task to refresh all secrets by invalidating the cache
     * and triggering a full fetch via pagination.
     */
    @Scheduled(fixedRateString = "${hcp.secrets.refresh-interval:1800000}") // Default 30 minutes
    public void refreshSecrets() {
        try {
            log.info("Starting scheduled secrets refresh...");
            // --- Cache Change: Invalidate all entries ---
            secretsCache.invalidateAll();
            log.info("Secrets cache invalidated.");
            // --- End Cache Change ---

            // Trigger reload by calling the pagination fetch method directly
            fetchSecretsWithPagination();
            log.info("Completed scheduled secrets refresh. Cache repopulated.");
        } catch (Exception e) {
            log.error("Scheduled secrets refresh failed.", e);
            // Allow scheduler to continue
        }
    }
}
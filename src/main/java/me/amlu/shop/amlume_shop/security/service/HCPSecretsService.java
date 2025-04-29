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
import me.amlu.shop.amlume_shop.payload.GetSecretsResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
import org.springframework.web.util.UriComponentsBuilder; // Import UriComponentsBuilder

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.payload.GetSecretsResponse.*;
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
    private final Cache<String, Map<String, String>> secretsCache;

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

    // Token for the secrets cache
    // To be used with the constants above
    public String getSecret(String key) {
        Map<String, String> secrets = getSecrets();
        String secret = secrets.get(key);
        if (secret == null) {
            // Consider logging a warning or error here as well
            log.warn("Secret key '{}' not found in retrieved secrets.", key);
            throw new FetchSecretsException("Secret not found: " + key);
        }
        return secret;
    }

    public Map<String, String> getSecrets() {
        try {
            // Cache key remains "secrets", loader method is fetchSecretsWithPagination
            return secretsCache.get("secrets", this::fetchSecretsWithPagination);
        } catch (ExecutionException e) {
            log.error("Failed to get secrets from cache loader", e.getCause() != null ? e.getCause() : e);
            // Throw the underlying cause if available, otherwise the wrapper
            Throwable cause = e.getCause();
            if (cause instanceof FetchSecretsException) {
                throw (FetchSecretsException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause; // Rethrow common runtime exceptions
            } else {
                throw new FetchSecretsException("Failed to fetch secrets due to cache execution error", e);
            }
        }
    }

    /**
     * Fetches secrets from HCP, handling pagination and potential stuck tokens.
     * This method will loop through pages until all secrets are retrieved or an issue occurs.
     * The entire multi-page fetch operation is wrapped in a retry mechanism via retryTemplate.
     *
     * @return A map containing all secrets fetched across all pages.
     * @throws FetchSecretsException if fetching fails after retries, or if pagination gets stuck.
     */
    private Map<String, String> fetchSecretsWithPagination() {
        Map<String, String> allSecrets = new HashMap<>();

        return retryTemplate.execute(context -> {
            allSecrets.clear();
            String nextPageToken = null;
            int pageNum = 1; // Declared in outer lambda scope
            log.info("Starting secrets fetch process (Retry attempt {})", context.getRetryCount() + 1);

            String previousPageToken = null;
            int stuckTokenCounter = 0;
            final int MAX_STUCK_ATTEMPTS = 3;

            do {
                // --- Create effectively final variable for inner lambda ---
                final int currentPageNum = pageNum; // This holds the value for THIS iteration
                // --- End change ---

                String currentRequestToken = nextPageToken;
                String url = buildPaginatedUrl(currentRequestToken);
                HttpHeaders headers = createHeaders();

                log.debug("Fetching secrets page {} from HCP. URL: {}", currentPageNum, url); // Use currentPageNum for consistency if desired
                String tokenUsed = headers.getFirst(HttpHeaders.AUTHORIZATION);
                log.debug("Authorization header being sent: {}", (tokenUsed != null ? tokenUsed.substring(0, Math.min(tokenUsed.length(), 15)) + "..." : "null"));

                try {
                    ResponseEntity<GetSecretsResponse> response = restTemplate.exchange(
                            url, HttpMethod.GET, new HttpEntity<>(headers), GetSecretsResponse.class
                    );

                    if (response.getBody() != null && response.getBody().secrets() != null) {
                        Map<String, String> currentPageSecrets = response.getBody().secrets().stream()
                                .filter(detail -> detail != null && detail.name() != null && detail.staticVersion() != null && detail.staticVersion().value() != null)
                                .collect(Collectors.toMap(
                                        SecretDetail::name,
                                        detail -> detail.staticVersion().value(),
                                        (existingValue, newValue) -> {
                                            // --- Use the effectively final variable ---
                                            log.warn("Duplicate secret key found ON PAGE {}. Using the value from the later entry on this page.", currentPageNum);
                                            // --- End change ---
                                            return newValue;
                                        }
                                ));

                        allSecrets.putAll(currentPageSecrets);
                        log.debug("Fetched {} secrets from page {}. Secrets accumulated in current attempt: {}", currentPageSecrets.size(), currentPageNum, allSecrets.size()); // Use currentPageNum

                        if (response.getBody().pagination() != null) {
                            String receivedToken = response.getBody().pagination().nextPageToken();
                            log.debug("Page {}: Received next page token: '{}'", currentPageNum, receivedToken); // Use currentPageNum

                            if (receivedToken != null && receivedToken.equals(currentRequestToken)) {
                                stuckTokenCounter++;
                                log.warn("Received the same page token ('{}') again. Stuck count: {}", receivedToken, stuckTokenCounter);
                                if (stuckTokenCounter >= MAX_STUCK_ATTEMPTS) {
                                    log.error("Page token appears stuck after {} attempts. Aborting pagination.", stuckTokenCounter);
                                    throw new FetchSecretsException("Pagination failed: Page token stuck after page " + currentPageNum); // Use currentPageNum
                                }
                            } else {
                                stuckTokenCounter = 0;
                            }
                            nextPageToken = receivedToken;
                            if (nextPageToken != null && nextPageToken.isBlank()) {
                                nextPageToken = null;
                            }
                        } else {
                            log.debug("Page {}: No pagination object found in response.", currentPageNum); // Use currentPageNum
                            nextPageToken = null;
                            stuckTokenCounter = 0;
                        }
                    } else {
                        log.warn("Received null body or null secrets list from HCP on page {}. Stopping pagination.", currentPageNum); // Use currentPageNum
                        nextPageToken = null;
                        stuckTokenCounter = 0;
                    }

                } catch (HttpClientErrorException.TooManyRequests e429) {
                    log.error("Client error fetching page {} from HCP: {}", currentPageNum, e429.getStatusCode(), e429); // Log full error first

                    long delaySeconds = 120; // INCREASED AS 60 IS NOT ENOUGH. Default 60 from error message or general safe default
                    try {
                        // Basic parsing (improve if needed for robustness)
                        String responseBody = e429.getResponseBodyAsString();
                        if (responseBody != null && responseBody.contains("try again in ")) {
                            String secondsStr = responseBody.substring(responseBody.indexOf("try again in ") + "try again in ".length()).split(" ")[0];
                            // Remove non-digit characters just in case
                            secondsStr = secondsStr.replaceAll("[^\\d]", "");
                            if (!secondsStr.isEmpty()) {
                                delaySeconds = Long.parseLong(secondsStr);
                                // Add a small buffer and sanity check
                                delaySeconds = Math.max(1, Math.min(delaySeconds + 2, 300)); // e.g., wait at least 1s, max 5 mins + buffer
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
                        // Wrap and throw immediately if interrupted, don't proceed to normal rethrow
                        throw new FetchSecretsException("Interrupted while waiting for rate limit cooldown", ie);
                    }
                    // Now rethrow the original exception so the outer retryTemplate handles the retry attempt logic
                    // The handleClientError might wrap it, but we still need the retryTemplate to see an exception.
                    handleClientError(e429); // Ensure it's wrapped appropriately if needed by handleClientError logic
                    throw e429; // Rethrow the original 429 error

                    // --- END: ADDED 429 CATCH BLOCK ---

                } catch (HttpClientErrorException e) { // <-- Catch other HttpClientErrors AFTER TooManyRequests
                    log.error("Client error fetching page {} from HCP: {}", currentPageNum, e.getStatusCode(), e); // Use currentPageNum
                    handleClientError(e); // This might wrap and throw FetchSecretsException
                    throw e; // Rethrow so retryTemplate can handle it

                } catch (HttpServerErrorException e) {
                    log.error("Server error fetching page {} from HCP: {}", currentPageNum, e.getStatusCode(), e); // Use currentPageNum
                    handleServerError(e); // This throws FetchSecretsException
                    throw e; // Rethrow so retryTemplate can handle it

                } catch (RestClientException e) {
                    log.error("RestClientException fetching page {} from HCP: {}", currentPageNum, e.getMessage(), e); // Use currentPageNum
                    // Wrap in FetchSecretsException if not already handled by specific handlers
                    throw new FetchSecretsException("Failed to fetch secrets from HCP on page " + currentPageNum, e); // Use currentPageNum
                }
                // Note: FetchSecretsException thrown by stuck token detection will also be caught by the outer retryTemplate mechanism

                pageNum++; // Modify the original pageNum for the next loop iteration
                previousPageToken = currentRequestToken;

            } while (nextPageToken != null); // <-- End of the do-while loop

            log.info("Finished fetching all secrets. Total secrets retrieved: {}", allSecrets.size());
            return allSecrets;

        }); // End of retryTemplate.execute
    }


    /**
     * Builds the base URL for fetching secrets.
     *
     * @return The base URL string.
     */
    private String buildBaseSecretsUrl() {
        // This method now only returns the base URL without pagination
        return String.format(
                "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets:open",
                organizationId, projectId, appName
        );
    }

    /**
     * Builds the full URL for fetching secrets, including the page token if provided.
     *
     * @param pageToken The token for the next page, or null/blank for the first page.
     * @return The full URL string with the page_token query parameter if applicable.
     */
    private String buildPaginatedUrl(String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(buildBaseSecretsUrl());
        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken); // Add page_token query parameter
        }
        return builder.toUriString();
    }


    // ... (createHeaders, getPaseto*Key methods, handleClientError, handleServerError remain the same) ...
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
        // Logged in fetchSecretsWithPagination, just throw appropriate exception here
        switch (e.getStatusCode()) {
            case UNAUTHORIZED:
                throw new FetchSecretsException("Unauthorized to fetch secrets", e);
            case FORBIDDEN:
                throw new FetchSecretsException("Forbidden to fetch secrets", e);
            case NOT_FOUND:
                throw new FetchSecretsException("Secrets not found (or invalid path/app)", e);
            default:
                throw new FetchSecretsException("Client error (" + e.getStatusCode() + ") while fetching secrets", e);
        }
    }

    private void handleServerError(HttpServerErrorException e) {
        // Logged in fetchSecretsWithPagination, just throw appropriate exception here
        throw new FetchSecretsException("Server error (" + e.getStatusCode() + ") while fetching secrets", e);
    }

    // --- Scheduled Refresh ---
    // This remains the same, it will trigger the cache invalidation and
    // cause getSecrets() -> fetchSecretsWithPagination() to run again.
    @Scheduled(fixedRateString = "${hcp.secrets.refresh-interval:1800000}") // Default 30 minutes
    public void refreshSecrets() {
        try {
            log.info("Starting scheduled secrets refresh..."); // Log info level
            secretsCache.invalidate("secrets"); // Invalidate cache entry
            getSecrets(); // Trigger reload (which calls fetchSecretsWithPagination)
            log.info("Completed scheduled secrets refresh.");
        } catch (Exception e) {
            // Log error but allow scheduler to continue
            log.error("Scheduled secrets refresh failed.", e);
        }
    }
}
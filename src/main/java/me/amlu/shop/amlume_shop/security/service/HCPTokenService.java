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

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TokenRefreshException;
import me.amlu.shop.amlume_shop.payload.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

//@Profile({"!local","!test"}) // Exclude this service from the local and test profile
@Service
@Slf4j
public class HCPTokenService {
    private static final String TOKEN_ENDPOINT = "https://auth.idp.hashicorp.com/oauth2/token";
    private String accessToken;
    private Instant tokenExpiration;

    @Value("${HCP_CLIENT_ID}")
    private String clientId;

    @Value("${HCP_CLIENT_SECRET}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public HCPTokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {
        if (isTokenExpired()) {
            refreshToken();
        }
        return accessToken;
    }

    private synchronized void refreshToken() {
        try {
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("grant_type", "client_credentials");
            requestBody.add("audience", "https://api.hashicorp.cloud");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    TOKEN_ENDPOINT,
                    new HttpEntity<>(requestBody, headers),
                    TokenResponse.class
            );

            if (response.getBody() != null) {
                this.accessToken = response.getBody().accessToken();
                this.tokenExpiration = Instant.now().plusSeconds(response.getBody().expiresIn());
            } else {
                log.error("Empty response body when refreshing token");
                throw new TokenRefreshException("Empty response body when refreshing token");
            }

        } catch (HttpClientErrorException e) {
            log.error("Failed to refresh HCP token due to client error: {}", e.getStatusCode(), e);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new TokenRefreshException("Authentication failed while refreshing HCP token", e);
            } else {
                throw new TokenRefreshException("Client error while refreshing HCP token", e);
            }
        } catch (HttpServerErrorException e) {
            log.error("Failed to refresh HCP token due to server error: {}", e.getStatusCode(), e);
            throw new TokenRefreshException("Server error while refreshing HCP token", e);
        } catch (RestClientException e) {
            log.error("Failed to refresh HCP token due to a rest client error", e);
            throw new TokenRefreshException("Rest client error while refreshing HCP token", e);
        } catch (Exception e) {
            log.error("Failed to refresh HCP token due to an unexpected error", e);
            throw new TokenRefreshException("Unexpected error while refreshing HCP token", e);
        }
    }

    private boolean isTokenExpired() {
        return accessToken == null || tokenExpiration == null ||
                Instant.now().isAfter(tokenExpiration.minus(Duration.ofMinutes(5)));
    }
}

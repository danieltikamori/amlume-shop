/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/** Usage Example:
 * @Service
 * public class YourService {
 *     private final HCPSecretsService secretsService;
 *
 *     public YourService(HCPSecretsService secretsService) {
 *         this.secretsService = secretsService;
 *     }
 *
 *     public void someMethod() {
 *         Map<String, String> secrets = secretsService.getSecrets();
 *         // Use your secrets here
 *         String apiKey = secrets.get("API_KEY");
 *     }
 * }
 */

package me.amlu.shop.amlume_shop.security.service;

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.FetchSecretsException;
import me.amlu.shop.amlume_shop.payload.SecretsResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import java.util.Collections;

@Service
@Slf4j
public class HCPSecretsService {
    private final HCPTokenService tokenService;
    private final RestTemplate restTemplate;
    
    @Value("${HCP_ORGANIZATION_ID}")
    private String organizationId;
    
    @Value("${HCP_PROJECT_ID}")
    private String projectId;
    
    @Value("${HCP_APP_NAME}")
    private String appName;

    public HCPSecretsService(HCPTokenService tokenService) {
        this.tokenService = tokenService;
        this.restTemplate = new RestTemplate();
    }

    public Map<String, String> getSecrets() {
        try {
            String url = String.format(
                    "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets:open",
                    organizationId, projectId, appName
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokenService.getAccessToken());

            ResponseEntity<SecretsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    SecretsResponse.class
            );

            if (response.getBody() != null) {
                return response.getBody().getSecrets();
            } else {
                return Collections.emptyMap();
            }

        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch secrets from HCP due to client error: {}", e.getStatusCode(), e);

            if(e.getStatusCode() == HttpStatus.UNAUTHORIZED){
                throw new FetchSecretsException("Unauthorized to fetch secrets", e);
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN){
                throw new FetchSecretsException("Forbidden to fetch secrets",e);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND){
                throw new FetchSecretsException("Secrets not found",e);
            } else {
                throw new FetchSecretsException("Client error while fetching secrets", e);
            }
        } catch (HttpServerErrorException e) {
            log.error("Failed to fetch secrets from HCP due to server error: {}", e.getStatusCode(), e);
            throw new FetchSecretsException("Server error while fetching secrets", e);
        } catch (RestClientException e) {
            log.error("Failed to fetch secrets from HCP due to a rest client error", e);
            throw new FetchSecretsException("Rest client error while fetching secrets", e);
        } catch (Exception e) {
            log.error("Failed to fetch secrets from HCP due to an unexpected error", e);
            throw new FetchSecretsException("Unexpected error while fetching secrets", e);
        }
    }
}

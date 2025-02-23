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

import me.amlu.shop.amlume_shop.exceptions.FetchSecretsException;
import me.amlu.shop.amlume_shop.payload.SecretsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HCPSecretsServiceTest {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HCPTokenService tokenService;

    @InjectMocks
    private HCPSecretsService secretsService;

    private static final String ORGANIZATION_ID = "test-org";
    private static final String PROJECT_ID = "test-project";
    private static final String APP_NAME = "test-app";
    private static final String ACCESS_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        secretsService = new HCPSecretsService(
                restTemplate,
                tokenService,
                ORGANIZATION_ID,
                PROJECT_ID,
                APP_NAME
        );

        when(tokenService.getAccessToken()).thenReturn(ACCESS_TOKEN);
    }

    @Test
    void getSecrets_Success() {
        // Arrange
        Map<String, String> expectedSecrets = Map.of("key1", "value1", "key2", "value2");
        SecretsResponse mockResponse = new SecretsResponse();
        mockResponse.setSecrets(expectedSecrets);

        ResponseEntity<SecretsResponse> responseEntity =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        )).thenReturn(responseEntity);

        // Act
        Map<String, String> result = secretsService.getSecrets();

        // Assert
        assertThat(result).isEqualTo(expectedSecrets);
        verify(tokenService).getAccessToken();
    }

    @Test
    void getSecrets_WhenUnauthorized_ShouldThrowException() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        // Act & Assert
        assertThrows(FetchSecretsException.class, () -> secretsService.getSecrets());
    }

    @Test
    void getSecrets_WhenServerError_ShouldRetry() {
        // Arrange
        Map<String, String> expectedSecrets = Map.of("key1", "value1");
        SecretsResponse mockResponse = new SecretsResponse();
        mockResponse.setSecrets(expectedSecrets);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        ))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        Map<String, String> result = secretsService.getSecrets();

        // Assert
        assertThat(result).isEqualTo(expectedSecrets);
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        );
    }

    @Test
    void getSecrets_WhenCached_ShouldNotCallAPI() {
        // Arrange
        Map<String, String> expectedSecrets = Map.of("key1", "value1");
        SecretsResponse mockResponse = new SecretsResponse();
        mockResponse.setSecrets(expectedSecrets);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        secretsService.getSecrets(); // First call
        secretsService.getSecrets(); // Second call - should use cache

        // Assert
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(SecretsResponse.class)
        );
    }
}

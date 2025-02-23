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

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HCPSecretsServiceTest {
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private HCPSecretsService secretsService;

    @BeforeEach
    void setUp() {
        // Set up environment variables for testing
        environmentVariables.set("HCP_CLIENT_ID", "test-client-id");
        environmentVariables.set("HCP_CLIENT_SECRET", "test-client-secret");
    }

    @Test
    void getSecrets_Success() {
        // Arrange
        Map<String, String> expectedSecrets = new HashMap<>();
        expectedSecrets.put("test-key", "test-value");

        ResponseEntity<Map<String, String>> responseEntity = 
            new ResponseEntity<>(expectedSecrets, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // Act
        Map<String, String> actualSecrets = secretsService.getSecrets();

        // Assert
        assertThat(actualSecrets).isEqualTo(expectedSecrets);
    }

    @Test
    void getSecrets_WhenHCPFails_ShouldRetry() {
        // Arrange
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            any(ParameterizedTypeReference.class)
        ))
        .thenThrow(new RestClientException("Network error"))
        .thenReturn(new ResponseEntity<>(Map.of("key", "value"), HttpStatus.OK));

        // Act
        Map<String, String> secrets = secretsService.getSecrets();

        // Assert
        assertThat(secrets).containsEntry("key", "value");
        verify(restTemplate, times(2)).exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void getSecret_WhenCached_ShouldNotCallHCP() {
        // Arrange
        Map<String, String> secrets = Map.of("test-key", "test-value");
        ResponseEntity<Map<String, String>> responseEntity = 
            new ResponseEntity<>(secrets, HttpStatus.OK);

        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // Act
        String value1 = secretsService.getSecret("test-key");
        String value2 = secretsService.getSecret("test-key");

        // Assert
        assertThat(value1).isEqualTo("test-value");
        assertThat(value2).isEqualTo("test-value");
        verify(restTemplate, times(1)).exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            any(ParameterizedTypeReference.class)
        );
    }
}

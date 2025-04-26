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

import me.amlu.shop.amlume_shop.exceptions.VaultOperationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VaultServiceTest {

    private VaultTemplate vaultTemplate;
    private VaultService vaultService;
    private final String path = "testPath";

    @BeforeEach
    void setUp() {
        vaultTemplate = mock(VaultTemplate.class);
        vaultService = new VaultService(vaultTemplate);
    }

    @Test
    void shouldGetSecret() {
        // Arrange
        String key = "testKey";
        String expectedValue = "testValue";
        VaultResponse mockResponse = new VaultResponse();
        Map<String, Object> data = new HashMap<>();
        data.put(key, expectedValue);
        mockResponse.setData(data);

        when(vaultTemplate.read(anyString())).thenReturn(mockResponse);

        // Act
        String actualValue = vaultService.getSecret(this.path, key);

        // Assert
        assertEquals(expectedValue, actualValue);
        verify(vaultTemplate).read(anyString());
    }

    @Test
    void shouldSetSecret() {
        // Arrange
        String key = "testKey";
        String value = "testValue";
        VaultResponse mockResponse = new VaultResponse();
        Map<String, Object> existingData = new HashMap<>();
        mockResponse.setData(existingData);

        when(vaultTemplate.read(anyString())).thenReturn(mockResponse);

        // Act
        vaultService.setSecret(this.path, key, value);

        // Assert
        verify(vaultTemplate).write(anyString(), argThat((Map<String, Object> map) ->
                map.containsKey(key) && value.equals(map.get(key))
        ));
    }

    @Test
    void shouldDeleteSecret() {
        // Arrange
        String key = "testKey";
        VaultResponse mockResponse = new VaultResponse();
        Map<String, Object> existingData = new HashMap<>();
        existingData.put(key, "value");
        mockResponse.setData(existingData);

        when(vaultTemplate.read(anyString())).thenReturn(mockResponse);

        // Act
        vaultService.deleteSecret(this.path, key);

        // Assert
        verify(vaultTemplate).write(anyString(), argThat((Map<String, Object> map) ->
                !map.containsKey(key)
        ));
    }

    @Test
    void shouldHandleVaultErrors() {
        // Arrange
        when(vaultTemplate.read(anyString()))
                .thenThrow(new RuntimeException("Vault error"));

        // Act & Assert
        assertThrows(VaultOperationException.class, () ->
                vaultService.getSecret("","testKey")
        );
    }

    @Test
    void shouldHandleNullResponse() {
        // Arrange
        when(vaultTemplate.read(anyString())).thenReturn(null);

        // Act
        String result = vaultService.getSecret("","testKey");

        // Assert
        assertNull(result);
    }

    @Test
    void shouldHandleNullData() {
        // Arrange
        VaultResponse mockResponse = new VaultResponse();
        mockResponse.setData(null);
        when(vaultTemplate.read(anyString())).thenReturn(mockResponse);

        // Act
        String result = vaultService.getSecret("","testKey");

        // Assert
        assertNull(result);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getSecretOptional() {
    }

    @Test
    void getSecretOptionalFallback() {
    }

    @Test
    void getSecret() {
    }

    @Test
    void setSecret() {
    }

    @Test
    void deleteSecret() {
    }

    @Test
    void writeSecretFallback() {
    }

    @Test
    void testWriteSecretFallback() {
    }
}

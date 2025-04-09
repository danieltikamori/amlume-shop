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
import me.amlu.shop.amlume_shop.exceptions.VaultOperationException;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VaultService {
    private static final String SECRET_PATH = "secret/amlume-shop";
    private final VaultTemplate vaultTemplate;

    public VaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public String getSecret(String key) {
        try {
            VaultResponse response = vaultTemplate.read(SECRET_PATH);
            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();
                return (String) data.get(key);
            }
            return null;
        } catch (Exception e) {
            log.error("Error retrieving secret from Vault: {}", key, e);
            throw new VaultOperationException("Failed to retrieve secret: " + key, e);
        }
    }

    public void setSecret(String key, String value) {
        try {
            VaultResponse existing = vaultTemplate.read(SECRET_PATH);
            Map<String, Object> data = existing != null && existing.getData() != null ?
                    existing.getData() : new HashMap<>();

            data.put(key, value);
            vaultTemplate.write(SECRET_PATH, data);

            log.info("Secret updated successfully: {}", key);
        } catch (Exception e) {
            log.error("Error storing secret in Vault: {}", key, e);
            throw new VaultOperationException("Failed to store secret: " + key, e);
        }
    }

    public void deleteSecret(String key) {
        try {
            VaultResponse existing = vaultTemplate.read(SECRET_PATH);
            if (existing != null && existing.getData() != null) {
                Map<String, Object> data = existing.getData();
                data.remove(key);
                vaultTemplate.write(SECRET_PATH, data);
                log.info("Secret deleted successfully: {}", key);
            }
        } catch (Exception e) {
            log.error("Error deleting secret from Vault: {}", key, e);
            throw new VaultOperationException("Failed to delete secret: " + key, e);
        }
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.vault.support.Versioned;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class VaultService {

    @Value("${vault.secret.base-path:secret/data/amlume-shop}")
    private String secretBasePath;

    private final VaultTemplate vaultTemplate;

    private static final Logger log = LoggerFactory.getLogger(VaultService.class);

    public VaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    // This method uses non-versioned ops for simplicity
    public Optional<String> getSecretOptional(String key) {
        try {
            // Use non-versioned ops for simple reads if version doesn't matter
            VaultKeyValueOperations kv2Ops = vaultTemplate.opsForKeyValue(secretBasePath, VaultKeyValueOperations.KeyValueBackend.KV_2);
            VaultResponseSupport<Map<String, Object>> response = kv2Ops.get(""); // Read data at the base path

            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();
                return Optional.ofNullable((String) data.get(key));
            }
            log.debug("No data found at Vault path '{}' or path does not exist.", secretBasePath);
            return Optional.empty();
        } catch (VaultException e) {
            log.error("Vault error retrieving secret '{}' from path '{}': {}", key, secretBasePath, e.getMessage());
            throw new VaultOperationException("Failed to retrieve secret: " + key, e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving secret '{}' from path '{}'", key, secretBasePath, e);
            throw new VaultOperationException("Unexpected error retrieving secret: " + key, e);
        }
    }

    public String getSecret(String key) {
        return getSecretOptional(key).orElse(null);
    }


    /**
     * Sets a secret in the Vault using the KV v2 API.
     * Writes with Check-and-Set (CAS) using versioning.
     * <p>
     * About the warning: Contents of collection 'data' are updated, but never queried
     * Probably NOT a functional bug.In the context of interacting with the Vault KV v2 API (which requires you to read the existing data, modify it, and write the entire modified map back for updates/deletes using Check-and-Set), the data map is acting as a temporary container or builder for the payload you send back to Vault.
     *
     * @param key
     * @param value
     */

    public void setSecret(String key, String value) {
        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(secretBasePath);

            // 1. Read current data and metadata using Versioned
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            Map<String, Object> data = new HashMap<>(); // Start with empty data
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null; // <-- Get Versioned.Metadata
            Versioned.Version versionForCas; // This will hold the version for the put operation

            if (metadata != null) { // or if (metadata.getVersion() != null)
                // Path exists, get the current version for CAS
                versionForCas = metadata.getVersion();
                if (response.getData() != null) {
                    // Create a mutable copy of existing data
                    data = new HashMap<>(response.getData());
                }
                log.debug("For setting. Read existing secret at path '{}', current version {}.", secretBasePath, versionForCas.getVersion());
            } else {
                // Path doesn't exist or has no metadata/data. Use CAS=0 for creation.
                versionForCas = Versioned.Version.unversioned(); // <<<--- Specify CAS 0 for creation
                log.info("Secret path '{}' does not exist or has no data/metadata. Will attempt to create with CAS version 0.", secretBasePath);
            }

            // 2. Modify data
            data.put(key, value);

            // 3. Write with CAS using the Versioned.Version object
            kv2Ops.put("", versionForCas); // <<<--- Pass the Versioned.Version object directly

            int expectedNewVersion = (versionForCas.isVersioned()) ? versionForCas.getVersion() + 1 : 1;
            log.info("Secret updated successfully using CAS at path '{}' for key (length {}). New version should be {}.", secretBasePath, key.length(), expectedNewVersion);

        } catch (VaultException e) {
            // Check specifically for CAS failure (a message might vary slightly)
            if (e.getMessage() != null && (e.getMessage().contains("check-and-set parameter did not match the current version") || e.getMessage().contains("status 400") || e.getMessage().contains("status 412"))) {
                log.warn("CAS conflict while setting secret '{}' at path '{}'. Concurrent modification detected.", key, secretBasePath);
                throw new VaultOperationException("Failed to set secret due to concurrent modification conflict: " + key, e);
            } else {
                log.error("Vault error storing secret '{}' at path '{}': {}", key, secretBasePath, e.getMessage());
                throw new VaultOperationException("Failed to store secret: " + key, e);
            }
        } catch (Exception e) {
            log.error("Unexpected error storing secret '{}' at path '{}'", key, secretBasePath, e);
            throw new VaultOperationException("Unexpected error storing secret: " + key, e);
        }
    }

    /**
     * Deletes a key version using CAS.
     *
     * @param key
     */
    public void deleteSecret(String key) {
        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(secretBasePath);

            // 1. Read current data and metadata using Versioned
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            Map<String, Object> data = null;
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null;
            Versioned.Version versionForCas = null; // Version for the put operation

            if (metadata != null) { // or if (metadata.getVersion() != null)
                versionForCas = metadata.getVersion();
                if (response.getData() != null) {
                    data = new HashMap<>(response.getData()); // Mutable copy
                }
                log.debug("For deletion. Read existing secret at path '{}', current version {}.", secretBasePath, versionForCas.getVersion());
            }

            // 2. Check if the path/data exists and contains the key
            if (versionForCas == null || data == null || !data.containsKey(key)) {
                log.warn("Attempted to delete non-existent secret key '{}' from path '{}'. No changes made.", key, secretBasePath);
                // Decide if this is an error or just a no-op
                // throw new VaultOperationException("Secret key '" + key + "' not found at path '" + secretBasePath + "'");
                return; // Key doesn't exist, nothing to delete
            }

            // 3. Modify data
            data.remove(key);

            // 4. Write back with CAS using the Versioned.Version object
            kv2Ops.put("", versionForCas); // <<<--- Pass the Versioned.Version object directly

            int expectedNewVersion = versionForCas.getVersion() + 1;
            log.info("Secret deleted successfully using CAS at path '{}' for key (length {}). New version should be {}.", secretBasePath, key.length(), expectedNewVersion);

        } catch (VaultException e) {
            // Check specifically for CAS failure
            if (e.getMessage() != null && (e.getMessage().contains("check-and-set parameter did not match the current version") || e.getMessage().contains("status 400") || e.getMessage().contains("status 412"))) {
                log.warn("CAS conflict while deleting secret '{}' at path '{}'. Concurrent modification detected.", key, secretBasePath);
                throw new VaultOperationException("Failed to delete secret due to concurrent modification conflict: " + key, e);
            } else {
                log.error("Vault error deleting secret '{}' from path '{}': {}", key, secretBasePath, e.getMessage());
                throw new VaultOperationException("Failed to delete secret: " + key, e);
            }
        } catch (Exception e) {
            log.error("Unexpected error deleting secret '{}' from path '{}'", key, secretBasePath, e);
            throw new VaultOperationException("Unexpected error deleting secret: " + key, e);
        }
    }
}

// --- Considered Finer-Grained Secret Paths ---
// Storing many unrelated secrets under one path (`secret/amlume-shop`) can become unwieldy
// and makes Vault ACLs less granular.
// Consider methods to read/write to more specific paths
// e.g., `secret/amlume-shop/database/password`, `secret/amlume-shop/api/external-key`.
// This would change the service's methods significantly
// (e.g., `getSecret(String path, String key)` or just `getSecret(String fullPath)`).

// --- Resilience ---
// If Vault access is critical and potentially unreliable, consider adding Resilience4j
// annotations (@Retry, @CircuitBreaker) to these methods, especially if VaultTemplate
// doesn't handle retries internally for the specific errors you encounter.

// --- Security Context (VaultConfig) ---
// While not in this file, the `VaultConfig` uses TokenAuthentication. For production,
// using more robust methods like AppRole or Kubernetes authentication is strongly recommended
// over long-lived static tokens.

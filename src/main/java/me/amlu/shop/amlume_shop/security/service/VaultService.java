/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 * ... (rest of copyright)
 */

package me.amlu.shop.amlume_shop.security.service;

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.VaultOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.VaultMetadataRequest;
import org.springframework.vault.support.VaultMetadataResponse;
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


    public void setSecret(String key, String value) {
        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(secretBasePath);

            // 1. Read current data and metadata using Versioned
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            int currentVersion = 0; // Default to 0 for CAS when creating a new path/secret
            Map<String, Object> data = new HashMap<>(); // Start with empty data
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null; // <-- Get Versioned.Metadata
//            Versioned.Version versionForCas; // This will hold the version for the put operation

            if (metadata != null) {
                Versioned.Version versionInfo = metadata.getVersion(); // <-- Get Versioned.Version
                // Path exists, get the current version for CAS
                currentVersion = versionInfo.getVersion(); // <<<--- Get the integer version
                if (response.getData() != null) {
                    // Create a mutable copy of existing data
                    data = new HashMap<>(response.getData()); // <-- Access data via response.getData()
                }
                log.debug("Read existing secret at path '{}', current version {}.", secretBasePath, currentVersion);
            } else {
                // Path doesn't exist or has no metadata/data
                log.info("Secret path '{}' does not exist or has no data/metadata. Will attempt to create with CAS version 0.", secretBasePath);
            }

            // 2. Modify data
            data.put(key, value);

            // 3. Write with CAS using the read version
            // VaultMetadataRequest is used to specify CAS parameter
            kv2Ops.put("", data, VaultMetadataRequest.builder().cas(currentVersion).build()); // <<<--- Use currentVersion for CAS

            log.info("Secret updated successfully using CAS at path '{}' for key (length {}). New version should be {}.", secretBasePath, key.length(), currentVersion + 1);

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

    public void deleteSecret(String key) {
        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(secretBasePath);

            // 1. Read current data and metadata using Versioned
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            int currentVersion = -1; // Indicates path doesn't exist or read failed
            Map<String, Object> data = null;
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null; // <-- Get Versioned.Metadata

            if (metadata != null) {
                Versioned.Version versionInfo = metadata.getVersion(); // <-- Get Versioned.Version
                if (versionInfo != null) {
                    currentVersion = versionInfo.getVersion(); // <<<--- Get the integer version
                    if (response.getData() != null) {
                        data = new HashMap<>(response.getData()); // <-- Access data via response.getData(). Mutable copy
                    }
                    log.debug("Read existing secret at path '{}', current version {}.", secretBasePath, currentVersion);
                } else {
                    // Metadata exists but no version info
                    log.warn("Vault metadata exists but version information is missing for path '{}'. Cannot perform CAS delete.", secretBasePath);
                    throw new VaultOperationException("Missing version information, cannot perform CAS delete for key: " + key);
                }
            }

            // 2. Check if the path/data exists and contains the key
            if (currentVersion == -1 || data == null || !data.containsKey(key)) {
                log.warn("Attempted to delete non-existent secret key '{}' from path '{}'. No changes made.", key, secretBasePath);
                // Decide if this is an error or just a no-op
                // throw new VaultOperationException("Secret key '" + key + "' not found at path '" + secretBasePath + "'");
                return; // Key doesn't exist, nothing to delete
            }

            // 3. Modify data
            data.remove(key);

            // 4. Write back with CAS using the read version
            // VaultMetadataRequest is used to specify CAS parameter
            kv2Ops.put("", data, VaultMetadataRequest.builder().cas(currentVersion).build()); // <<<--- Use currentVersion for CAS

            log.info("Secret deleted successfully using CAS at path '{}' for key (length {}). New version should be {}.", secretBasePath, key.length(), currentVersion + 1);

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

    // --- Improvement 6: Consider Finer-Grained Secret Paths ---
    // Storing many unrelated secrets under one path (`secret/amlume-shop`) can become unwieldy
    // and makes Vault ACLs less granular. Consider methods to read/write to more specific paths
    // e.g., `secret/amlume-shop/database/password`, `secret/amlume-shop/api/external-key`.
    // This would change the service's methods significantly (e.g., `getSecret(String path, String key)` or just `getSecret(String fullPath)`).

    // --- Improvement 7: Resilience ---
    // If Vault access is critical and potentially unreliable, consider adding Resilience4j
    // annotations (@Retry, @CircuitBreaker) to these methods, especially if VaultTemplate
    // doesn't handle retries internally for the specific errors you encounter.

    // --- Improvement 8: Security Context (VaultConfig) ---
    // While not in this file, the `VaultConfig` uses TokenAuthentication. For production,
    // using more robust methods like AppRole or Kubernetes authentication is strongly recommended
    // over long-lived static tokens.

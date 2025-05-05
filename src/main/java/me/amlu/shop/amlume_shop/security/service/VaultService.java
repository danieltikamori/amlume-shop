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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import me.amlu.shop.amlume_shop.exceptions.VaultOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Removed @Value for secretBasePath as it's now a parameter
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.vault.support.Versioned;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class VaultService {

    private final VaultTemplate vaultTemplate;
    private static final Logger log = LoggerFactory.getLogger(VaultService.class);

    // Define names for Resilience4j configurations (must match config in application.yml or ResilienceConfig.java)
    private static final String VAULT_RESILIENCE_CONFIG = "vaultService"; // Common name for CB and Retry

    public VaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    /**
     * Retrieves a secret value of a specific version from a specific path in Vault, returning an Optional.
     * Includes Retry and Circuit Breaker resilience patterns.
     *
     * @param path The Vault path (e.g., "secret/data/amlume-shop/database").
     * @param key  The key of the secret within the path.
     * @param version The version of the secret to retrieve.
     * @return An Optional containing the secret value if found, otherwise empty.
     * @throws VaultOperationException if a non-transient Vault error occurs or fallback fails.
     */
    @Retry(name = VAULT_RESILIENCE_CONFIG)
    @CircuitBreaker(name = VAULT_RESILIENCE_CONFIG, fallbackMethod = "getSecretOptionalFallback")
    public Optional<String> getSpecificSecretOptional(String path, String key, int version) {
        validatePathAndKey(path, key);
        try {
            // Use versioned ops for specific reads if a version does matter
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(path);
            Versioned<Map<String, Object>> response = kv2Ops.get("", Versioned.Version.from(version)); // Read data at the specified path

            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();
                log.debug("Successfully retrieved data from Vault path '{}'. with version '{}'.", path, version);
                return Optional.ofNullable((String) data.get(key));
            }
            log.debug("No data found at Vault path '{}'with version '{}' or path does not exist.", path, version);
            return Optional.empty();
        } catch (VaultException e) {
            log.error("Vault error retrieving secret key '{}'of version '{}' from path '{}': {}", key, version, path, e.getMessage());
            // Let Resilience4j handle this based on configuration (retry/break circuit)
            throw new VaultOperationException("Failed to retrieve secret: " + key + " of version: " + version + " from path: " + path, e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving secret key '{}' of version '{}' from path '{}'", key, version, path, e);
            throw new VaultOperationException("Unexpected error retrieving secret: " + key + " of version: " + version + " from path: " + path, e);
        }
    }

    /**
     * Fallback method for getSecretOptional. Returns empty Optional on failure.
     */
    public Optional<String> getSpecificSecretOptionalFallback(String path, String key, Throwable t, int version) {
        log.warn("VaultService.getSecretOptional fallback triggered for path '{}', key '{}', version '{}' due to: {}", path, key, version, t.getMessage());
        return Optional.empty(); // Fail safely for reads
    }

    /**
     * Retrieves a secret value from a specific path in the Vault.
     * Returns null if the secret is not found.
     *
     * @param path The Vault path (e.g., "secret/data/amlume-shop/database").
     * @param key  The key of the secret within the path.
     * @return The secret value as a String, or null if not found or on error during fallback.
     */
    public String getSpecificSecret(String path, String key, int version) {
        // Uses the resilient getSecretOptional method
        return getSpecificSecretOptional(path, key, version).orElse(null);
    }

    /**
     * Retrieves a specific version of a secret from Vault and maps it directly to the specified type T.
     * Includes Retry and Circuit Breaker resilience patterns.
     * Assumes the data stored at the Vault path directly corresponds to the fields of the target class T.
     *
     * @param path    The Vault path (e.g., "secret/data/amlume-shop/config-object").
     * @param version The specific version number to retrieve.
     * @param type    The Class representing the target type to map the data to.
     * @param <T>     The target type.
     * @return An Optional containing the mapped object if found and mapping is successful, otherwise empty.
     * @throws VaultOperationException      if a non-transient Vault error occurs or fallback fails.
     * @throws IllegalArgumentException if path, type is null, or version is not positive.
     */
    @Retry(name = VAULT_RESILIENCE_CONFIG)
    @CircuitBreaker(name = VAULT_RESILIENCE_CONFIG, fallbackMethod = "getSpecificVersionedSecretAsTypeFallback")
    public <T> Optional<T> getSpecificVersionedSecretAsType(String path, int version, Class<T> type) {
        // Validate path and type, version
        validatePath(path); // Use a simpler validation for just the path
        Objects.requireNonNull(type, "Target type cannot be null.");
        if (version <= 0) {
            throw new IllegalArgumentException("Version must be a positive integer.");
        }

        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(path);
            // Use the desired overload: get(path, version, type)
            // Note: The first argument to get() is the sub-path within the mount point, often empty ("")
            Versioned<T> response = kv2Ops.get("", Versioned.Version.from(version), type);

            if (response != null && response.hasData()) {
                log.debug("Successfully retrieved and mapped data from Vault path '{}', version '{}' to type {}.", path, version, type.getSimpleName());
                // getData() now returns an object of type T
                return Optional.ofNullable(response.getData());
            }
            log.debug("No data found at Vault path '{}', version '{}' or mapping to type {} failed.", path, version, type.getSimpleName());
            return Optional.empty();
        } catch (VaultException e) {
            // Handle Vault-specific errors
            log.error("Vault error retrieving version '{}' from path '{}' as type {}: {}", version, path, type.getSimpleName(), e.getMessage());
            throw new VaultOperationException("Failed to retrieve secret version: " + version + " from path: " + path + " as " + type.getSimpleName(), e);
        } catch (Exception e) {
            // Catch other potential errors, including mapping/deserialization issues
            log.error("Unexpected error retrieving version '{}' from path '{}' as type {}: {}", version, path, type.getSimpleName(), e.getMessage(), e);
            throw new VaultOperationException("Unexpected error retrieving secret version: " + version + " from path: " + path + " as " + type.getSimpleName(), e);
        }
    }

    /**
     * Fallback method for getSpecificVersionedSecretAsType. Returns empty Optional on failure.
     */
    public <T> Optional<T> getSpecificVersionedSecretAsTypeFallback(String path, int version, Class<T> type, Throwable t) {
        log.warn("VaultService.getSpecificVersionedSecretAsType fallback triggered for path '{}', version '{}', type {} due to: {}", path, version, type.getSimpleName(), t.getMessage());
        return Optional.empty(); // Fail safely for reads
    }

    /**
     * Retrieves a secret value from a specific path in Vault, returning an Optional.
     * Includes Retry and Circuit Breaker resilience patterns.
     *
     * @param path The Vault path (e.g., "secret/data/amlume-shop/database").
     * @param key  The key of the secret within the path.
     * @return An Optional containing the secret value if found, otherwise empty.
     * @throws VaultOperationException if a non-transient Vault error occurs or fallback fails.
     */
    @Retry(name = VAULT_RESILIENCE_CONFIG)
    @CircuitBreaker(name = VAULT_RESILIENCE_CONFIG, fallbackMethod = "getSecretOptionalFallback")
    public Optional<String> getSecretOptional(String path, String key) {
        validatePathAndKey(path, key);
        try {
            // Use non-versioned ops for simple reads if a version doesn't matter
            VaultKeyValueOperations kv2Ops = vaultTemplate.opsForKeyValue(path, VaultKeyValueOperations.KeyValueBackend.KV_2);
            VaultResponseSupport<Map<String, Object>> response = kv2Ops.get(""); // Read data at the specified path

            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();
                log.debug("Successfully retrieved data from Vault path '{}'.", path);
                return Optional.ofNullable((String) data.get(key));
            }
            log.debug("No data found at Vault path '{}' or path does not exist.", path);
            return Optional.empty();
        } catch (VaultException e) {
            log.error("Vault error retrieving secret key '{}' from path '{}': {}", key, path, e.getMessage());
            // Let Resilience4j handle this based on configuration (retry/break circuit)
            throw new VaultOperationException("Failed to retrieve secret: " + key + " from path: " + path, e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving secret key '{}' from path '{}'", key, path, e);
            throw new VaultOperationException("Unexpected error retrieving secret: " + key + " from path: " + path, e);
        }
    }

    /**
     * Fallback method for getSecretOptional. Returns empty Optional on failure.
     */
    public Optional<String> getSecretOptionalFallback(String path, String key, Throwable t) {
        log.warn("VaultService.getSecretOptional fallback triggered for path '{}', key '{}' due to: {}", path, key, t.getMessage());
        return Optional.empty(); // Fail safely for reads
    }

    /**
     * Retrieves a secret value from a specific path in the Vault.
     * Returns null if the secret is not found.
     *
     * @param path The Vault path (e.g., "secret/data/amlume-shop/database").
     * @param key  The key of the secret within the path.
     * @return The secret value as a String, or null if not found or on error during fallback.
     */
    public String getSecret(String path, String key) {
        // Uses the resilient getSecretOptional method
        return getSecretOptional(path, key).orElse(null);
    }

    /**
     * Sets a secret in Vault at a specific path using the KV v2 API with Check-and-Set (CAS).
     * Includes Retry and Circuit Breaker resilience patterns.
     *
     * @param path  The Vault path (e.g., "secret/data/amlume-shop/api-keys").
     * @param key   The key of the secret.
     * @param value The value of the secret.
     * @throws VaultOperationException if the operation fails after retries or the circuit is open.
     */
    @Retry(name = VAULT_RESILIENCE_CONFIG)
    @CircuitBreaker(name = VAULT_RESILIENCE_CONFIG, fallbackMethod = "writeSecretFallback")
    public void setSecret(String path, String key, String value) {
        validatePathAndKey(path, key);
        if (value == null) {
            log.warn("Attempted to set null value for key '{}' at path '{}'. Deleting instead.", key, path);
            deleteSecret(path, key); // Or throw IllegalArgumentException based on desired behavior
            return;
        }

        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(path);

            // 1. Read current data and metadata using Versioned
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            Map<String, Object> data = new HashMap<>(); // Start with empty data
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null;
            Versioned.Version versionForCas;

            if (metadata != null) {
                versionForCas = metadata.getVersion();
                if (response.getData() != null) {
                    data = new HashMap<>(response.getData()); // Mutable copy
                }
                log.debug("For setting. Read existing secret at path '{}', current version {}.", path, versionForCas.getVersion());
            } else {
                versionForCas = Versioned.Version.unversioned(); // CAS 0 for creation
                log.info("Secret path '{}' does not exist or has no data/metadata. Will attempt to create with CAS version 0.", path);
            }

            // 2. Modify data
            data.put(key, value);

            // 3. Write with CAS by passing the expected version
            // Inside setSecret and deleteSecret, after modifying 'data'
            // Ensure you actually read a version
            kv2Ops.put("", Versioned.create(data, versionForCas)); // Pass Versioned object for CAS


            int expectedNewVersion = (versionForCas.isVersioned()) ? versionForCas.getVersion() + 1 : 1;
            log.info("Secret updated successfully using CAS at path '{}' for key (length {}). New version should be {}.", path, key.length(), expectedNewVersion);

        } catch (VaultException e) {
            handleVaultWriteException(e, path, key, "setting");
        } catch (Exception e) {
            log.error("Unexpected error storing secret key '{}' at path '{}'", key, path, e);
            throw new VaultOperationException("Unexpected error storing secret: " + key + " at path: " + path, e);
        }
    }

    /**
     * Deletes a secret key from Vault at a specific path using the KV v2 API with Check-and-Set (CAS).
     * Includes Retry and Circuit Breaker resilience patterns.
     *
     * @param path The Vault path (e.g., "secret/data/amlume-shop/old-config").
     * @param key  The key of the secret to delete.
     * @throws VaultOperationException if the operation fails after retries or the circuit is open.
     */
    @Retry(name = VAULT_RESILIENCE_CONFIG)
    @CircuitBreaker(name = VAULT_RESILIENCE_CONFIG, fallbackMethod = "writeSecretFallback")
    public void deleteSecret(String path, String key) {
        validatePathAndKey(path, key);
        try {
            VaultVersionedKeyValueOperations kv2Ops = vaultTemplate.opsForVersionedKeyValue(path);

            // 1. Read current data and metadata
            Versioned<Map<String, Object>> response = kv2Ops.get("");
            Map<String, Object> data = null;
            Versioned.Metadata metadata = (response != null) ? response.getMetadata() : null;
            Versioned.Version versionForCas = null;

            if (metadata != null) {
                versionForCas = metadata.getVersion();
                if (response.getData() != null) {
                    data = new HashMap<>(response.getData()); // Mutable copy
                }
                log.debug("For deletion. Read existing secret at path '{}', current version {}.", path, versionForCas.getVersion());
            }

            // 2. Check if the path/data exists and contains the key
            if (versionForCas == null || data == null || !data.containsKey(key)) {
                log.warn("Attempted to delete non-existent secret key '{}' from path '{}'. No changes made.", key, path);
                return; // Key doesn't exist, nothing to delete
            }

            // 3. Modify data
            data.remove(key);

            // 4. Write back with CAS by passing the expected version
            // If removing the last key makes the data map empty, Vault might delete the path version.
            // If you want to ensure the path remains with empty data, handle that case if needed.
            // Inside setSecret and deleteSecret, after modifying 'data'
            if (versionForCas != null) { // Ensure you actually read a version
                kv2Ops.put("", Versioned.create(data, versionForCas)); // Pass Versioned object for CAS
            } else {
                // Handle the case where the path didn't exist - CAS 0 write
                // VaultVersionedKeyValueOperations might handle this implicitly with put,
                // or you might need specific logic if Versioned.create needs a non-null version.
                // Let's assume put handles the creation case correctly for now, but verify.
                // A safer approach for creation might be:
                // kv2Ops.put("", Versioned.create(data)); // If path doesn't exist
                // Or handle based on specific VaultTemplate behavior for CAS 0.
                // For simplicity and focusing on the update case:
                kv2Ops.put("", Versioned.create(data, Versioned.Version.unversioned())); // Explicit CAS 0 if needed
            }


            int expectedNewVersion = versionForCas.getVersion() + 1;
            log.info("Secret deleted successfully using CAS at path '{}' for key (length {}). New version should be {}.", path, key.length(), expectedNewVersion);

        } catch (VaultException e) {
            handleVaultWriteException(e, path, key, "deleting");
        } catch (Exception e) {
            log.error("Unexpected error deleting secret key '{}' from path '{}'", key, path, e);
            throw new VaultOperationException("Unexpected error deleting secret: " + key + " from path: " + path, e);
        }
    }

    /**
     * Fallback method for setSecret and deleteSecret. Logs the error and throws an exception.
     */
    public void writeSecretFallback(String path, String key, String value, Throwable t) { // For setSecret
        log.error("VaultService write operation (setSecret) fallback triggered for path '{}', key '{}' due to: {}", path, key, t.getMessage());
        throw new VaultOperationException("Vault operation failed for path " + path + ", key " + key + " after retries or circuit open.", t);
    }

    public void writeSecretFallback(String path, String key, Throwable t) { // For deleteSecret
        log.error("VaultService write operation (deleteSecret) fallback triggered for path '{}', key '{}' due to: {}", path, key, t.getMessage());
        throw new VaultOperationException("Vault operation failed for path " + path + ", key " + key + " after retries or circuit open.", t);
    }

    // --- Helper Methods ---

    // Simplified validation just for a path
    private void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Vault path cannot be null or blank.");
        }
        // Basic path validation (adjust regex as needed)
        if (!path.matches("^[a-zA-Z0-9_\\-/]+$")) {
            throw new IllegalArgumentException("Invalid Vault path format: " + path);
        }
    }

    private void validatePathAndKey(String path, String key) {
        validatePath(path); // Reuse path validation
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Secret key cannot be null or blank.");
        }
        // Basic key validation
        if (!key.matches("^[a-zA-Z0-9_\\-.]+$")) {
            throw new IllegalArgumentException("Invalid secret key format: " + key);
        }
    }

    private void handleVaultWriteException(VaultException e, String path, String key, String operationType) {
        // Check specifically for CAS failure (a message might vary slightly depending on Vault version/config)
        String message = e.getMessage();
        if (message != null && (message.contains("check-and-set parameter did not match") || message.contains("status 400") || message.contains("status 412"))) {
            log.warn("CAS conflict while {} secret key '{}' at path '{}'. Concurrent modification detected.", operationType, key, path);
            // Don't necessarily retry CAS conflicts immediately, let the caller handle if needed.
            throw new VaultOperationException("Failed to " + operationType + " secret due to concurrent modification conflict: " + key + " at path: " + path, e);
        } else {
            log.error("Vault error {} secret key '{}' at path '{}': {}", operationType, key, path, e.getMessage());
            // Let Resilience4j handle other VaultExceptions based on configuration
            throw new VaultOperationException("Failed to " + operationType + " secret: " + key + " at path: " + path, e);
        }
    }
}

// Note: Ensure to adjust the regex patterns in validatePath and validatePathAndKey methods as per your Vault path/key naming conventions.

// Also, consider adding more specific exception handling based on your application's needs.
// This code is designed to be resilient and handle various scenarios, including transient errors, circuit breaking, and fallback mechanisms.
// The resilience patterns (Retry and Circuit Breaker) are applied to the methods where appropriate.
// The fallback methods are designed to handle failures gracefully, returning empty Optionals or throwing exceptions as needed.

// The logging statements are designed to provide detailed information about the operations and any errors encountered.
// Adjust the logging levels (DEBUG, INFO, WARN, ERROR) based on your application's logging strategy and requirements.
// Ensure to test the resilience patterns and fallback mechanisms thoroughly to ensure they behave as expected under various conditions.
// The VaultService class is designed to be a reusable component for interacting with HashiCorp Vault, providing methods for retrieving and storing secrets with resilience patterns applied.

// Usage example:

//@Service
//public class SomeOtherService {
//
//    private final VaultService vaultService;
//    private final String dbConfigPath = "secret/data/amlume-shop/database"; // KV v2 path
//
//    public SomeOtherService(VaultService vaultService) {
//        this.vaultService = vaultService;
//    }
//
//    public String getDatabasePassword() {
//        // Get the latest version
//        return vaultService.getSecret(dbConfigPath, "password");
//        // Or get a specific version
//        // return vaultService.getSpecificSecret(dbConfigPath, "password", 3);
//    }
//
//    public void updateApiKey(String newKey) {
//        String apiPath = "secret/data/amlume-shop/api-keys"; // KV v2 path
//        vaultService.setSecret(apiPath, "external-service-key", newKey);
//    }
//}
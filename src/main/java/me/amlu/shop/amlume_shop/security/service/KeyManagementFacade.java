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

import me.amlu.shop.amlume_shop.exceptions.KeyManagementException;
import me.amlu.shop.amlume_shop.security.model.KeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
// Removed Map, TimeUnit, ExecutionException imports

@Service
public class KeyManagementFacade {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementFacade.class);

    // --- Injected Secret Strings from Vault/Environment ---
    // NOTE: Ensure these property names match the keys in your Vault path (e.g., secret/<application-name>/mfa)

    // Access Keys (Asymmetric - Public/Private)
    @Value("${paseto-access-private-key}")
    private String pasetoAccessPrivateKey;

    @Value("${paseto-access-public-key}")
    private String pasetoAccessPublicKey;

    // Refresh Keys (Symmetric - Secret) - Assuming symmetric for refresh based on original code
    @Value("${paseto-refresh-secret-key}")
    private String pasetoRefreshSecretKey;

    // Access Keys (Symmetric - Secret) - If you also use symmetric for access
    @Value("${paseto-access-secret-key}")
    private String pasetoAccessSecretKey; // Added based on KeyManagementService usage

    // Other secrets if needed by this facade or passed through
    @Value("${mfa-encryption-password}")
    private String mfaEncryptionPassword;

    // Removed HCPSecretsService dependency and Cache

    // Constructor is now simpler or can be removed if no other dependencies
    public KeyManagementFacade() {
        // No dependencies to inject here anymore
        log.info("KeyManagementFacade initialized. Secrets will be injected via @Value.");
    }

    // Removed initialize() and loadSecrets() - @Value handles loading

    /**
     * Retrieves the asymmetric key pair strings for the given purpose.
     * Now directly returns the injected values.
     *
     * @param purpose Typically "ACCESS" or potentially others if defined.
     * @return KeyPair record containing the private and public key strings.
     * @throws KeyManagementException if required keys for the purpose are not injected/found.
     */
    public KeyPair getAsymmetricKeys(String purpose) {
        log.debug("Retrieving asymmetric keys for purpose: {}", purpose);
        // Simple example: Assuming only "ACCESS" uses asymmetric keys based on original code
        if ("ACCESS".equalsIgnoreCase(purpose)) {
            if (pasetoAccessPrivateKey == null || pasetoAccessPublicKey == null) {
                log.error("Asymmetric keys for purpose '{}' are not available (check Vault/environment variables: paseto-access-private-key, paseto-access-public-key)", purpose);
                throw new KeyManagementException("Asymmetric keys for purpose '" + purpose + "' not found.");
            }
            return new KeyPair(pasetoAccessPrivateKey, pasetoAccessPublicKey);
        } else {
            // Handle other purposes if necessary, or throw an exception
            log.warn("Asymmetric keys requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for asymmetric keys: " + purpose);
        }
    }

    /**
     * Retrieves the symmetric secret key string for the given purpose.
     * Now directly returns the injected value.
     *
     * @param purpose Typically "ACCESS" or "REFRESH".
     * @return The secret key string.
     * @throws KeyManagementException if the required key for the purpose is not injected/found.
     */
    public String getSymmetricKey(String purpose) {
        log.debug("Retrieving symmetric key for purpose: {}", purpose);
        String key = null;
        String propertyName = "unknown";

        if ("ACCESS".equalsIgnoreCase(purpose)) {
            key = pasetoAccessSecretKey; // Use the symmetric access key if needed
            propertyName = "paseto-access-secret-key";
        } else if ("REFRESH".equalsIgnoreCase(purpose)) {
            key = pasetoRefreshSecretKey;
            propertyName = "paseto-refresh-secret-key";
        } else {
            log.warn("Symmetric key requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for symmetric key: " + purpose);
        }

        if (key == null) {
            log.error("Symmetric key for purpose '{}' is not available (check Vault/environment variable: {})", purpose, propertyName);
            throw new KeyManagementException("Symmetric key for purpose '" + purpose + "' not found.");
        }
        return key;
    }

    // --- Optional: Getter for other injected secrets if needed elsewhere ---
    public String getMfaEncryptionPassword() {
        if (mfaEncryptionPassword == null) {
            log.error("MFA encryption password is not available (check Vault/environment variable: mfa-encryption-password)");
            throw new KeyManagementException("MFA encryption password not found.");
        }
        return mfaEncryptionPassword;
    }


    // Removed getCachedKey(), updateKeys(), refreshKeys()
}
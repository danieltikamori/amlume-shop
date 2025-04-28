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

import me.amlu.shop.amlume_shop.config.properties.PasetoProperties; // Import PasetoProperties
import me.amlu.shop.amlume_shop.exceptions.KeyManagementException;
import me.amlu.shop.amlume_shop.security.model.KeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Keep for mfaEncryptionPassword if needed
import org.springframework.stereotype.Service;

import java.util.Objects; // Import Objects for null checks

@Service
public class KeyManagementFacade {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementFacade.class);

    // --- Injected Paseto Properties Bean ---
    private final PasetoProperties pasetoProperties;

    // --- Other secrets if needed by this facade or passed through ---
    // Keep @Value for secrets NOT managed within PasetoProperties
    @Value("${mfa-encryption-password}")
    private String mfaEncryptionPassword;

    // Constructor updated to inject PasetoProperties
    public KeyManagementFacade(PasetoProperties pasetoProperties) {
        this.pasetoProperties = pasetoProperties;
        log.info("KeyManagementFacade initialized. PASETO secrets will be retrieved via PasetoProperties bean.");
    }

    /**
     * Retrieves the asymmetric key pair strings for the given purpose.
     * Now retrieves values from the injected PasetoProperties bean.
     *
     * @param purpose Typically "ACCESS" or potentially "REFRESH" if asymmetric refresh keys are used.
     * @return KeyPair record containing the private and public key strings.
     * @throws KeyManagementException if required keys for the purpose are not configured.
     */
    public KeyPair getAsymmetricKeys(String purpose) {
        log.debug("Retrieving asymmetric keys for purpose: {}", purpose);
        String privateKey;
        String publicKey;
        String privateKeyPropertyPath;
        String publicKeyPropertyPath;

        if ("ACCESS".equalsIgnoreCase(purpose)) {
            privateKey = pasetoProperties.getAccessPrivateKey();
            publicKey = pasetoProperties.getAccessPublicKey();
            privateKeyPropertyPath = "paseto.access.public.private-key";
            publicKeyPropertyPath = "paseto.access.public.public-key";
        } else if ("REFRESH".equalsIgnoreCase(purpose)) {
            // Assuming REFRESH might also use asymmetric keys (adjust if not)
            privateKey = pasetoProperties.getRefreshPrivateKey();
            publicKey = pasetoProperties.getRefreshPublicKey();
            privateKeyPropertyPath = "paseto.refresh.public.private-key";
            publicKeyPropertyPath = "paseto.refresh.public.public-key";
        } else {
            log.warn("Asymmetric keys requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for asymmetric keys: " + purpose);
        }

        // Use Objects.requireNonNull for cleaner null checks
        Objects.requireNonNull(privateKey, "Private key for purpose '" + purpose + "' is not available (check Vault/environment variable/YAML path: " + privateKeyPropertyPath + ")");
        Objects.requireNonNull(publicKey, "Public key for purpose '" + purpose + "' is not available (check Vault/environment variable/YAML path: " + publicKeyPropertyPath + ")");

        return new KeyPair(privateKey, publicKey);
    }

    /**
     * Retrieves the symmetric secret key string for the given purpose.
     * Now retrieves values from the injected PasetoProperties bean.
     *
     * @param purpose Typically "ACCESS" or "REFRESH".
     * @return The secret key string.
     * @throws KeyManagementException if the required key for the purpose is not configured.
     */
    public String getSymmetricKey(String purpose) {
        log.debug("Retrieving symmetric key for purpose: {}", purpose);
        String key;
        String propertyName; // For logging the expected property path

        if ("ACCESS".equalsIgnoreCase(purpose)) {
            key = pasetoProperties.getAccessSecretKey();
            propertyName = "paseto.access.local.secret-key";
        } else if ("REFRESH".equalsIgnoreCase(purpose)) {
            key = pasetoProperties.getRefreshSecretKey();
            propertyName = "paseto.refresh.local.secret-key";
        } else {
            log.warn("Symmetric key requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for symmetric key: " + purpose);
        }

        // Use Objects.requireNonNull for cleaner null check
        Objects.requireNonNull(key, "Symmetric key for purpose '" + purpose + "' is not available (check Vault/environment variable/YAML path: " + propertyName + ")");

        return key;
    }

    // --- Optional: Getter for other injected secrets if needed elsewhere ---
    public String getMfaEncryptionPassword() {
        // Use Objects.requireNonNull
        Objects.requireNonNull(mfaEncryptionPassword, "MFA encryption password is not available (check Vault/environment variable: mfa-encryption-password)");
        return mfaEncryptionPassword;
    }

    // Removed getCachedKey(), updateKeys(), refreshKeys()
}
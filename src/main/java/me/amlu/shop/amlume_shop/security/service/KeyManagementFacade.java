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

import jakarta.annotation.PostConstruct;
import me.amlu.shop.amlume_shop.config.properties.MfaProperties;
import me.amlu.shop.amlume_shop.config.properties.PasetoProperties;
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

    private final MfaProperties mfaProperties;

    // --- Other secrets if needed by this facade or passed through ---
    // Keep @Value for secrets NOT managed within PasetoProperties

    // DONE: SECURITY - Move MFA password to Vault (e.g., secret/amlume-shop/mfa)
    //       and inject via a dedicated @ConfigurationProperties bean (e.g., MfaProperties)
    //       populated by Spring Cloud Vault instead of using @Value.
//    private String mfaEncryptionPassword;

    // Constructor updated to inject PasetoProperties
    public KeyManagementFacade(PasetoProperties pasetoProperties, MfaProperties mfaProperties) {
        this.pasetoProperties = pasetoProperties;
        this.mfaProperties = mfaProperties;
        log.info("KeyManagementFacade initialized. PASETO secrets will be retrieved via PasetoProperties bean.");
    }

    // --- TEMPORARY DEBUG ---
    @PostConstruct
    public void checkInjectedProperties() {
        log.info("--- KeyManagementFacade PostConstruct ---");
        log.info("MFA Encryption Password (@Value): '{}'", mfaProperties.getMfaEncryptionPassword() != null ? "[REDACTED]" : "null");

        if (pasetoProperties == null) {
            log.error("PasetoProperties bean is NULL in KeyManagementFacade!");
        } else {
            log.info("PasetoProperties: {}", pasetoProperties); // Log the whole structure (uses updated toString)

            // More specific checks using the new structure
            if (pasetoProperties.getPub() != null && pasetoProperties.getPub().getAccess() != null) {
                log.info("PasetoProperties.pub.access.privateKey is null? {}", pasetoProperties.getPub().getAccess().getPrivateKey() == null);
            } else {
                log.warn("PasetoProperties.pub or PasetoProperties.pub.access is NULL");
            }
            if (pasetoProperties.getLocal() != null && pasetoProperties.getLocal().getAccess() != null) {
                log.info("PasetoProperties.local.access.secretKey is null? {}", pasetoProperties.getLocal().getAccess().getSecretKey() == null);
            } else {
                log.warn("PasetoProperties.local or PasetoProperties.local.access is NULL");
            }
            // Add checks for refresh keys if needed
        }
        log.info("--- End KeyManagementFacade PostConstruct ---");
    }
    // --- END TEMPORARY DEBUG ---

    /**
     * Retrieves the asymmetric key pair strings for the given purpose.
     * Now retrieves values from the injected PasetoProperties bean using nested access.
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

        // Use nested accessors
        if ("ACCESS".equalsIgnoreCase(purpose)) {
            privateKey = pasetoProperties.getPub().getAccess().getPrivateKey();
            publicKey = pasetoProperties.getPub().getAccess().getPublicKey();
            privateKeyPropertyPath = "paseto.public.access.private-key";
            publicKeyPropertyPath = "paseto.public.access.public-key";  
        } else if ("REFRESH".equalsIgnoreCase(purpose)) {
            // Assuming REFRESH might also use asymmetric keys
            privateKey = pasetoProperties.getPub().getRefresh().getPrivateKey();
            publicKey = pasetoProperties.getPub().getRefresh().getPublicKey();
            privateKeyPropertyPath = "paseto.public.refresh.private-key";
            publicKeyPropertyPath = "paseto.public.refresh.public-key";  
        } else {
            log.warn("Asymmetric keys requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for asymmetric keys: " + purpose);
        }

        // Use Objects.requireNonNull for cleaner null check
        Objects.requireNonNull(privateKey, "Private key for purpose '" + purpose + "' is not available (check Vault/config path: " + privateKeyPropertyPath + ")");
        Objects.requireNonNull(publicKey, "Public key for purpose '" + purpose + "' is not available (check Vault/config path: " + publicKeyPropertyPath + ")");

        return new KeyPair(privateKey, publicKey);
    }

    /**
     * Retrieves the symmetric secret key string for the given purpose.
     * Now retrieves values from the injected PasetoProperties bean using nested access.
     *
     * @param purpose Typically "ACCESS" or "REFRESH".
     * @return The secret key string.
     * @throws KeyManagementException if the required key for the purpose is not configured.
     */
    public String getSymmetricKey(String purpose) {
        log.debug("Retrieving symmetric key for purpose: {}", purpose);
        String key;
        String propertyName;

        // Use NEW nested accessors
        if ("ACCESS".equalsIgnoreCase(purpose)) {
            key = pasetoProperties.getLocal().getAccess().getSecretKey();
            propertyName = "paseto.local.access.secret-key";
        } else if ("REFRESH".equalsIgnoreCase(purpose)) {
            key = pasetoProperties.getLocal().getRefresh().getSecretKey();
            propertyName = "paseto.local.refresh.secret-key";
        } else {
            log.warn("Symmetric key requested for unsupported purpose: {}", purpose);
            throw new KeyManagementException("Unsupported purpose for symmetric key: " + purpose);
        }

        // Use Objects.requireNonNull for cleaner null check
        Objects.requireNonNull(key, "Symmetric key for purpose '" + purpose + "' is not available (check Vault/config path: " + propertyName + ")");

        return key;
    }


    // --- Optional: Getter for other injected secrets if needed elsewhere ---
    public String getMfaEncryptionPassword() {
        // Use Objects.requireNonNull
        Objects.requireNonNull(mfaProperties.getMfaEncryptionPassword(), "MFA encryption password is not available (check Vault/environment variable/YAML path: mfa.encryption.password)");
        return mfaProperties.getMfaEncryptionPassword();
    }

    // Removed getCachedKey(), updateKeys(), refreshKeys()
}
/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import me.amlu.shop.amlume_shop.exceptions.KeyConversionException; // Keep for potential Base64 errors
import me.amlu.shop.amlume_shop.exceptions.KeyInitializationException;
import me.amlu.shop.amlume_shop.security.model.KeyPair; // Use the KeyPair record from facade
import me.amlu.shop.amlume_shop.security.service.KeyManagementFacade;
import org.paseto4j.commons.Version; // Import Version again

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Import paseto4j key types
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;

import java.util.Base64;
import java.util.Objects;

@Service
public class KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementService.class);
    private final KeyManagementFacade keyManagementFacade;

    // Use volatile for thread safety with double-checked locking pattern
    private volatile KeyPairHolder accessKeyPairHolder;
    // Add holders for refresh keys if they are asymmetric too
    // private volatile KeyPairHolder refreshKeyPairHolder;
    private volatile SecretKeyHolder secretKeyHolder;
    private final Object initLock = new Object(); // Lock object for synchronization

    // Constructor injection
    public KeyManagementService(KeyManagementFacade keyManagementFacade) {
        this.keyManagementFacade = keyManagementFacade;
        log.info("KeyManagementService initialized. Keys will be loaded lazily using paseto4j types.");
    }

    // --- Lazy Initialization Method ---
    private void ensureKeysInitialized() {
        // Double-checked locking pattern
        if (accessKeyPairHolder == null || secretKeyHolder == null /* || add other holders */) {
            synchronized (initLock) {
                // Check again inside synchronized block
                if (accessKeyPairHolder == null || secretKeyHolder == null /* || add other holders */) {
                    log.info("Lazily initializing PASETO keys using paseto4j types...");
                    try {
                        // --- Asymmetric Keys (ACCESS) ---
                        KeyPair accessKeyStrings = keyManagementFacade.getAsymmetricKeys("ACCESS");
                        Objects.requireNonNull(accessKeyStrings.privateKey(), "Access private key string is null");
                        Objects.requireNonNull(accessKeyStrings.publicKey(), "Access public key string is null");

                        // Use deprecated paseto4j Key constructors (accepting byte[] and Version)
                        // Acknowledging the deprecation warning as it's the only way shown to create from bytes.
                        PrivateKey pasetoAccessPrivateKey = new PrivateKey(Base64.getDecoder().decode(accessKeyStrings.privateKey()), Version.V4);
                        PublicKey pasetoAccessPublicKey = new PublicKey(Base64.getDecoder().decode(accessKeyStrings.publicKey()), Version.V4);

                        this.accessKeyPairHolder = new KeyPairHolder(pasetoAccessPrivateKey, pasetoAccessPublicKey);
                        log.debug("Access asymmetric keys loaded and created using paseto4j constructors.");

                        // --- Asymmetric Keys (REFRESH - if applicable) ---
                        // KeyPair refreshKeyStrings = keyManagementFacade.getAsymmetricKeys("REFRESH");
                        // PrivateKey pasetoRefreshPrivateKey = new PrivateKey(Base64.getDecoder().decode(refreshKeyStrings.privateKey()), Version.V4); // Use deprecated
                        // PublicKey pasetoRefreshPublicKey = new PublicKey(Base64.getDecoder().decode(refreshKeyStrings.publicKey()), Version.V4); // Use deprecated
                        // this.refreshKeyPairHolder = new KeyPairHolder(pasetoRefreshPrivateKey, pasetoRefreshPublicKey);
                        // log.debug("Refresh asymmetric keys loaded and created using paseto4j constructors.");

                        // --- Symmetric Keys ---
                        String accessSecretStr = keyManagementFacade.getSymmetricKey("ACCESS");
                        String refreshSecretStr = keyManagementFacade.getSymmetricKey("REFRESH");
                        Objects.requireNonNull(accessSecretStr, "Access secret key string is null");
                        Objects.requireNonNull(refreshSecretStr, "Refresh secret key string is null");

                        // Use deprecated paseto4j Key constructors (accepting byte[] and Version)
                        SecretKey pasetoAccessSecretKey = new SecretKey(Base64.getDecoder().decode(accessSecretStr), Version.V4);
                        SecretKey pasetoRefreshSecretKey = new SecretKey(Base64.getDecoder().decode(refreshSecretStr), Version.V4);

                        this.secretKeyHolder = new SecretKeyHolder(pasetoAccessSecretKey, pasetoRefreshSecretKey);
                        log.debug("Symmetric keys loaded and created using paseto4j constructors.");

                        log.info("PASETO keys initialized successfully (lazily).");

                    } catch (IllegalArgumentException e) { // Catch Base64 decoding errors or constructor errors
                        log.error("Failed to decode/construct key during lazy initialization", e);
                        throw new KeyInitializationException("Invalid key string or format", e);
                    } catch (Exception e) { // Catch other potential errors
                        log.error("Failed to lazily initialize PASETO keys", e);
                        throw new KeyInitializationException("Failed to initialize PASETO keys during lazy loading", e);
                    }
                }
            }
        }
    }


    // --- Getter methods now ensure initialization and return paseto4j types ---

    public PrivateKey getAccessPrivateKey() { // Return paseto4j PrivateKey
        ensureKeysInitialized();
        Objects.requireNonNull(accessKeyPairHolder, "Access KeyPairHolder not initialized after lazy load attempt");
        Objects.requireNonNull(accessKeyPairHolder.privateKey(), "Access PrivateKey not initialized after lazy load attempt");
        return accessKeyPairHolder.privateKey();
    }

    public PublicKey getAccessPublicKey() { // Return paseto4j PublicKey
        ensureKeysInitialized();
        Objects.requireNonNull(accessKeyPairHolder, "Access KeyPairHolder not initialized after lazy load attempt");
        Objects.requireNonNull(accessKeyPairHolder.publicKey(), "Access PublicKey not initialized after lazy load attempt");
        return accessKeyPairHolder.publicKey();
    }

    public SecretKey getAccessSecretKey() { // Return paseto4j SecretKey
        ensureKeysInitialized();
        Objects.requireNonNull(secretKeyHolder, "SecretKeyHolder not initialized after lazy load attempt");
        Objects.requireNonNull(secretKeyHolder.accessKey(), "Access SecretKey not initialized after lazy load attempt");
        return secretKeyHolder.accessKey();
    }

    public SecretKey getRefreshSecretKey() { // Return paseto4j SecretKey
        ensureKeysInitialized();
        Objects.requireNonNull(secretKeyHolder, "SecretKeyHolder not initialized after lazy load attempt");
        Objects.requireNonNull(secretKeyHolder.refreshKey(), "Refresh SecretKey not initialized after lazy load attempt");
        return secretKeyHolder.refreshKey();
    }

    // --- Internal Holder Records (Update types) ---
    private record KeyPairHolder(PrivateKey privateKey, PublicKey publicKey) {} // Use paseto4j types
    private record SecretKeyHolder(SecretKey accessKey, SecretKey refreshKey) {} // Use paseto4j types
}
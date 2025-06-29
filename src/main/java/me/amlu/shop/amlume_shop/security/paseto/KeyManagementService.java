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

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
        if (accessKeyPairHolder == null || secretKeyHolder == null) {
            synchronized (initLock) {
                if (accessKeyPairHolder == null || secretKeyHolder == null) {
                    log.info("Lazily initializing PASETO keys using paseto4j types...");
                    try {
                        // --- Asymmetric Keys (ACCESS) ---
                        KeyPair accessKeyStrings = keyManagementFacade.getAsymmetricKeys("ACCESS");
                        Objects.requireNonNull(accessKeyStrings.privateKey(), "Access private key string is null");
                        Objects.requireNonNull(accessKeyStrings.publicKey(), "Access public key string is null");

                        // 1. Decode Base64 DER strings
                        byte[] accessPrivateDerBytes = Base64.getDecoder().decode(accessKeyStrings.privateKey());
                        byte[] accessPublicDerBytes = Base64.getDecoder().decode(accessKeyStrings.publicKey());

                        // 2. Parse DER bytes into java.security keys
                        KeyFactory keyFactory = KeyFactory.getInstance("EdDSA"); // Algorithm for Ed25519

                        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(accessPrivateDerBytes);
                        java.security.PrivateKey javaPrivateKey = keyFactory.generatePrivate(privateKeySpec);

                        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(accessPublicDerBytes);
                        java.security.PublicKey javaPublicKey = keyFactory.generatePublic(publicKeySpec);

                        // 3. Create paseto4j keys using the non-deprecated constructors
                        PrivateKey pasetoAccessPrivateKey = new PrivateKey(javaPrivateKey, Version.V4);
                        PublicKey pasetoAccessPublicKey = new PublicKey(javaPublicKey, Version.V4);
                        log.debug("Successfully parsed DER and created paseto4j asymmetric keys.");

                        this.accessKeyPairHolder = new KeyPairHolder(pasetoAccessPrivateKey, pasetoAccessPublicKey);
                        log.debug("Access asymmetric keys loaded and created.");

                        // --- Symmetric Keys (Logic remains the same) ---
                        String accessSecretStr = keyManagementFacade.getSymmetricKey("ACCESS");
                        String refreshSecretStr = keyManagementFacade.getSymmetricKey("REFRESH");
                        Objects.requireNonNull(accessSecretStr, "Access secret key string is null");
                        Objects.requireNonNull(refreshSecretStr, "Refresh secret key string is null");

                        byte[] accessSecretBytes = Base64.getDecoder().decode(accessSecretStr);
                        byte[] refreshSecretBytes = Base64.getDecoder().decode(refreshSecretStr);

                        // Use the constructor expecting raw bytes for SecretKey
                        SecretKey pasetoAccessSecretKey = new SecretKey(accessSecretBytes, Version.V4);
                        SecretKey pasetoRefreshSecretKey = new SecretKey(refreshSecretBytes, Version.V4);
                        // Keep warning if factory methods are preferred but unknown
                        log.warn("TODO: Verify if paseto4j v4 offers factory methods for SecretKey creation.");

                        this.secretKeyHolder = new SecretKeyHolder(pasetoAccessSecretKey, pasetoRefreshSecretKey);
                        log.debug("Symmetric keys loaded and created.");

                        log.info("PASETO keys initialized successfully (lazily).");

                    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                        // Catch Base64 errors, KeyFactory errors, KeySpec errors
                        log.error("Failed to decode/parse/construct key during lazy initialization", e);
                        throw new KeyInitializationException("Invalid key string, format, or algorithm issue", e);
                    } catch (Exception e) { // Catch any other unexpected errors
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

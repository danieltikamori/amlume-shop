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

import me.amlu.shop.amlume_shop.exceptions.KeyConversionException;
import me.amlu.shop.amlume_shop.exceptions.KeyInitializationException;
import me.amlu.shop.amlume_shop.security.model.KeyPair; // Use the KeyPair record from facade
import me.amlu.shop.amlume_shop.security.service.KeyManagementFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

@Service
public class KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagementService.class);
    private final KeyManagementFacade keyManagementFacade;
    private static final KeyFactory KEY_FACTORY_ED25519;

    // Use volatile for thread safety with double-checked locking pattern
    private volatile KeyPairHolder accessKeyPairHolder;
    // Add holders for refresh keys if they are asymmetric too
    // private volatile KeyPairHolder refreshKeyPairHolder;
    private volatile SecretKeyHolder secretKeyHolder;
    private final Object initLock = new Object(); // Lock object for synchronization

    static {
        try {
            // Initialize the specific KeyFactory needed (e.g., Ed25519 for v4.public)
            KEY_FACTORY_ED25519 = KeyFactory.getInstance("Ed25519");
            log.info("Initialized Ed25519 KeyFactory.");
        } catch (NoSuchAlgorithmException e) {
            log.error("FATAL: Ed25519 algorithm not supported by the security provider!", e);
            throw new IllegalStateException("Required Ed25519 algorithm not available", e);
        }
    }

    // Constructor injection
    public KeyManagementService(KeyManagementFacade keyManagementFacade) {
        this.keyManagementFacade = keyManagementFacade;
        log.info("KeyManagementService initialized. Keys will be loaded lazily.");
    }

    // --- REMOVED @PostConstruct initializeKeys() method ---

    // --- Lazy Initialization Method ---
    private void ensureKeysInitialized() {
        // Double-checked locking pattern to minimize synchronization overhead
        if (accessKeyPairHolder == null || secretKeyHolder == null /* || add other holders */) {
            synchronized (initLock) {
                // Check again inside synchronized block to prevent redundant initialization
                if (accessKeyPairHolder == null || secretKeyHolder == null /* || add other holders */) {
                    log.info("Lazily initializing PASETO keys...");
                    try {
                        // --- Asymmetric Keys (ACCESS) ---
                        // Now this call happens later, hopefully after PasetoProperties is fully bound
                        KeyPair accessKeys = keyManagementFacade.getAsymmetricKeys("ACCESS");
                        PrivateKey accessPrivateKey = convertToPrivateKey(accessKeys.privateKey());
                        PublicKey accessPublicKey = convertToPublicKey(accessKeys.publicKey());
                        this.accessKeyPairHolder = new KeyPairHolder(accessPrivateKey, accessPublicKey);
                        log.debug("Access asymmetric keys loaded and converted.");

                        // --- Asymmetric Keys (REFRESH - if applicable) ---
                        // Uncomment and adapt if refresh tokens also use public keys
                        // KeyPair refreshKeys = keyManagementFacade.getAsymmetricKeys("REFRESH");
                        // PrivateKey refreshPrivateKey = convertToPrivateKey(refreshKeys.privateKey());
                        // PublicKey refreshPublicKey = convertToPublicKey(refreshKeys.publicKey());
                        // this.refreshKeyPairHolder = new KeyPairHolder(refreshPrivateKey, refreshPublicKey);
                        // log.debug("Refresh asymmetric keys loaded and converted.");

                        // --- Symmetric Keys ---
                        String accessSecretStr = keyManagementFacade.getSymmetricKey("ACCESS");
                        String refreshSecretStr = keyManagementFacade.getSymmetricKey("REFRESH");
                        SecretKey accessSecretKey = convertToSecretKey(accessSecretStr);
                        SecretKey refreshSecretKey = convertToSecretKey(refreshSecretStr);
                        this.secretKeyHolder = new SecretKeyHolder(accessSecretKey, refreshSecretKey);
                        log.debug("Symmetric keys loaded and converted.");

                        log.info("PASETO keys initialized successfully (lazily).");

                    } catch (Exception e) {
                        log.error("Failed to lazily initialize PASETO keys", e);
                        // Wrap in a runtime exception to indicate critical failure during lazy load
                        throw new KeyInitializationException("Failed to initialize PASETO keys during lazy loading", e);
                    }
                }
            }
        }
    }


    // --- Getter methods now ensure initialization ---

    public PrivateKey getAccessPrivateKey() {
        ensureKeysInitialized(); // Ensure keys are loaded before returning
        Objects.requireNonNull(accessKeyPairHolder, "Access KeyPairHolder not initialized after lazy load attempt");
        Objects.requireNonNull(accessKeyPairHolder.privateKey(), "Access PrivateKey not initialized after lazy load attempt");
        return accessKeyPairHolder.privateKey();
    }

    public PublicKey getAccessPublicKey() {
        ensureKeysInitialized(); // Ensure keys are loaded before returning
        Objects.requireNonNull(accessKeyPairHolder, "Access KeyPairHolder not initialized after lazy load attempt");
        Objects.requireNonNull(accessKeyPairHolder.publicKey(), "Access PublicKey not initialized after lazy load attempt");
        return accessKeyPairHolder.publicKey();
    }

    public SecretKey getAccessSecretKey() {
        ensureKeysInitialized(); // Ensure keys are loaded before returning
        Objects.requireNonNull(secretKeyHolder, "SecretKeyHolder not initialized after lazy load attempt");
        Objects.requireNonNull(secretKeyHolder.accessKey(), "Access SecretKey not initialized after lazy load attempt");
        return secretKeyHolder.accessKey();
    }

    public SecretKey getRefreshSecretKey() {
        ensureKeysInitialized(); // Ensure keys are loaded before returning
        Objects.requireNonNull(secretKeyHolder, "SecretKeyHolder not initialized after lazy load attempt");
        Objects.requireNonNull(secretKeyHolder.refreshKey(), "Refresh SecretKey not initialized after lazy load attempt");
        return secretKeyHolder.refreshKey();
    }

    // --- Conversion Methods (Ensure these handle potential nulls/errors) ---

    private PrivateKey convertToPrivateKey(String keyString) throws KeyConversionException {
        Objects.requireNonNull(keyString, "Private key string cannot be null for conversion");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KEY_FACTORY_ED25519.generatePrivate(keySpec);
        } catch (InvalidKeySpecException | IllegalArgumentException e) {
            log.error("Failed to convert string to Ed25519 PrivateKey: {}", e.getMessage());
            throw new KeyConversionException("Invalid private key format or data", e);
        }
    }

    private PublicKey convertToPublicKey(String keyString) throws KeyConversionException {
        Objects.requireNonNull(keyString, "Public key string cannot be null for conversion");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return KEY_FACTORY_ED25519.generatePublic(keySpec);
        } catch (InvalidKeySpecException | IllegalArgumentException e) {
            log.error("Failed to convert string to Ed25519 PublicKey: {}", e.getMessage());
            throw new KeyConversionException("Invalid public key format or data", e);
        }
    }

    private SecretKey convertToSecretKey(String keyString) throws KeyConversionException {
        Objects.requireNonNull(keyString, "Secret key string cannot be null for conversion");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            // v4.local uses a 32-byte key
            if (keyBytes.length != 32) {
                log.warn("Expected 32-byte secret key for v4.local, but got {} bytes.", keyBytes.length);
                // Consider throwing if strict length is required by your PASETO library
                // throw new KeyConversionException("Invalid secret key length: Expected 32 bytes, got " + keyBytes.length);
            }
            // Algorithm name for SecretKeySpec is often "AES" for libraries using AES-GCM.
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 secret key string: {}", e.getMessage());
            throw new KeyConversionException("Invalid Base64 encoding for secret key", e);
        }
    }

    // --- Internal Holder Records ---
    // Using records for simple, immutable data carriers
    private record KeyPairHolder(PrivateKey privateKey, PublicKey publicKey) {}
    private record SecretKeyHolder(SecretKey accessKey, SecretKey refreshKey) {}
}
    
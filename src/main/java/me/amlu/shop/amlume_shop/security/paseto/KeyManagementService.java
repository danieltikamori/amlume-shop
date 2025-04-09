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

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.ErrorMessages;
import me.amlu.shop.amlume_shop.exceptions.KeyConversionException;
import me.amlu.shop.amlume_shop.exceptions.KeyFactoryException;
import me.amlu.shop.amlume_shop.exceptions.KeyInitializationException;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants;
import me.amlu.shop.amlume_shop.security.service.KeyManagementFacade;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyManagementService {
    private final KeyManagementFacade keyManagementFacade;
    private static final KeyFactory KEY_FACTORY = initializeKeyFactory();

    // Key holders - immutable
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class KeyPairHolder {
        PrivateKey privateKey;
        PublicKey publicKey;

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class SecretKeyHolder {
        SecretKey accessKey;
        SecretKey refreshKey;
    }

    // Key management
    private KeyPairHolder keyPairHolder;
    private SecretKeyHolder secretKeyHolder;

    // --- Key accessors ---
    // IMPORTANT: Getters for keys with null checks

    /**
     * Usage example:
     * public String signToken(String payload) {
     * try {
     * return Paseto.sign(payload, getAccessPrivateKey());
     * } catch (Exception e) {
     * throw new TokenSigningException("Failed to sign token", e);
     * }
     * }
     * <p>
     * public boolean verifyToken(String token) {
     * try {
     * return Paseto.verify(token, getAccessPublicKey());
     * } catch (Exception e) {
     * throw new TokenVerificationException("Failed to verify token", e);
     * }
     * }
     *
     * @return the access private key
     */
    protected PrivateKey getAccessPrivateKey() {
        return Objects.requireNonNull(keyPairHolder.getPrivateKey(), "Access private key not initialized");
    }

    protected PublicKey getAccessPublicKey() {
        return Objects.requireNonNull(keyPairHolder.getPublicKey(), "Access public key not initialized");
    }

    protected SecretKey getAccessSecretKey() {
        return Objects.requireNonNull(secretKeyHolder.getAccessKey(), "Access secret key not initialized");
    }

    protected SecretKey getRefreshSecretKey() {
        return Objects.requireNonNull(secretKeyHolder.getRefreshKey(), "Refresh secret key not initialized");
    }

    @PostConstruct
    private void initializeKeys() {
        try {
            // Initialize asymmetric keys
            KeyManagementFacade.KeyPair accessKeys = keyManagementFacade.getAsymmetricKeys("ACCESS");
            this.keyPairHolder = new KeyPairHolder(
                    convertToPrivateKey(validateKey(accessKeys.privateKey(), "private")),
                    convertToPublicKey(validateKey(accessKeys.publicKey(), "public"),"access")
            );

            // Initialize symmetric keys
            this.secretKeyHolder = new SecretKeyHolder(
                    convertToSecretKey(validateKey(keyManagementFacade.getSymmetricKey("ACCESS"), "access secret")),
                    convertToSecretKey(validateKey(keyManagementFacade.getSymmetricKey("REFRESH"), "refresh secret"))
            );
        } catch (Exception e) {
            log.error("Failed to initialize keys", e);
            throw new KeyInitializationException("Failed to initialize keys", e);
        }
    }
    /**
     * Initializes the key factory for the specified algorithm.
     * This method is called during the initialization of the service.
     * It uses the KeyFactory.getInstance() method to get an instance of the KeyFactory for the specified algorithm.
     *
     * @return a {@link KeyFactory} instance for the specified algorithm.
     * @throws KeyConversionException   if the algorithm is not available.
//     * @throws NoSuchAlgorithmException if the algorithm is not available.
//     * @throws InvalidKeySpecException  if the key specification is invalid.
     * @throws KeyFactoryException      if the key factory is not available.
     */
    private static KeyFactory initializeKeyFactory() throws KeyConversionException, KeyFactoryException{
        try {
            return KeyFactory.getInstance(TokenConstants.KEY_CONVERSION_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error(String.valueOf(ErrorMessages.KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
            throw new KeyConversionException(String.valueOf(ErrorMessages.KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
        } catch (KeyFactoryException e) {
            throw new IllegalStateException("Failed to initialize KeyFactory", e);
        }
    }
    /**
     * Converts a base64 encoded private key string to a {@link PrivateKey} object.
     *
     * @param privateKeyString the base64 encoded private key string
     * @return a {@link PrivateKey} object
     * @throws KeyConversionException if the private key string is invalid
     */
    public PrivateKey convertToPrivateKey(String privateKeyString) {
        try {
            if (privateKeyString == null || privateKeyString.isEmpty()) {
                throw new IllegalArgumentException("Private key string cannot be null or empty");
            }

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            return new PrivateKey(
                    KEY_FACTORY.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)),
                    Version.V4
            );
        } catch (IllegalArgumentException e) {
            // Handle specific base64 decoding errors
            throw new KeyConversionException("Invalid base64 encoding in private key", e);
        } catch (InvalidKeySpecException e) {
            // Handle specific key specification errors
            throw new KeyConversionException("Invalid private key format", e);
        } catch (SecurityException e) {
            // Handle security-related exceptions without exposing details
            log.error("Security error during private key conversion", e);
            throw new KeyConversionException("Security error during key conversion");
        }
    }


    /**
     * Converts a base64 encoded public key string to a {@link PublicKey} object.
     *
     * @param publicKeyString the base64 encoded public key string
     * @return a {@link PublicKey} object
     * @throws KeyConversionException if the public key string is invalid
     */
    public PublicKey convertToPublicKey(String publicKeyString, String correlationId) {
        MDC.put("correlationId", correlationId);

        if (publicKeyString.isBlank()) {
            throw new IllegalArgumentException("Public key string cannot be null or empty");
        }

        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            return new PublicKey(
                    KEY_FACTORY.generatePublic(new X509EncodedKeySpec(publicKeyBytes)),
                    Version.V4
            );
        } catch (IllegalArgumentException e) {
            // Handle Base64 decoding errors
            log.warn("Invalid Base64 encoding in public key");
            throw new KeyConversionException("Invalid public key format: Base64 decoding failed", e);
        } catch (InvalidKeySpecException e) {
            // Handle invalid key specification
            log.warn("Invalid public key specification format. CorrelationId: {}", correlationId);
            throw new KeyConversionException("Invalid public key format: incorrect key specification", e);
        } catch (SecurityException e) {
            // Handle security-related exceptions
            log.error("Security violation during public key conversion");
            throw new KeyConversionException("Security error during key conversion", e);
        } catch (RuntimeException e) {
            // Handle unexpected runtime errors
            log.error("Unexpected error during public key conversion");
            throw new KeyConversionException("Internal error during key conversion", e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Converts a base64 encoded secret key string to a {@link SecretKey} object.
     *
     * @param secretKeyString the base64 encoded secret key string
     * @return a {@link SecretKey} object
     * @throws KeyConversionException if the secret key string is invalid
     */
    public SecretKey convertToSecretKey(String secretKeyString) {
        if (secretKeyString.isBlank()) {
            throw new IllegalArgumentException("Secret key string cannot be null or empty");
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
            return new SecretKey(decodedKey, Version.V4);
        } catch (Exception e) {
            log.error("Failed to convert secret key", e);
            throw new KeyConversionException("Failed to convert secret key", e);
        }
    }
        public  <T> T validateKey(T key, String keyType) {
        return Objects.requireNonNull(key, keyType + " key cannot be null");
    }
}
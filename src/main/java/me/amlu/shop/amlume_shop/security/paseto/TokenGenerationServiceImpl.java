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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
// Removed: import me.amlu.shop.amlume_shop.user_management.User;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.commons.PrivateKey;
// Removed: import org.paseto4j.commons.PublicKey; // Not directly used here, only via KeyManagementService
import org.paseto4j.commons.SecretKey;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Service responsible for generating PASETO tokens (v4.public and v4.local).
 * It uses the KeyManagementService to retrieve keys and ObjectMapper to serialize claims.
 * This service expects pre-constructed PasetoClaims objects containing the payload and footer.
 * Payload size validation is performed before signing or encrypting.
 */
@Service
public class TokenGenerationServiceImpl implements TokenGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TokenGenerationServiceImpl.class);

    private final KeyManagementService keyManagementService;
    private final ObjectMapper objectMapper;
    private final TokenValidationService tokenValidationService; // For payload size validation

    // Removed: TokenClaimsService dependency as claim creation is handled upstream.

    public TokenGenerationServiceImpl(KeyManagementService keyManagementService,
                                      ObjectMapper objectMapper,
                                      TokenValidationService tokenValidationService) {
        this.keyManagementService = Objects.requireNonNull(keyManagementService, "KeyManagementService cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.tokenValidationService = Objects.requireNonNull(tokenValidationService, "TokenValidationService cannot be null");
    }

    /**
     * Generates a v4.public PASETO access token by signing the payload.
     *
     * @param claims The PasetoClaims object containing payload and footer claims. Must not be null.
     * @return The generated PASETO token string (v4.public...).
     * @throws TokenGenerationFailureException if token generation fails due to serialization, signing, validation, or other errors.
     * @throws NullPointerException if claims is null.
     */
    @Override
    public String generatePublicAccessToken(PasetoClaims claims) throws TokenGenerationFailureException {
        Objects.requireNonNull(claims, "PasetoClaims cannot be null for public access token generation");
        log.debug("Generating v4.public access token...");

        try {
            PrivateKey privateKey = keyManagementService.getAccessPrivateKey();
            Map<String, Object> payloadMap = claims.getClaims();
            Map<String, Object> footerMap = claims.getFooter(); // Footer is mandatory for v4.public

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String footerJson = objectMapper.writeValueAsString(footerMap);

            // Validate payload size before signing
            tokenValidationService.validatePayloadSize(payloadJson);

            String token = Paseto.sign(privateKey, payloadJson, footerJson);
            log.info("v4.public access token generated successfully.");
            return token;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize claims/footer to JSON for public access token", e);
            throw new TokenGenerationFailureException("Error serializing token data for public access token", e);
        } catch (PasetoException e) {
            log.error("PASETO signing failed for public access token", e);
            throw new TokenGenerationFailureException("PASETO signing operation failed for public access token", e);
        } catch (TokenGenerationFailureException e) { // Catch validation exception specifically
            log.error("Payload size validation failed for public access token", e);
            throw e; // Re-throw validation exception
        } catch (Exception e) {
            log.error("Unexpected error generating public access token", e);
            throw new TokenGenerationFailureException("Unexpected error during public access token generation", e);
        }
    }

    /**
     * Generates a v4.local PASETO access token by encrypting the payload.
     *
     * @param claims The PasetoClaims object containing payload and footer claims. Must not be null.
     * @return The generated PASETO token string (v4.local...).
     * @throws TokenGenerationFailureException if token generation fails due to serialization, encryption, validation, or other errors.
     * @throws NullPointerException if claims is null.
     */
    @Override
    public String generateLocalAccessToken(PasetoClaims claims) throws TokenGenerationFailureException {
        Objects.requireNonNull(claims, "PasetoClaims cannot be null for local access token generation");
        log.debug("Generating v4.local access token...");

        try {
            SecretKey secretKey = keyManagementService.getAccessSecretKey();
            Map<String, Object> payloadMap = claims.getClaims();
            Map<String, Object> footerMap = claims.getFooter(); // Footer is optional for v4.local but recommended

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String footerJson = objectMapper.writeValueAsString(footerMap);

            // Validate payload size before encrypting
            tokenValidationService.validatePayloadSize(payloadJson);

            String token = Paseto.encrypt(secretKey, payloadJson, footerJson);
            log.info("v4.local access token generated successfully.");
            return token;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize claims/footer to JSON for local access token", e);
            throw new TokenGenerationFailureException("Error serializing token data for local access token", e);
        } catch (PasetoException e) {
            log.error("PASETO encryption failed for local access token", e);
            throw new TokenGenerationFailureException("PASETO encryption operation failed for local access token", e);
        } catch (TokenGenerationFailureException e) { // Catch validation exception specifically
            log.error("Payload size validation failed for local access token", e);
            throw e; // Re-throw validation exception
        } catch (Exception e) {
            log.error("Unexpected error generating local access token", e);
            throw new TokenGenerationFailureException("Unexpected error during local access token generation", e);
        }
    }

    /**
     * Generates a v4.local PASETO refresh token by encrypting the payload.
     *
     * @param claims The PasetoClaims object containing payload and footer claims for the refresh token. Must not be null.
     * @return The generated PASETO token string (v4.local...).
     * @throws TokenGenerationFailureException if token generation fails due to serialization, encryption, validation, or other errors.
     * @throws NullPointerException if claims is null.
     */
    @Override
    public String generateLocalRefreshToken(PasetoClaims claims) throws TokenGenerationFailureException {
        Objects.requireNonNull(claims, "PasetoClaims cannot be null for local refresh token generation");
        log.debug("Generating v4.local refresh token...");

        try {
            SecretKey secretKey = keyManagementService.getRefreshSecretKey();
            Map<String, Object> payloadMap = claims.getClaims();
            Map<String, Object> footerMap = claims.getFooter(); // Footer is optional for v4.local but recommended

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            String footerJson = objectMapper.writeValueAsString(footerMap);

            // Validate payload size before encrypting
            tokenValidationService.validatePayloadSize(payloadJson);

            String token = Paseto.encrypt(secretKey, payloadJson, footerJson);
            log.info("v4.local refresh token generated successfully.");
            return token;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize claims/footer to JSON for local refresh token", e);
            throw new TokenGenerationFailureException("Error serializing token data for local refresh token", e);
        } catch (PasetoException e) {
            log.error("PASETO encryption failed for local refresh token", e);
            throw new TokenGenerationFailureException("PASETO encryption operation failed for local refresh token", e);
        } catch (TokenGenerationFailureException e) { // Catch validation exception specifically
            log.error("Payload size validation failed for local refresh token", e);
            throw e; // Re-throw validation exception
        } catch (Exception e) {
            log.error("Unexpected error generating local refresh token", e);
            throw new TokenGenerationFailureException("Unexpected error during local refresh token generation", e);
        }
    }

    // Removed generateLocalRefreshToken(User user) method.
    // Claim creation is handled upstream by TokenClaimsService or the caller (PasetoTokenServiceImpl).
    // This service now focuses solely on the PASETO generation step using provided claims.
}
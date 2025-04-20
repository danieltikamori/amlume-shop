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
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.repositories.RefreshTokenRepository;
import me.amlu.shop.amlume_shop.security.service.AuthenticationInterface;
import me.amlu.shop.amlume_shop.security.service.util.BLAKE3;
import me.amlu.shop.amlume_shop.user_management.User;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static me.amlu.shop.amlume_shop.exceptions.ErrorMessages.FAILED_TO_SERIALIZE_CLAIMS;

/**
 * Service for token generation
 * <p>
 * Generates access and refresh tokens
 * <p>
 * Uses PASETO for token generation
 */

@Service
public class TokenGenerationServiceImpl implements TokenGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TokenGenerationServiceImpl.class);

    // Use Micrometer MeterRegistry
    private final MeterRegistry meterRegistry;

    private final KeyManagementService keyManagementService;
    private final ObjectMapper objectMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationInterface authenticationInterface;
    private final TokenClaimsService tokenClaimsService;
    private final HttpServletRequest httpServletRequest;

    @Value("${paseto.access.public.kid}")
    private String pasetoAccessPublicKid;

    @Value("${paseto.access.local.kid}")
    private String pasetoAccessLocalKid;

    @Value("${paseto.refresh.local.kid}")
    private String pasetoRefreshLocalKid;

    // Constructor updated to inject MeterRegistry and remove PrometheusMeterRegistry/TokenCacheService
    public TokenGenerationServiceImpl(
            MeterRegistry meterRegistry,
            KeyManagementService keyManagementService,
            ObjectMapper objectMapper,
            RefreshTokenRepository refreshTokenRepository,
            AuthenticationInterface authenticationInterface,
            TokenClaimsService tokenClaimsService,
            HttpServletRequest httpServletRequest) {
        this.meterRegistry = meterRegistry;
        this.keyManagementService = keyManagementService;
        this.objectMapper = objectMapper;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authenticationInterface = authenticationInterface;
        this.tokenClaimsService = tokenClaimsService;
        this.httpServletRequest = httpServletRequest;
    }


    /**
     * Generates a public access token
     *
     * @param claims The claims to be included in the token
     * @return The generated token
     * @throws TokenGenerationFailureException If token generation fails
     */
    @Override
    public String generatePublicAccessToken(PasetoClaims claims) throws TokenGenerationFailureException {
        String generatedToken = null;
        try {
            String payload = objectMapper.writeValueAsString(claims);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoAccessPublicKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            generatedToken = Paseto.sign(keyManagementService.getAccessPrivateKey(), payload, footer);

            // Increment success counter
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "success").increment();
            log.debug("Successfully generated public access token.");
            return generatedToken;

        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "failure", "reason", "serialization").increment();
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to sign PASETO token", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "failure", "reason", "paseto_sign").increment();
            throw new TokenGenerationFailureException("Failed to sign PASETO token", e);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid argument or null pointer during PASETO token generation", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "failure", "reason", "invalid_argument").increment();
            throw new TokenGenerationFailureException("Invalid argument or null pointer during token generation", e);
        } catch (SecurityException e) {
            log.error("Security violation during PASETO token generation", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "failure", "reason", "security").increment();
            throw new TokenGenerationFailureException("Security violation during token generation", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "public_access", "status", "failure", "reason", "unknown").increment();
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
    }

    /**
     * Generates a local access token
     *
     * @param claims The claims to be included in the token
     * @return The generated token
     * @throws TokenGenerationFailureException If token generation fails
     */
    @Override
    public String generateLocalAccessToken(PasetoClaims claims) throws TokenGenerationFailureException {
        String generatedToken = null;
        try {
            String payload = objectMapper.writeValueAsString(claims);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoAccessLocalKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            generatedToken = Paseto.encrypt(keyManagementService.getAccessSecretKey(), payload, footer);

            // Increment success counter
            meterRegistry.counter("paseto_token_generation_total", "type", "local_access", "status", "success").increment();
            log.debug("Successfully generated local access token.");
            return generatedToken;

        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_access", "status", "failure", "reason", "serialization").increment();
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to encrypt PASETO token", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_access", "status", "failure", "reason", "paseto_encrypt").increment();
            throw new TokenGenerationFailureException("Failed to encrypt PASETO token", e);
        } catch (Exception e) { // Catch broader exceptions for local encryption
            log.error("Unexpected error during local PASETO token generation", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_access", "status", "failure", "reason", "unknown").increment();
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
    }

    /**
     * Generates a local refresh token
     * Used in conjunction with the public access token
     *
     * @param user The user associated with the refresh token
     * @return The generated token
     * @throws TokenGenerationFailureException If token generation fails
     */
    @Override
    public String generateLocalRefreshToken(User user) throws TokenGenerationFailureException {
        String refreshToken = null;
        try {
            PasetoClaims claims = tokenClaimsService.createLocalRefreshPasetoClaims(String.valueOf(user.getUserId()), authenticationInterface.getRefreshTokenDuration());
            String payload = objectMapper.writeValueAsString(claims);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoRefreshLocalKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            refreshToken = Paseto.encrypt(keyManagementService.getRefreshSecretKey(), payload, footer);

            // Hash and store the refresh token in the database
            String hashedRefreshToken = BLAKE3.hash(refreshToken.getBytes());
            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setToken(hashedRefreshToken);
            refreshTokenEntity.setUser(user);
            refreshTokenEntity.setExpiryDate(Instant.now().plus(authenticationInterface.getRefreshTokenDuration()));
            refreshTokenEntity.setDeviceFingerprint(httpServletRequest.getHeader("User-Agent")); // Consider making fingerprinting more robust
//            refreshTokenEntity.setRevoked(false);

            // Save the RefreshToken entity to the database
            refreshTokenRepository.save(refreshTokenEntity);

            // Increment success counter
            meterRegistry.counter("paseto_token_generation_total", "type", "local_refresh", "status", "success").increment();
            log.debug("Successfully generated and stored local refresh token for user ID: {}", user.getUserId());
            return refreshToken; // Return the *unhashed* refresh token to the client

        } catch (JsonProcessingException e) {
            log.error("Failed to generate refresh token due to JSON processing error", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_refresh", "status", "failure", "reason", "serialization").increment();
            throw new TokenGenerationFailureException("Failed to generate refresh token due to JSON processing error", e);
        } catch (PasetoException e) {
            log.error("Failed to generate refresh token due to PASETO encryption error", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_refresh", "status", "failure", "reason", "paseto_encrypt").increment();
            throw new TokenGenerationFailureException("Failed to generate refresh token due to PASETO encryption error", e);
        } catch (IllegalArgumentException e) { // Catch specific DB/hashing related errors if possible
            log.error("Failed to generate refresh token due to invalid argument (potentially during hashing/saving)", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_refresh", "status", "failure", "reason", "invalid_argument").increment();
            throw new TokenGenerationFailureException("Failed to generate refresh token due to invalid argument", e);
        } catch (Exception e) { // Catch broader exceptions
            log.error("Unexpected error occurred while generating refresh token", e);
            meterRegistry.counter("paseto_token_generation_total", "type", "local_refresh", "status", "failure", "reason", "unknown").increment();
            throw new TokenGenerationFailureException("Unexpected error occurred while generating refresh token", e);
        }
    }
}

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
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.repositories.RefreshTokenRepository;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.security.service.util.BLAKE3;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.version4.Paseto;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class TokenGenerationServiceImpl implements TokenGenerationService {

    // Metrics
    private final PrometheusMeterRegistry meterRegistry;

    private final Counter tokenGenerationCounter = Counter.build()
            .name("paseto_token_generation_total")
            .help("Total number of PASETO tokens generated")
            .labelNames("status") // Add labels for better metrics granularity
            .register();

    private final KeyManagementService keyManagementService;
    private final ObjectMapper objectMapper;
    private final TokenCacheService tokenCacheService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final TokenClaimsService tokenClaimsService;
    private final HttpServletRequest httpServletRequest;

    @Value("${paseto.access.public.kid}")
    private String pasetoAccessPublicKid;

    @Value("${paseto.access.local.kid}")
    private String pasetoAccessLocalKid;

    @Value("${paseto.refresh.local.kid}")
    private String pasetoRefreshLocalKid;

    /**
     * Generates a public access token
     *
     * @param claims The claims to be included in the token
     * @return The generated token
     * @throws TokenGenerationFailureException If token generation fails
     */
    @Override
    public String generatePublicAccessToken(PasetoClaims claims) throws TokenGenerationFailureException {
        try {
            String payload = objectMapper.writeValueAsString(claims);

            tokenCacheService.validatePayload(payload);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoAccessPublicKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            return Paseto.sign(keyManagementService.getAccessPrivateKey(), payload, footer);
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to sign PASETO token", e);
            throw new TokenGenerationFailureException("Failed to sign PASETO token", e);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Invalid argument or null pointer during PASETO token generation", e);
            throw new TokenGenerationFailureException("Invalid argument or null pointer during token generation", e);
        } catch (SecurityException e) {
            log.error("Security violation during PASETO token generation", e);
            throw new TokenGenerationFailureException("Security violation during token generation", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
//        finally {
//            log.info("Token generation completed successfully");
//        }
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
        try {
            String payload = objectMapper.writeValueAsString(claims);

            tokenCacheService.validatePayload(payload);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoAccessLocalKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            return Paseto.encrypt(keyManagementService.getAccessSecretKey(), payload, footer);
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to encrypt PASETO token", e);
            throw new TokenGenerationFailureException("Failed to encrypt PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
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
        try {
            PasetoClaims claims = tokenClaimsService.createLocalRefreshPasetoClaims(String.valueOf(user.getUserId()), enhancedAuthenticationService.getRefreshTokenDuration());
            String payload = objectMapper.writeValueAsString(claims);

            tokenCacheService.validatePayload(payload);

            PasetoClaims footerClaims = tokenClaimsService.createPasetoFooterClaims(pasetoRefreshLocalKid);
            String footer = objectMapper.writeValueAsString(footerClaims);

            String refreshToken = Paseto.encrypt(keyManagementService.getRefreshSecretKey(), payload, footer);

            // Hash and store the refresh token in the database
            String hashedRefreshToken = BLAKE3.hash(refreshToken.getBytes()); // Hash the refresh token using BLAKE3 // import org.bouncycastle.crypto.digests.BLAKE3Digest;
            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setToken(hashedRefreshToken);
            refreshTokenEntity.setUser(user);
            refreshTokenEntity.setExpiryDate(Instant.now().plus(enhancedAuthenticationService.getRefreshTokenDuration())); // Set expiry date
            refreshTokenEntity.setDeviceFingerprint(httpServletRequest.getHeader("User-Agent"));
            refreshTokenEntity.setRevoked(false);

            // Save the RefreshToken entity to the database
            refreshTokenRepository.save(refreshTokenEntity);

            return refreshToken; // Return the *unhashed* refresh token to the client

        } catch (JsonProcessingException e) {
            log.error("Failed to generate refresh token due to JSON processing error", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token due to JSON processing error", e);
        } catch (PasetoException e) {
            log.error("Failed to generate refresh token due to PASETO encryption error", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token due to PASETO encryption error", e);
        } catch (IllegalArgumentException e) {
            log.error("Failed to generate refresh token due to invalid argument", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token due to invalid argument", e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while generating refresh token", e);
            throw new TokenGenerationFailureException("Unexpected error occurred while generating refresh token", e);
        }
//        finally {
//            log.info("Token generation completed successfully");
//        }
    }
}

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

import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants; // Import TokenConstants
import me.amlu.shop.amlume_shop.user_management.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SignatureException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects; // Import Objects for null checks


/**
 * Orchestration Service for generating and validating tokens
 * <p>
 * Uses Paseto for token generation and validation
 */

@Service
public class PasetoTokenServiceImpl implements PasetoTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasetoTokenServiceImpl.class);

    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;
    private final TokenClaimsService tokenClaimsService;

    public PasetoTokenServiceImpl(TokenGenerationService tokenGenerationService, TokenValidationService tokenValidationService, TokenClaimsService tokenClaimsService) {
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidationService = tokenValidationService;
        this.tokenClaimsService = tokenClaimsService;
    }

    /**
     * Generates a public access token.
     * Uses asymmetric signing (v4.public).
     *
     * @param userId User ID for the token's subject claim.
     * @param accessTokenDuration Duration for which the token should be valid.
     * @return The generated PASETO public access token string.
     * @throws TokenGenerationFailureException if token generation fails.
     */
    @Override
    public String generatePublicAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        log.debug("Generating public access token for user ID: {}", userId);
        // 1. Create claims (including footer)
        PasetoClaims claims = tokenClaimsService.createPublicAccessPasetoClaims(userId, accessTokenDuration);
        PasetoClaims footer = tokenClaimsService.createPasetoFooterClaims("public_access"); // Create footer
        claims.getFooter().putAll(footer.getFooter()); // Merge footer into claims object

        // 2. Generate token
        return tokenGenerationService.generatePublicAccessToken(claims);
    }

    /**
     * Generates a local access token.
     * For internal use (between microservices). Uses symmetric encryption (v4.local).
     *
     * @param userId User ID for the token's subject claim.
     * @param accessTokenDuration Duration for which the token should be valid.
     * @return The generated PASETO local access token string.
     * @throws TokenGenerationFailureException if token generation fails.
     */
    @Override
    public String generateLocalAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        log.debug("Generating local access token for user ID: {}", userId);
        // 1. Create claims (including footer)
        // NOTE: Using createPublicAccessPasetoClaims here might be incorrect if local tokens
        // need different claims (e.g., different audience, no session ID).
        // Assuming for now it's okay, but consider creating createLocalAccessPasetoClaims if needed.
        PasetoClaims claims = tokenClaimsService.createPublicAccessPasetoClaims(userId, accessTokenDuration);
        PasetoClaims footer = tokenClaimsService.createPasetoFooterClaims("local_access"); // Create footer
        claims.getFooter().putAll(footer.getFooter()); // Merge footer into claims object

        // 2. Generate token
        return tokenGenerationService.generateLocalAccessToken(claims);
    }

    /**
     * Generates a local refresh token.
     * Used in conjunction with the access token. Uses symmetric encryption (v4.local).
     *
     * @param user The User object for whom to generate the refresh token. Must not be null and must have an ID.
     * @return The generated PASETO local refresh token string.
     * @throws TokenGenerationFailureException if token generation fails.
     * @throws NullPointerException if user or user.getId() is null.
     */
    @Override
    public String generateRefreshToken(User user) throws TokenGenerationFailureException {
        Objects.requireNonNull(user, "User cannot be null for refresh token generation");
        Objects.requireNonNull(user.getId(), "User ID cannot be null for refresh token generation");
        log.debug("Generating refresh token for user ID: {}", user.getId());

        // 1. Create claims (including footer) using TokenClaimsService
        PasetoClaims claims = tokenClaimsService.createLocalRefreshPasetoClaims(
                user.getId().toString(),
                TokenConstants.REFRESH_TOKEN_DURATION // Use constant for duration
        );
        PasetoClaims footer = tokenClaimsService.createPasetoFooterClaims("local_refresh"); // Create footer
        claims.getFooter().putAll(footer.getFooter()); // Merge footer into claims object


        // 2. Generate token using the claims by calling the correct method signature
        return tokenGenerationService.generateLocalRefreshToken(claims); // Pass PasetoClaims
    }

    /**
     * Validates a public access token (v4.public).
     * Verifies signature and claims.
     *
     * @param token the public access token string to be validated.
     * @return a map of claims extracted from the token if validation is successful.
     * @throws TokenValidationFailureException if the token validation fails (e.g., expired, revoked, invalid claims).
     * @throws SignatureException              if the token's signature is invalid.
     */
    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException, SignatureException {
        log.trace("Validating public access token...");
        return tokenValidationService.validatePublicAccessToken(token);
    }

    /**
     * Validates a local access token (v4.local).
     * Decrypts and verifies claims.
     *
     * @param token the local access token string to be validated.
     * @return a map of claims extracted from the token if validation is successful.
     * @throws TokenValidationFailureException if the token validation fails (e.g., decryption error, expired, revoked, invalid claims).
     */
    @Override
    public Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException {
        log.trace("Validating local access token...");
        return tokenValidationService.validateLocalAccessToken(token);
    }

    /**
     * Validates a local refresh token (v4.local).
     * Decrypts and verifies claims.
     *
     * @param token the local refresh token string to be validated.
     * @return a map of claims extracted from the token if validation is successful.
     * @throws TokenValidationFailureException if the token validation fails (e.g., decryption error, expired, revoked, invalid claims).
     */
    @Override
    public Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException {
        log.trace("Validating local refresh token...");
        return tokenValidationService.validateLocalRefreshToken(token);
    }
}

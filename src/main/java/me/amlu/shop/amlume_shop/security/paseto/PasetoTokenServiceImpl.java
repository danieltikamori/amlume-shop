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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.user_management.User;
import org.springframework.stereotype.Service;

import java.security.SignatureException;
import java.time.Duration;
import java.util.Map;


/**
 * Orchestration Service for generating and validating tokens
 * <p>
 * Uses Paseto for token generation and validation
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class PasetoTokenServiceImpl implements PasetoTokenService {

    private final TokenGenerationService tokenGenerationService;
    private final TokenValidationService tokenValidationService;
    private final TokenClaimsService tokenClaimsService;


    /**
     * Generates a public access token
     * Uses asymmetric encryption
     *
     * @param userId
     * @param accessTokenDuration
     * @return
     * @throws TokenGenerationFailureException
     */
    @Override
    public String generatePublicAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        return tokenGenerationService.generatePublicAccessToken(tokenClaimsService.createPublicAccessPasetoClaims(userId, accessTokenDuration));
    }

    /**
     * Generates a local access token
     * For internal use (between microservices, e.g. between gateway and auth service)
     * Uses symmetric encryption
     *
     * @param userId
     * @param accessTokenDuration
     * @return
     * @throws TokenGenerationFailureException
     */
    @Override
    public String generateLocalAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        return tokenGenerationService.generateLocalAccessToken(tokenClaimsService.createPublicAccessPasetoClaims(userId, accessTokenDuration));
    }

    /**
     * Generates a local refresh token
     * Used in conjunction with the public access token
     * Uses symmetric encryption
     *
     * @param user
     * @return
     * @throws TokenGenerationFailureException
     */
    @Override
    public String generateRefreshToken(User user) throws TokenGenerationFailureException {
        return tokenGenerationService.generateLocalRefreshToken(user);
    }

    /**
     * Validates a public access token.
     * <p>
     * This method verifies the authenticity and integrity of a public access token
     * using asymmetric cryptography. It checks the token's signature and validates
     * its claims to ensure it is not expired, tampered with, or invalid.
     *
     * @param token the public access token to be validated
     * @return a map of claims extracted from the token if validation is successful
     * @throws TokenValidationFailureException if the token validation fails
     * @throws SignatureException              if the token's signature is invalid
     */
    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException, SignatureException {
        return tokenValidationService.validatePublicAccessToken(token);
    }

    /**
     * Validates a local access token.
     * <p>
     * This method verifies the authenticity and integrity of a local access token
     * using symmetric encryption. It checks the token's signature and validates
     * its claims to ensure it is not expired, tampered with, or invalid.
     *
     * @param token the local access token to be validated
     * @return a map of claims extracted from the token if validation is successful
     * @throws TokenValidationFailureException if the token validation fails
     */
    @Override
    public Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException {
        return tokenValidationService.validateLocalAccessToken(token);
    }

    /**
     * Validates a local refresh token.
     * <p>
     * This method verifies the authenticity and integrity of a local refresh token
     * using symmetric encryption. It checks the token's signature and validates
     * its claims to ensure it is not expired, tampered with, or invalid.
     *
     * @param token the local refresh token to be validated
     * @return a map of claims extracted from the token if validation is successful
     * @throws TokenValidationFailureException if the token validation fails
     */
    @Override
    public Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException {
        return tokenValidationService.validateLocalRefreshToken(token);
    }
}
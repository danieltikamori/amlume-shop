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

import me.amlu.shop.amlume_shop.exceptions.*; // Import necessary exceptions

import java.security.SignatureException;
import java.util.Map;

/**
 * Interface defining the contract for validating different types of PASETO tokens
 * and their claims.
 */
public interface TokenValidationService {

    /**
     * Checks if a public access token is valid (signature, expiry, claims).
     * Convenience method that wraps validatePublicAccessToken.
     *
     * @param token The public access token string.
     * @return true if the token is valid, false otherwise.
     */
    boolean isAccessTokenValid(String token);

    /**
     * Validates a v4.public PASETO access token.
     * Verifies signature, expiry, and standard/custom claims.
     *
     * @param token The token string.
     * @return A map containing the validated claims.
     * @throws TokenValidationFailureException If validation fails (e.g., expired, invalid issuer, revoked).
     * @throws SignatureException              If the token signature is invalid.
     * @throws ClaimsParsingException          If claims cannot be parsed.
     * @throws TooManyRequestsException        If rate limiting is exceeded.
     */
    Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException, SignatureException;

    /**
     * Validates a v4.local PASETO access token.
     * Decrypts the token and verifies expiry and standard/custom claims.
     *
     * @param token The token string.
     * @return A map containing the validated claims.
     * @throws TokenValidationFailureException If validation fails (e.g., decryption error, expired, invalid issuer, revoked).
     * @throws TooManyRequestsException        If rate limiting is exceeded.
     */
    Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException;

    /**
     * Validates a v4.local PASETO refresh token.
     * Decrypts the token and verifies expiry and standard/custom claims specific to refresh tokens.
     *
     * @param token The token string.
     * @return A map containing the validated claims.
     * @throws TokenValidationFailureException If validation fails (e.g., decryption error, expired, invalid issuer, revoked).
     * @throws TooManyRequestsException        If rate limiting is exceeded.
     */
    Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException;

    /**
     * Validates the size of a serialized token payload string.
     *
     * @param payload The serialized payload JSON string.
     * @throws TokenGenerationFailureException if the payload is null, empty, or exceeds the maximum allowed size.
     */
    void validatePayloadSize(String payload) throws TokenGenerationFailureException; // <-- ADD THIS METHOD

    /**
     * Validates the size of the claims map after deserialization.
     *
     * @param claims The map of claims.
     * @throws ClaimsSizeException If the serialized size of the claims map exceeds the maximum limit or if serialization fails.
     */
    void validateClaimsSize(Map<String, Object> claims) throws ClaimsSizeException;

    /**
     * Validates the claims specific to a public access token.
     *
     * @param claims       The token claims map.
     * @param expectedType The expected token type string (e.g., "ACCESS_TOKEN").
     * @throws TokenValidationFailureException If any claim validation fails.
     */
    void validatePublicAccessTokenClaims(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException;

    /**
     * Validates the claims specific to a local access token.
     *
     * @param claims       The token claims map.
     * @param expectedType The expected token type string (e.g., "ACCESS_TOKEN").
     * @throws TokenValidationFailureException If any claim validation fails.
     */
    void validateLocalAccessTokenClaims(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException;

    /**
     * Validates the claims specific to a local refresh token.
     *
     * @param claims       The token claims map.
     * @param expectedType The expected token type string (e.g., "REFRESH_TOKEN").
     * @throws TokenValidationFailureException If any claim validation fails.
     */
    void validateLocalRefreshTokenClaims(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException;

    /**
     * Extracts claims from a public access token without performing full validation (useful for inspection).
     * Parses the token and deserializes the payload.
     *
     * @param token The public access token string.
     * @return A map containing the claims.
     * @throws ClaimsExtractionFailureException If parsing or claims extraction fails.
     * @throws TokenValidationFailureException  If the token format is invalid.
     * @throws NullPointerException             if token is null.
     * @throws IllegalArgumentException         if token is empty or has invalid parts.
     */
    Map<String, Object> extractClaimsFromPublicAccessToken(String token) throws ClaimsExtractionFailureException, TokenValidationFailureException;

}

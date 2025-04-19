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

//import static me.amlu.shop.amlume_shop.exceptions.ErrorMessages.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.paseto.util.PasetoPropertyResolver;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.version4.Paseto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.*;


@Slf4j
@Service
public class TokenValidationServiceImpl implements TokenValidationService {

    private final ObjectMapper objectMapper;
    private final KeyManagementService keyManagementService;
    private final TokenUtilService tokenUtilService;
    private final TokenRevocationService tokenRevocationService;
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final PasetoPropertyResolver pasetoPropertyResolver;

    @Qualifier("redisSlidingWindowRateLimiter")
    private final RateLimiter rateLimiter;

    // Metrics
    private final MeterRegistry meterRegistry;

//    private final PrometheusMeterRegistry meterRegistry;

    private final Counter tokenValidationCounter;
    private final Timer tokenValidationLatency;
    private final Timer tokenClaimsValidationLatency;

    public TokenValidationServiceImpl(ObjectMapper objectMapper, KeyManagementService keyManagementService, TokenUtilService tokenUtilService, TokenRevocationService tokenRevocationService, HttpServletRequest httpServletRequest, UserRepository userRepository, MeterRegistry meterRegistry, Counter tokenValidationCounter, PasetoPropertyResolver pasetoPropertyResolver, @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter, Timer tokenValidationLatency, Timer tokenClaimsValidationLatency) {
        this.rateLimiter = rateLimiter;
        this.tokenClaimsValidationLatency = tokenClaimsValidationLatency;
        this.objectMapper = objectMapper;
        this.keyManagementService = keyManagementService;
        this.tokenUtilService = tokenUtilService;
        this.tokenRevocationService = tokenRevocationService;
        this.httpServletRequest = httpServletRequest;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
        this.pasetoPropertyResolver = pasetoPropertyResolver;
        this.tokenValidationCounter = tokenValidationCounter;
        this.tokenValidationLatency = tokenValidationLatency;
    }

    private Timer createTokenValidationTimer(MeterRegistry registry) {
        return Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);
    }

    private void logTokenValidationAttempt(String token, boolean success, String errorMessage) {
        if (success) {
            log.info("Token validation successful for token: {}", maskToken(token));
        } else {
            log.error("Token validation failed for token: {}. Error: {}",
                    maskToken(token), errorMessage);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        try {
            validatePublicAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;

        }
    }

    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException, SignatureException {
        // Apply rate limiting
        String rateLimitKey = TokenConstants.TOKEN_VALIDATION_RATELIMIT_KEY + token; // e.g., userId or token JTI
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            throw new TooManyRequestsException("Token validation rate limit exceeded");
        }

        tokenValidationCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.debug("Validating public access token");
            validateTokenStringLength(token);

            String[] parts = splitToken(token);
            String signedMessage = createSignedMessage(parts);
            String footer = extractFooter(parts);
            String payload = parseAndVerifyToken(signedMessage, footer);
            Map<String, Object> claims = parseClaims(payload);
            validatePublicAccessKid(claims);
            validatePublicAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));
            log.debug("Public access token validation successful");
            return claims;
        } catch (TokenValidationFailureException e) {
            handlePublicAccessTokenValidationException(e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED, e); // Use the constant
        } catch (JsonProcessingException e) {
            throw new ClaimsParsingException(ERROR_PARSING_CLAIMS, e); // Use the constant
        } catch (SignatureException e) {
            throw new SignatureException(INVALID_PASETO_SIGNATURE, e); // Use the constant
        } finally {
            sample.stop(tokenValidationLatency);


        }
    }

    @Override
    public Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException {
        // Apply rate limiting
        String rateLimitKey = TokenConstants.TOKEN_VALIDATION_RATELIMIT_KEY + token; // e.g., userId or token JTI
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            throw new TooManyRequestsException("Token validation rate limit exceeded");
        }

        tokenValidationCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(keyManagementService.getAccessSecretKey(), parts[PASETO_TOKEN_PAYLOAD_PLACE], parts[PASETO_TOKEN_FOOTER_PLACE]);

            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // Validate kid from footer claims
            validateLocalAccessKid(claims);

            validateLocalAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));

            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(ERROR_PARSING_CLAIMS, e);
            throw new TokenValidationFailureException(ERROR_PARSING_CLAIMS, e);

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(INVALID_PASETO_TOKEN, e);
            throw new TokenValidationFailureException(INVALID_PASETO_TOKEN, e);

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate local access PASETO token", e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED, e);
        } finally {
            sample.stop(tokenValidationLatency);
        }
    }


    @Override
    public Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException { // Separate refresh token validation
        // Apply rate limiting
        String rateLimitKey = TokenConstants.TOKEN_VALIDATION_RATELIMIT_KEY + token; // e.g., userId or token JTI
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            throw new TooManyRequestsException("Token validation rate limit exceeded");
        }

        tokenValidationCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(keyManagementService.getRefreshSecretKey(), parts[PASETO_TOKEN_PAYLOAD_PLACE], parts[PASETO_TOKEN_FOOTER_PLACE]);

            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // Validate kid from footer claims
            validateLocalRefreshKid(claims);

            validateLocalRefreshTokenClaims(claims, String.valueOf(TokenType.REFRESH_TOKEN)); // Specific validation rules for refresh token claims
            return claims;

        } catch (JsonProcessingException e) {
            log.error("Error parsing claims during refresh token validation: {}", e.getMessage(), e);
            throw new TokenValidationFailureException("Error parsing token claims", e);
        } catch (IllegalArgumentException | PasetoException e) {
            log.error("Invalid Refresh PASETO token or PASETO error: {}", e.getMessage(), e);
            throw new TokenValidationFailureException("Invalid refresh token format", e);
        } catch (InvalidTokenLengthException e) {
            log.error("Invalid refresh token length", e);
            throw new InvalidTokenLengthException("Invalid refresh token length", e);
        } catch (TokenValidationFailureException e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED, e);
        } finally {
            sample.stop(tokenValidationLatency);
        }
    }

    private void validateTokenStringLength(String token) throws InvalidTokenLengthException {
        Objects.requireNonNull(token, "Token cannot be null");

        if (token.length() < TokenConstants.MIN_TOKEN_LENGTH || token.length() > TokenConstants.MAX_TOKEN_LENGTH) {
            throw new InvalidTokenLengthException(INVALID_TOKEN_LENGTH); // Use the constant
        }
    }


    private String[] splitToken(String token) throws InvalidTokenFormatException {
        String[] parts = token.split("\\.");
        if (parts.length > PASETO_TOKEN_PARTS_LENGTH || parts.length < PASETO_TOKEN_PARTS_LENGTH) {
            throw new InvalidTokenFormatException(INVALID_TOKEN_FORMAT);
        }
        return parts;
    }

    private String createSignedMessage(String[] parts) throws InvalidTokenFormatException {
        if (parts[2].isBlank()) {
            throw new InvalidTokenFormatException(INVALID_TOKEN_FORMAT);
        }
        return parts[PASETO_TOKEN_PAYLOAD_PLACE]; // Payload part of the token
    }

    private String extractFooter(String[] parts) {
        if (parts[3].isBlank()) {
            throw new InvalidTokenFormatException(INVALID_TOKEN_FORMAT);
        }
        return parts[PASETO_TOKEN_FOOTER_PLACE]; // Footer part of the token
    }

    private String parseAndVerifyToken(String signedMessage, String footer) throws SignatureException {
        try {
            return Paseto.parse(keyManagementService.getAccessPublicKey(), signedMessage, footer);
        } catch (SignatureException e) {
            log.error(INVALID_PASETO_SIGNATURE, e);
            throw new SignatureException(INVALID_PASETO_SIGNATURE, e);
        }
    }

    private Map<String, Object> parseClaims(String payload) throws JsonProcessingException {
        try {
            if (payload.isBlank()) {
                throw new IllegalArgumentException("Payload is null or empty");
            }
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error(ERROR_PARSING_CLAIMS, e);
            throw new TokenValidationFailureException("Error parsing claims", e);
        }
    }

    private void validatePublicAccessKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER); // Use the constant
        }
        if (!pasetoPropertyResolver.resolvePublicAccessKid().equals(kid)) {
            throw new TokenValidationFailureException(INVALID_KEY_ID); // Use the constant
        }
    }

    private void validateLocalAccessKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER);
        }
        if (!pasetoPropertyResolver.resolveLocalAccessKid().equals(kid)) {
            throw new TokenValidationFailureException(INVALID_KEY_ID);
        }
    }

    private void validateLocalRefreshKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER);
        }
        if (!pasetoPropertyResolver.resolveLocalRefreshKid().equals(kid)) {
            throw new TokenValidationFailureException(INVALID_KEY_ID);
        }
    }


    private void handlePublicAccessTokenValidationException(Exception e) throws TokenValidationFailureException {
        switch (e) {
            case SignatureException signatureException -> {
                log.error(INVALID_PASETO_SIGNATURE, e); // Use the constant

                throw new InvalidTokenSignatureException(INVALID_PASETO_SIGNATURE, e); // Use the constant // Use the constant
            }
            case JsonProcessingException jsonProcessingException -> {
                log.error(ERROR_PARSING_CLAIMS, e); // Use the constant

                throw new TokenValidationFailureException(ERROR_PARSING_CLAIMS, e); // Use the constant // Use the constant
            }
            case IllegalArgumentException illegalArgumentException -> {
                log.error(INVALID_PASETO_TOKEN, e);
                throw new TokenValidationFailureException(INVALID_PASETO_TOKEN, e);
            }
            case TokenValidationFailureException tokenValidationFailureException ->
                    throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED, e); // TOCHECK
            case PasetoException pasetoException -> {
                log.error("PASETO-specific error occurred", e);
                throw new TokenValidationFailureException("PASETO-specific error occurred", e);
            }
            case null, default -> {
                log.error("Unexpected error during public access PASETO token validation", e);
                throw new TokenValidationFailureException("Unexpected error during token validation", e);
            }
        }
    }


    /**
     * Validates the payload of a token to ensure it is not too large.
     *
     * @param payload the payload to validate.
     * @throws TokenGenerationFailureException if the payload is too large.
     */
    public void validatePayloadSize(String payload) throws TokenGenerationFailureException {
        if (payload.isBlank()) {
            throw new TokenGenerationFailureException("Payload cannot be null or empty");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > TokenConstants.MAX_PAYLOAD_SIZE) {
            throw new TokenGenerationFailureException("Payload size exceeds maximum allowed size");
        }
    }

    /**
     * Validates all claims present in an access token.
     *
     * @param claims       The token claims to validate
     * @param expectedType The expected token type
     * @throws TokenValidationFailureException if any validation fails
     */
    public void validatePublicAccessTokenClaims(Map<String, Object> claims, String expectedType)
            throws TokenValidationFailureException {
        // Apply rate limiting if required
//        String jti = (String) claims.get("jti");
//        String rateLimitKey = TokenConstants.CLAIMS_VALIDATION_RATELIMIT_KEY + jti; // e.g., userId or token JTI
//        if (!rateLimiter.tryAcquire(rateLimitKey)) {
//            throw new TooManyRequestsException("Claims validation rate limit exceeded");
//        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            validateUserStatus(claims);
            validateTokenSessionId(claims);
            coreValidateTokenClaims(claims, expectedType);
        } finally {
            sample.stop(tokenClaimsValidationLatency);
        }
    }

    /**
     * Validates all claims present in an access token.
     *
     * @param claims       The token claims to validate
     * @param expectedType The expected token type
     * @throws TokenValidationFailureException if any validation fails
     */
    public void validateLocalAccessTokenClaims(Map<String, Object> claims, String expectedType)
            throws TokenValidationFailureException {
//        // Apply rate limiting if required
//        String jti = (String) claims.get("jti");
//        String rateLimitKey = TokenConstants.CLAIMS_VALIDATION_RATELIMIT_KEY + jti; // e.g., userId or token JTI
//        if (!rateLimiter.tryAcquire(rateLimitKey)) {
//            throw new TooManyRequestsException("Claims validation rate limit exceeded");
//        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            coreValidateTokenClaims(claims, expectedType);
            validateTokenSessionId(claims);
//            validateServiceStatus(claims);
        } finally {
            sample.stop(tokenClaimsValidationLatency);
        }
    }

    /**
     * Validates all claims present in a refresh token.
     *
     * @param claims       The token claims to validate
     * @param expectedType The expected token type
     * @throws TokenValidationFailureException if any validation fails
     */
    public void validateLocalRefreshTokenClaims(Map<String, Object> claims, String expectedType)
            throws TokenValidationFailureException {
        // Apply rate limiting if required
//        String jti = (String) claims.get("jti");
//        String rateLimitKey = TokenConstants.CLAIMS_VALIDATION_RATELIMIT_KEY + jti; // e.g., userId or token JTI
//        if (!rateLimiter.tryAcquire(rateLimitKey)) {
//            throw new TooManyRequestsException("Claims validation rate limit exceeded");
//        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            validateUserStatus(claims);
            coreValidateTokenClaims(claims, expectedType);
        } finally {
            sample.stop(tokenClaimsValidationLatency);
        }
    }

    /**
     * Core Token claims validator for access and refresh tokens. Common for all validators.
     *
     * @param claims       The token claims to validate
     * @param expectedType The expected token type
     */
    private void coreValidateTokenClaims(Map<String, Object> claims, String expectedType) {
        validateClaimsSize(claims);
        validateTokenMetadata(claims, expectedType);
        validateTokenTiming(claims);
        validateTokenIdentity(claims);
    }

    @Override
    public void validateClaimsSize(Map<String, Object> claims) throws ClaimsSizeException {
        try {
            String claimsJson = objectMapper.writeValueAsString(claims);
            if (claimsJson.length() > TokenConstants.MAX_CLAIMS_SIZE) {
                throw new ClaimsSizeException(CLAIMS_PAYLOAD_EXCEEDS_MAXIMUM_SIZE); // Use the constant
            }
        } catch (JsonProcessingException e) {
            throw new ClaimsSizeException(ERROR_SERIALIZING_CLAIMS_TO_JSON, e); // Use the constant
        }
    }

    /**
     * Validates the user's status and existence
     */
    private void validateUserStatus(Map<String, Object> claims) throws TokenValidationFailureException {
        log.debug("Validating user status for token");
        try {
            String userId = tokenUtilService.extractUserId(claims);
            User user = tokenUtilService.getUserByUserId(userId);
            validateUserEnabled(user);
            tokenRevocationService.validateNotRevoked(claims);
            validateUserScope(claims);
            log.debug("User status validation successful");
        } catch (TokenValidationFailureException e) {
            log.warn("User status validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates if user account is enabled
     */
    private void validateUserEnabled(User user) throws TokenValidationFailureException {
        if (!user.isEnabled()) {
            throw new TokenValidationFailureException(USER_ACCOUNT_DISABLED);
        }
    }

    private void validateUserScope(Map<String, Object> claims) throws TokenValidationFailureException {
        String userScope = tokenUtilService.extractUserScope(claims);

        Set<UserRole> roles = userRepository.findRolesByUserId(Long.valueOf(tokenUtilService.extractUserId(claims)));

        if (Objects.isNull(roles) || roles.isEmpty()) {
            throw new TokenValidationFailureException(NO_ROLES_FOUND_FOR_USER);
        }
        Set<String> expectedScopes = roles.stream()
                .map(role -> role.getRoleName().name().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> actualScopes = Arrays.stream(userScope.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (!expectedScopes.containsAll(actualScopes)) {
            log.warn("User scope validation failed. Expected: {}, Actual: {}", expectedScopes, actualScopes);
            throw new TokenValidationFailureException("Invalid user scope. Actual: " + actualScopes);
        }
    }

//    /**
//     * Validates service status and existence
//     * Local access token validation
//     * For Kubernetes, this method should be modified to check the Kubernetes API for service status and authorization
//     */
//    private void validateServiceStatus(Map<String, Object> claims) throws TokenValidationFailureException {
//        log.debug("Validating service status for token");
//        try {
//            String serviceId = tokenUtilService.extractServiceId(claims);
//            if (serviceId == null || serviceId.isBlank()) {
//                throw new TokenValidationFailureException("Service ID is missing");
//            }
//            // Implement service status validation logic here
//            // E.g. check if the service is enabled or exists in the system
//            log.debug("Service status validation successful");
//        } catch (TokenValidationFailureException e) {
//            log.warn("Service status validation failed: {}", e.getMessage());
//            throw e;
//        }
//    }

    /**
     * Validates token metadata including type and audience
     */
    private void validateTokenMetadata(Map<String, Object> claims, String expectedType)
            throws TokenValidationFailureException {
        validateRequiredClaims(claims);
        validateTokenType(claims, expectedType);
    }

    /**
     * Validates all time-related claims
     */
    private void validateTokenTiming(Map<String, Object> claims) throws TokenValidationFailureException {
        validateExpiration(claims);
        validateNotBefore(claims);
        validateIssuanceTime(claims);
    }

    /**
     * Validates token identity claims including type, issuer, audience, and subject
     */
    private void validateTokenIdentity(Map<String, Object> claims)
            throws TokenValidationFailureException {
        validateIssuer(claims);
        validateAudience(claims);
        validateSubject(claims);
    }

    /**
     * Validates session ID
     * Checks if the session ID in the token matches the session ID of the current HTTP session
     *
     * @param claims
     */
    private void validateTokenSessionId(Map<String, Object> claims) {
        String sessionId = tokenUtilService.extractSessionId(claims);
        if (!httpServletRequest.getSession().getId().equals(sessionId)) {
            // Session ID mismatch, revoke the token
            String tokenId = tokenUtilService.extractTokenId(claims);
            if (StringUtils.isNotBlank(tokenId)) {
                tokenRevocationService.revokeToken(tokenId, "Session ID mismatch");
            }
            throw new TokenValidationFailureException("Session ID mismatch");
        }

    }

    /**
     * Validates that all required claims are present. Efficient as it stops after the first missing claim.
     */
    private void validateRequiredClaims(Map<String, Object> claims) throws TokenValidationFailureException {
        for (String requiredClaim : REQUIRED_ACCESS_TOKEN_CLAIMS) {
            if (!claims.containsKey(requiredClaim)) {
                // Found a missing claim, handle it immediately
                handleMissingClaim(claims, requiredClaim);
                return; // Exit after the first missing claim
            }
        }
    }

    /**
     * Handles missing claims by revoking the token and throwing an exception
     *
     * @param claims       token claims
     * @param missingClaim missing claim name
     * @throws TokenValidationFailureException
     */
    private void handleMissingClaim(Map<String, Object> claims, String missingClaim) throws TokenValidationFailureException {
        String errorMessage = String.format(MISSING_REQUIRED_CLAIM, missingClaim); // Use String.format()

        // Revoke the token
        String tokenId = (String) claims.get("jti");
        if (tokenId != null) {
            tokenRevocationService.revokeToken(tokenId, String.format(TOKEN_REVOKED_MISSING_CLAIM, missingClaim));
        } else {
            log.error(TOKEN_ID_IS_MISSING_CANNOT_REVOKE);
        }

        // Log the error
        log.error(errorMessage);

        // Throw the exception
        throw new InvalidTokenException(errorMessage);
    }

    /**
     * Validates token expiration
     */
    private void validateExpiration(Map<String, Object> claims)
            throws TokenValidationFailureException {
        Instant expiration = tokenUtilService.extractClaimInstant(claims, ClaimKeys.EXPIRATION);
        if (Instant.now().isAfter(expiration.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
            // The Token has expired, so revoke it
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_EXPIRED);
            log.error("At expiration validation, Token with id {} has expired or possible replay attack detected", tokenId);
            throw new TokenValidationFailureException(TOKEN_EXPIRED);
        }
    }

    /**
     * Validates token not before
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token not before is not valid
     */
    private void validateNotBefore(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant nbf = tokenUtilService.extractClaimInstant(claims, ClaimKeys.NOT_BEFORE);
        if (Instant.now().minus(Constants.CLOCK_SKEW_TOLERANCE).isBefore(nbf)) {
            // Token is not yet valid, so revoke it as possible replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_NOT_YET_VALID);
            log.error("At not before validation, Token with id {} is not yet valid, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException(TOKEN_NOT_YET_VALID);
        }
    }

    /**
     * Validates token issuance time
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token issuance time is not valid
     */
    private void validateIssuanceTime(Map<String, Object> claims) throws TokenValidationFailureException {
        long startTime = System.nanoTime();
        try {
            Object iatClaim = claims.get(ClaimKeys.ISSUED_AT);
            if (iatClaim == null) {
                throw new TokenValidationFailureException("Missing issuance time claim");
            }

            Instant iat = tokenUtilService.extractClaimInstant(claims, ClaimKeys.ISSUED_AT);

            if (Instant.now().minus(Constants.CLOCK_SKEW_TOLERANCE).isBefore(iat)) {
                // Token issuance time is not valid, so revoke it as possible replay attack or another security issue
                String tokenId = tokenUtilService.extractTokenId(claims);
                tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_ISSUED_IN_THE_FUTURE);
                log.error("At issuance time validation, Token with id {} was issued in the future, possible replay attack or another security issue", tokenId);
                throw new TokenValidationFailureException(TOKEN_ISSUED_IN_THE_FUTURE);
            }
        } catch (DateTimeParseException e) {
            throw new TokenValidationFailureException("Invalid issuance time format", e);
        } finally {
            long duration = System.nanoTime() - startTime;
            log.debug("Validation took {} nanoseconds", duration);
        }
    }

    /**
     * Validates token issuer
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token issuer is not valid
     */
    private void validateIssuer(Map<String, Object> claims) throws TokenValidationFailureException {
        String issuer = claims.get(ClaimKeys.ISSUER).toString();
        if (!TokenConstants.DEFAULT_ISSUER.equals(issuer)) {
            // Token issuer is not valid, so revoke it as possible Issuer Spoofing/Impersonation, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_INVALID_ISSUER);
            log.error("At issuer validation, Token with id {} issuer is not valid, expecting {} but found {}.Possible Issuer Spoofing/Impersonation, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_ISSUER, issuer);
            throw new TokenValidationFailureException(INVALID_TOKEN_ISSUER);
        }
    }

    /**
     * Validates token type
     * ACCESS_TOKEN or REFRESH_TOKEN
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token type is not valid
     */
    private void validateTokenType(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        try {
            String tokenType = claims.get(ClaimKeys.TOKEN_TYPE).toString();
            if (!expectedType.equals(tokenType)) {
                // Token type is not valid, so revoke it as possible Token Confusion/Type Mismatch attacks, replay attack or another security issue
                String tokenId = claims.get(ClaimKeys.PASETO_ID).toString();
                tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_INVALID_TOKEN_TYPE);
                log.error("At token type validation, Token type is not valid, expected {} but got {}. Possible Token Confusion/Type Mismatch attacks, replay attack or another security issue", expectedType, tokenType);
                throw new TokenValidationFailureException(INVALID_TOKEN_TYPE);
            }
            log.debug("Token type validation successful");
        } catch (NullPointerException e) {
            throw new TokenValidationFailureException(TOKEN_TYPE_CLAIM_IS_MISSING, e);
        }
    }

    /**
     * Validates token audience
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token audience is not valid
     */
    private void validateAudience(Map<String, Object> claims) throws TokenValidationFailureException {
        String audience = claims.get(ClaimKeys.AUDIENCE).toString();
        if (!DEFAULT_AUDIENCE.equals(audience)) {
            // Token audience is not valid, so revoke it as possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_INVALID_AUDIENCE);
            log.error("At audience validation, Token with id {} audience is not valid, expecting {} but found {}. Possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue", tokenId, DEFAULT_AUDIENCE, audience);
            throw new TokenValidationFailureException(INVALID_AUDIENCE);
        }
    }

    /**
     * Validates token subject
     * User ID
     *
     * @param claims Map<String, Object>
     * @throws TokenValidationFailureException if the token subject is not valid
     */
    private void validateSubject(Map<String, Object> claims) throws TokenValidationFailureException {
        String subject = claims.get(ClaimKeys.SUBJECT).toString();
        String userId = httpServletRequest.getSession().getAttribute("userId").toString();
        if (!subject.equals(userId)) {
            // Token subject is not valid, so revoke it as possible Subject Impersonation/Authorization Bypass, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, TOKEN_REVOKED_INVALID_SUBJECT);
            log.error("At subject validation, Token with id {} subject is not valid, expecting {} but found {}. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId, userId, subject);
            throw new TokenValidationFailureException(INVALID_SUBJECT);
        }
    }

    /**
     * Extracts claims from public access token
     * It was originally intended to be in the TokenUtilService class, but it was moved here to avoid excessive dependency injection
     *
     * @param token String
     * @return Map<String, Object>
     */
    @Override
    public Map<String, Object> extractClaimsFromPublicAccessToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null or empty"); // Checks if parameter is null
        if (token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty"); // Checks if parameter is empty/whitespace
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != PASETO_TOKEN_PARTS_LENGTH) {
                throw new IllegalArgumentException(INVALID_TOKEN_FORMAT);
            }

            // Validate that each part is not empty
            StringBuilder invalidParts = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] == null || parts[i].trim().isEmpty()) {
                    invalidParts.append(i).append(", ");
                }
            }
            if (!invalidParts.isEmpty()) {
                invalidParts.setLength(invalidParts.length() - 2); // Remove trailing ", "
                throw new IllegalArgumentException(String.format(INVALID_TOKEN_FORMAT_INVALID_PARTS, invalidParts));
            }

            String payload = Paseto.parse(keyManagementService.getAccessPublicKey(), parts[2], parts[3]);

            // Null check after parsing
            if (payload == null) {
                throw new ClaimsExtractionFailureException(NULL_PAYLOAD_AFTER_PARSING);
            }

            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });

        } catch (IllegalArgumentException e) {
            throw new TokenValidationFailureException("Invalid token format: " + e.getMessage(), e); // Add context when re-throwing
        } catch (Exception e) {
            throw new ClaimsExtractionFailureException(ERROR_EXTRACTING_CLAIMS_FROM_TOKEN, e);
        }
    }

    /**
     * Claim key constants
     */
    private static final class ClaimKeys {
        private static final String ISSUER = "iss";
        private static final String SUBJECT = "sub";
        private static final String AUDIENCE = "aud";
        private static final String EXPIRATION = "exp";
        private static final String NOT_BEFORE = "nbf";
        private static final String ISSUED_AT = "iat";
        private static final String PASETO_ID = "jti";
        private static final String SCOPE = "scope";
        private static final String TOKEN_TYPE = "type";

        private ClaimKeys() {
        } // Prevent instantiation
    }

    /**
     * Required claims for access tokens
     */
    private static final Set<String> REQUIRED_ACCESS_TOKEN_CLAIMS = Set.of(
            ClaimKeys.ISSUER,
            ClaimKeys.SUBJECT,
            ClaimKeys.AUDIENCE,
            ClaimKeys.EXPIRATION,
            ClaimKeys.NOT_BEFORE,
            ClaimKeys.ISSUED_AT,
            ClaimKeys.PASETO_ID,
            ClaimKeys.SCOPE,
            ClaimKeys.TOKEN_TYPE
    );
}

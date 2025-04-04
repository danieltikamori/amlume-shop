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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.paseto.util.PasetoPropertyResolver;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.version4.Paseto;
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

import static me.amlu.shop.amlume_shop.exceptions.ErrorMessages.*;
import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.DEFAULT_AUDIENCE;
import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.PASETO_TOKEN_PARTS_LENGTH;

//Single Responsibility Principle :
//
//TokenValidationService should focus on token validation (structure, signature, expiration)
//
//TokenClaimService should handle claim-specific operations (extraction, validation, processing)
//
//Separation of Concerns :
//
//Claims validation is a distinct responsibility from token validation
//
//Token validation checks if the token is valid structurally and cryptographically
//
//Claims validation checks the content/business rules within the token
//
//Maintainability :
//
//Keeping claims logic separate makes the code more maintainable
//
//Easier to modify claim requirements without touching token validation
//
//Better testability as you can test claims validation independently

//public class TokenValidationServiceImpl {
//    // Handles token structure, signature, expiration
//    public boolean validateToken(String token) {
//        // Validate token structure
//        // Check signature
//        // Verify expiration
//    }
//}


@Slf4j
@Service
public class TokenValidationServiceImpl implements TokenValidationService {

    private final ObjectMapper objectMapper;
    private final KeyManagementService keyManagementService;
    private final TokenUtilService tokenUtilService;
    private final TokenCacheService tokenCacheService;
    private final TokenClaimsService tokenClaimsService;
    private final TokenRevocationService tokenRevocationService;
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasetoPropertyResolver pasetoPropertyResolver;
    private final TokenConfigurationService tokenConfigurationService;

    private final RateLimiter tokenValidationRateLimiter;
    private final RateLimiter claimsValidationRateLimiter;

//    @Value("${rateLimit.token.validation}")
//    private double tokenValidationRateLimiterValue;
//
//    @Value("${rateLimit.claims.validation}")
//    private double claimsValidationRateLimiterValue;

//    @Value("${paseto.access.kid}")
//    private String pasetoAccessKid;
//
//    @Value("${paseto.access.local.kid}")
//    private String pasetoAccessLocalKid;
//
//    @Value("${paseto.refresh.local.kid}")
//    private String pasetoRefreshLocalKid;

    // Metrics
    private final PrometheusMeterRegistry meterRegistry;

    private final Summary tokenValidationLatency = Summary.build()
            .name("paseto_token_validation_seconds")
            .help(TokenConstants.TIME_SPENT_VALIDATING_PASETO_TOKENS)
            .quantile(0.5, 0.05)    // Add 50th percentile with 5% tolerance
            .quantile(0.95, 0.01)   // Add 95th percentile with 1% tolerance
            .maxAgeSeconds(300) // 5 minutes maximum age for samples
            .ageBuckets(5)
            .register();

    private final Counter tokenValidationCounter;
    private final Timer tokenValidationTimer;


    public TokenValidationServiceImpl(ObjectMapper objectMapper, KeyManagementService keyManagementService, TokenUtilService tokenUtilService, TokenCacheService tokenCacheService, TokenClaimsService tokenClaimsService, TokenRevocationService tokenRevocationService, HttpServletRequest httpServletRequest, UserRepository userRepository, RevokedTokenRepository revokedTokenRepository, PrometheusMeterRegistry meterRegistry, Counter tokenValidationCounter, Timer tokenValidationTimer, PasetoPropertyResolver pasetoPropertyResolver, TokenConfigurationService tokenConfigurationService) {
        this.objectMapper = objectMapper;
        this.keyManagementService = keyManagementService;
        this.tokenUtilService = tokenUtilService;
        this.tokenCacheService = tokenCacheService;
        this.tokenClaimsService = tokenClaimsService;
        this.tokenRevocationService = tokenRevocationService;
        this.httpServletRequest = httpServletRequest;
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.meterRegistry = meterRegistry;
        this.pasetoPropertyResolver = pasetoPropertyResolver;
        this.tokenConfigurationService = tokenConfigurationService;
        this.tokenValidationCounter = Counter.build()
                .name("paseto_token_validation_total")
                .help("Total number of PASETO token validations")
                .register();
        this.tokenValidationTimer = Timer.builder("paseto.token.validation")
                .description(TokenConstants.TIME_SPENT_VALIDATING_PASETO_TOKENS)
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);
        this.tokenValidationRateLimiter = RateLimiter.create(tokenConfigurationService.getValidationRateLimitPermitsPerSecond());
        this.claimsValidationRateLimiter = RateLimiter.create(tokenConfigurationService.getClaimsValidationRateLimitPermitsPerSecond());
    }

    private Timer createTokenValidationTimer(MeterRegistry registry) {
        return Timer.builder("paseto.token.validation")
                .description(TokenConstants.TIME_SPENT_VALIDATING_PASETO_TOKENS)
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);
    }

//    // Rate limiter for token validation to prevent abuse and ensure rate limiting
//    private final RateLimiter tokenValidationRateLimiter = RateLimiter.create(tokenConfigurationService.getValidationRateLimitPermitsPerSecond());
//    private final RateLimiter claimsValidationRateLimiter = RateLimiter.create(tokenConfigurationService.getClaimsValidationRateLimitPermitsPerSecond());

    // Add structured logging
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
        tokenValidationRateLimiter.tryAcquire();

        Summary.Timer timer = tokenValidationLatency.startTimer();
        try {
            //            Timer.Sample sample = Timer.start();
//            Timer.Sample sample = Timer.start(meterRegistry);
            log.debug("Validating public access token");

            // Check if token is blacklisted
//            isTokenBlacklisted();

            validateTokenStringLength(token);

            String[] parts = splitToken(token);
            String signedMessage = createSignedMessage(parts);
            String footer = extractFooter(parts);
            String payload = parseAndVerifyToken(signedMessage, footer);
            Map<String, Object> claims = parseClaims(payload);
            validatePublicAccessKid(claims);
            validatePublicAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));
//            sample.stop(tokenValidationTimer);
            log.debug("Public access token validation successful");
            return claims;
        } catch (TokenValidationFailureException e) {
            handlePublicAccessTokenValidationException(e);
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED), e);
        } catch (JsonProcessingException e) {
            throw new ClaimsParsingException("Error parsing claims", e);
        } catch (SignatureException e) {
            throw new SignatureException("Invalid PASETO signature", e);
        } finally {
            timer.observeDuration();
            //            sample.stop(tokenValidationTimer);
        }
    }

    @Override
    public Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException {
        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(keyManagementService.getAccessSecretKey(), parts[TokenConstants.PASETO_TOKEN_PAYLOAD_PLACE], parts[TokenConstants.PASETO_TOKEN_FOOTER_PLACE]);

            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // Validate kid from footer claims
            validateLocalAccessKid(claims);

            validateLocalAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));

            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ERROR_PARSING_CLAIMS), e);

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN), e);

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate local access PASETO token", e);
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED), e);
        }
    }


    @Override
    public Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException { // Separate refresh token validation
        tokenValidationRateLimiter.acquire();

        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(keyManagementService.getRefreshSecretKey(), parts[TokenConstants.PASETO_TOKEN_PAYLOAD_PLACE], parts[TokenConstants.PASETO_TOKEN_FOOTER_PLACE]);

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
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED), e);
        }
    }

//    private static void isTokenBlacklisted() {
//        // Check if token is blacklisted
//        String jti = (String) claims.get(TokenClaims.JTI);
//        if (tokenBlacklistService.isBlacklisted(jti)) {
//            throw new TokenValidationFailureException("Token has been revoked");
//        }
//    }


    private void validateTokenStringLength(String token) throws InvalidTokenLengthException {
        Objects.requireNonNull(token, "Token cannot be null");

        if (token.length() < TokenConstants.MIN_TOKEN_LENGTH || token.length() > TokenConstants.MAX_TOKEN_LENGTH) {
            throw new InvalidTokenLengthException("Token length out of acceptable range");
        }
    }


    private String[] splitToken(String token) throws InvalidTokenFormatException {
        String[] parts = token.split("\\.");
        if (parts.length > PASETO_TOKEN_PARTS_LENGTH || parts.length < PASETO_TOKEN_PARTS_LENGTH) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
        return parts;
    }

    private String createSignedMessage(String[] parts) throws InvalidTokenFormatException {
        if (parts[2].isBlank()) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
//        return parts[0] + "." + parts[1];
        return parts[TokenConstants.PASETO_TOKEN_PAYLOAD_PLACE]; // Payload part of the token
    }

    private String extractFooter(String[] parts) {
        if (parts[3].isBlank()) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
//        return parts.length == PASETO_TOKEN_PARTS_LENGTH ? parts[3] : ""; // Use this if the there's tokens with AND without footers
        return parts[TokenConstants.PASETO_TOKEN_FOOTER_PLACE]; // Footer part of the token
    }

    private String parseAndVerifyToken(String signedMessage, String footer) throws SignatureException {
        try {
            return Paseto.parse(keyManagementService.getAccessPublicKey(), signedMessage, footer);
        } catch (SignatureException e) {
            log.error("Invalid PASETO signature", e);
            throw new SignatureException("Invalid PASETO signature", e);
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
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException("Error parsing claims", e);
        }
    }

    private void validatePublicAccessKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
        }
        if (!pasetoPropertyResolver.resolvePublicAccessKid().equals(kid)) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
        }
    }

    private void validateLocalAccessKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
        }
        if (!pasetoPropertyResolver.resolveLocalAccessKid().equals(kid)) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
        }
    }

    private void validateLocalRefreshKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
        }
        if (!pasetoPropertyResolver.resolveLocalRefreshKid().equals(kid)) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
        }
    }


    private void handlePublicAccessTokenValidationException(Exception e) throws TokenValidationFailureException {
        if (e instanceof SignatureException) {
            log.error("Invalid PASETO signature", e);
            throw new InvalidTokenSignatureException("Invalid PASETO signature", e);
        } else if (e instanceof JsonProcessingException) {
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ERROR_PARSING_CLAIMS), e);
        } else if (e instanceof IllegalArgumentException) {
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN), e);
        } else if (e instanceof TokenValidationFailureException) {
            throw new TokenValidationFailureException("Token validation failed", e); // TOCHECK
        } else if (e instanceof PasetoException) {
            log.error("PASETO-specific error occurred", e);
            throw new TokenValidationFailureException("PASETO-specific error occurred", e);
        } else {
            log.error("Unexpected error during public access PASETO token validation", e);
            throw new TokenValidationFailureException("Unexpected error during token validation", e);
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

        claimsValidationRateLimiter.tryAcquire();

        validateClaimsSize(claims);
        validateTokenSessionId(claims);
        validateUserStatus(claims);
        validateTokenMetadata(claims, expectedType);
        validateTokenTiming(claims);
        validateTokenIdentity(claims);
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

        claimsValidationRateLimiter.tryAcquire();
        validateClaimsSize(claims);
        validateTokenSessionId(claims);
        validateServiceStatus(claims);
        validateTokenMetadata(claims, expectedType);
        validateTokenTiming(claims);
        validateTokenIdentity(claims);
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

        claimsValidationRateLimiter.tryAcquire();

        validateClaimsSize(claims);
        validateUserStatus(claims);
        validateTokenMetadata(claims, expectedType);
        validateTokenTiming(claims);
        validateTokenIdentity(claims);
    }

    @Override
    public void validateClaimsSize(Map<String, Object> claims) throws ClaimsSizeException {
        try {
            String claimsJson = objectMapper.writeValueAsString(claims);
            if (claimsJson.length() > TokenConstants.MAX_CLAIMS_SIZE) {
                throw new ClaimsSizeException("Claims payload exceeds maximum size");
            }
        } catch (JsonProcessingException e) {
            throw new ClaimsSizeException("Error serializing claims to JSON", e);
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
            throw new TokenValidationFailureException("User account is disabled");
        }
    }

    private void validateUserScope(Map<String, Object> claims) throws TokenValidationFailureException {
        String userScope = tokenUtilService.extractUserScope(claims);

        Set<UserRole> roles = userRepository.findRolesByUserId(Long.valueOf(tokenUtilService.extractUserId(claims)));

        if (Objects.isNull(roles) || roles.isEmpty()) {
            throw new TokenValidationFailureException("No roles found for user");
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
//            throw new TokenValidationFailureException("Invalid user scope. Expected: " + expectedScopes + ", Actual: " + actualScopes);
        }
    }

    /**
     * Validates service status and existence
     * Local access token validation
     * For Kubernetes, this method should be modified to check the Kubernetes API for service status and authorization
     */
    private void validateServiceStatus(Map<String, Object> claims) throws TokenValidationFailureException {
        // Finish implementation if required to Kubernetes
//        log.debug("Validating service status for token");
//        try {
//            String serviceId = tokenUtilService.extractServiceId(claims);
//            Service service= tokenUtilService.getServiceId(serviceId);
//            validateServiceEnabled(service);
//            validateNotRevoked(claims);
//            validateServiceScope(claims);
//            log.debug("Service status validation successful");
//        } catch (TokenValidationFailureException e) {
//            log.warn("Service status validation failed: {}", e.getMessage());
//            throw e;
//        }
    }

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
                try {
                    tokenRevocationService.revokeToken(tokenId, "Session ID mismatch");
                } catch (TokenRevocationException e) {
                    log.error("Failed to revoke token: {}", e.getMessage());
                }
            }
            throw new TokenValidationFailureException("Session ID mismatch");
        }

    }

    /**
     * Less efficient as creates a set of ALL missing claims. Validates that all required claims are present
     */
//    @Override
//    public void validateRequiredClaims(Map<String, Object> claims, Set<String> requiredClaims) throws TokenValidationFailureException {
//        Set<String> missingClaims = new HashSet<>();
//        for (String claimName : requiredClaims) {
//            if (!claims.containsKey(claimName)) {
//                missingClaims.add(claimName);
//            }
//        }
//        if (!missingClaims.isEmpty()) {
//            throw new TokenValidationFailureException("Missing required claims: " + String.join(", ", missingClaims));
//        }
//    }

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
     * @param claims
     * @param missingClaim
     * @throws TokenValidationFailureException
     */
    private void handleMissingClaim(Map<String, Object> claims, String missingClaim) throws TokenValidationFailureException {
        String errorMessage = "Missing required claim: " + missingClaim + ". Possible token tampering.";

        // Revoke the token
        String tokenId = (String) claims.get("jti");
        if (tokenId != null) {
            try {
                tokenRevocationService.revokeToken(tokenId, errorMessage);
            } catch (TokenRevocationException e) {
                log.error("Failed to revoke token: {}", e.getMessage());
            }
        } else {
            log.error("Token ID (jti) is missing, cannot revoke.");
        }

        // Log the error
        log.error("Missing required claim: {}", missingClaim);

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
            // Token has expired, so revoke it
            String tokenId = tokenUtilService.extractTokenId(claims);
            try {
                tokenRevocationService.revokeToken(tokenId, "Token has expired or possible replay attack detected");
            } catch (Exception e) {
                log.error("Failed to revoke expired token: {}", e.getMessage());
            }
            log.error("At expiration validation, Token with id {} has expired or possible replay attack detected", tokenId);
            throw new TokenValidationFailureException("Token has expired");

        }
    }

    /**
     * Validates token not before
     *
     * @param claims
     * @throws TokenValidationFailureException
     */
    private void validateNotBefore(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant nbf = tokenUtilService.extractClaimInstant(claims, ClaimKeys.NOT_BEFORE);
        if (Instant.now().minus(Constants.CLOCK_SKEW_TOLERANCE).isBefore(nbf)) {
            // Token is not yet valid, so revoke it as possible replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, "At not before validation, Token is not yet valid (possible replay attack)");
            log.error("At not before validation, Token with id {} is not yet valid, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException("Token is not yet valid");
        }
    }

    /**
     * Validates token issuance time
     *
     * @param claims
     * @throws TokenValidationFailureException
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
                tokenRevocationService.revokeToken(tokenId, "At issuance time validation, Token issuance time is not valid (possible replay attack or another security issue)");
                log.error("At issuance time validation, Token with id {} was issued in the future, possible replay attack or another security issue", tokenId);
                throw new TokenValidationFailureException("Token issuance time is not valid");
            }
        } catch (DateTimeParseException e) {
            throw new TokenValidationFailureException("Invalid issuance time format", e);
        } finally {
            long duration = System.nanoTime() - startTime;
//            metricRegistry.recordTimingMetric("token.validation.issuance_time", duration);
        }
    }

    /**
     * Validates token issuer
     *
     * @param claims
     * @throws TokenValidationFailureException
     */
    private void validateIssuer(Map<String, Object> claims) throws TokenValidationFailureException {
        String issuer = claims.get(ClaimKeys.ISSUER).toString();
        if (!TokenConstants.DEFAULT_ISSUER.equals(issuer)) {
            // Token issuer is not valid, so revoke it as possible Issuer Spoofing/Impersonation, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, "At issuer validation, Token issuer is not valid (possible Issuer Spoofing/Impersonation, replay attack or another security issue)");
            log.error("At issuer validation, Token with id {} issuer is not valid, expecting {} but found {}.Possible Issuer Spoofing/Impersonation, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_ISSUER, issuer);
            throw new TokenValidationFailureException("Invalid token issuer");
        }
    }

    /**
     * Validates token type
     * ACCESS_TOKEN or REFRESH_TOKEN
     *
     * @param claims
     * @throws TokenValidationFailureException
     */
    private void validateTokenType(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        try {
            String tokenType = claims.get(ClaimKeys.TOKEN_TYPE).toString();
            if (!expectedType.equals(tokenType)) {
                // Token type is not valid, so revoke it as possible Token Confusion/Type Mismatch attacks, replay attack or another security issue
                String tokenId = claims.get(ClaimKeys.PASETO_ID).toString();
                tokenRevocationService.revokeToken(tokenId, "At token type validation, Token type is not valid (possible Token Confusion/Type Mismatch attacks, replay attack or another security issue)");
                log.error("At token type validation, Token type is not valid, expected {} but got {}. Possible Token Confusion/Type Mismatch attacks, replay attack or another security issue", expectedType, tokenType);
                throw new TokenValidationFailureException("Invalid token type");
            }
            log.debug("Token type validation successful");
        } catch (NullPointerException e) {
            throw new TokenValidationFailureException("Token type claim is missing", e);
        }
    }

    /**
     * Validates token audience
     *
     * @param claims
     * @throws TokenValidationFailureException
     */
    private void validateAudience(Map<String, Object> claims) throws TokenValidationFailureException {
        String audience = claims.get(ClaimKeys.AUDIENCE).toString();
        if (!DEFAULT_AUDIENCE.equals(audience)) {
            // Token audience is not valid, so revoke it as possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, "At audience validation, Token audience is not valid (possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue)");
            log.error("At audience validation, Token with id {} audience is not valid, expecting {} but found {}. Possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue", tokenId, DEFAULT_AUDIENCE, audience);
            throw new TokenValidationFailureException("Invalid audience");
        }
    }

    /**
     * Validates token subject
     * User ID
     *
     * @param claims
     * @throws TokenValidationFailureException
     */
    private void validateSubject(Map<String, Object> claims) throws TokenValidationFailureException {

        String subject = claims.get(ClaimKeys.SUBJECT).toString();
        String userId = httpServletRequest.getSession().getAttribute("userId").toString();
        if (!subject.equals(userId)) {
            // Token subject is not valid, so revoke it as possible Subject Impersonation/Authorization Bypass, replay attack or another security issue
            String tokenId = tokenUtilService.extractTokenId(claims);
            tokenRevocationService.revokeToken(tokenId, "At subject validation, Token subject is not valid (possible Subject Impersonation/Authorization Bypass, replay attack or another security issue)");
            log.error("At subject validation, Token with id {} subject is not valid, expecting {} but found {}. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId, userId, subject);
            throw new TokenValidationFailureException("Invalid subject");
        }
    }

    /**
     * Extracts claims from public access token
     * It was originally intended to be in the TokenUtilService class, but it was moved here to avoid excessive dependency injection
     *
     * @param token
     * @return
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
                throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT));
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
                throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT) + ". Invalid parts: " + invalidParts);
            }

            String payload = Paseto.parse(keyManagementService.getAccessPublicKey(), parts[2], parts[3]);

            // Null check after parsing
            if (payload == null) {
                throw new ClaimsExtractionFailureException("Null payload after parsing");
            }

            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException e) {
            throw new TokenValidationFailureException("Invalid token format: " + e.getMessage(), e); // Add context when re-throwing
        } catch (Exception e) {
            throw new ClaimsExtractionFailureException("Error extracting claims from token", e);
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

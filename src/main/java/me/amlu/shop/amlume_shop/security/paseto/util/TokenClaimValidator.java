/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto.util;

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.InvalidTokenException;
import me.amlu.shop.amlume_shop.exceptions.TokenRevokedException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class TokenClaimValidator {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TokenClaimValidator.class);
    private final HttpServletRequest httpServletRequest;
    private final TokenRevocationService tokenRevocationService; // Keep this
    private final UserRepository userRepository;

    public TokenClaimValidator(HttpServletRequest httpServletRequest, TokenRevocationService tokenRevocationService, UserRepository userRepository) {
        this.httpServletRequest = httpServletRequest;
        this.tokenRevocationService = tokenRevocationService;
        this.userRepository = userRepository;
    }

    // This method now delegates entirely to the service layer
    public void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException {
        // Delegate the check to the service, which handles caching and DB lookup
        tokenRevocationService.validateNotRevoked(claims);
    }

    public void validateRequiredClaims(Map<String, Object> claims) throws InvalidTokenException {
        // Ensure 'jti' is also considered required if used for revocation
        List<String> requiredClaims = Arrays.asList("exp", "iat", "nbf", "sub", "iss", "jti");
        List<String> missingClaims = requiredClaims.stream()
                .filter(claim -> !claims.containsKey(claim) || claims.get(claim) == null || claims.get(claim).toString().isBlank()) // Check for null/blank too
                .toList(); // Unmodifiable list

        if (!missingClaims.isEmpty()) {
            String missingClaimsStr = String.join(", ", missingClaims);
            log.error("Token validation failed: Missing required claims: {}", missingClaimsStr);
            // Attempt to revoke only if JTI is present, otherwise, it's impossible
            Object tokenIdClaim = claims.get("jti");
            if (tokenIdClaim != null && !tokenIdClaim.toString().isBlank()) {
                String tokenId = tokenIdClaim.toString();
                try {
                    tokenRevocationService.revokeToken(tokenId, "Missing required claims: " + missingClaimsStr + ". Possible token tampering.");
                } catch (Exception e) {
                    log.error("Failed to revoke token {} during missing claim validation: {}", tokenId, e.getMessage());
                }
            } else {
                log.warn("Cannot revoke token due to missing 'jti' claim, along with other missing claims: {}", missingClaimsStr);
            }
            throw new InvalidTokenException("Missing required claims: " + missingClaimsStr + ". Possible token tampering.");
        }
    }

    // --- Other validation methods (validateExpiration, validateNotBefore, etc.) remain largely the same ---
    // Make sure they use tokenRevocationService.revokeToken() correctly when needed.

    public void validateExpiration(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();
        Object expClaim = claims.get("exp");
        if (expClaim == null) {
            throw new TokenValidationFailureException("Expiration claim is missing");
        }

        try {
            Instant exp = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(expClaim.toString()));
            if (now.isAfter(exp.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
                String tokenId = claims.get("jti").toString(); // Assumes jti is present (validated by validateRequiredClaims)
                log.error("At expiration validation, Token with id {} has expired or possible replay attack detected", tokenId);
                try {
                    tokenRevocationService.revokeToken(tokenId, "Token has expired or possible replay attack detected");
                } catch (Exception e) {
                    log.error("Failed to revoke expired token {}: {}", tokenId, e.getMessage());
                }
                throw new TokenValidationFailureException("Token has expired");
            }
        } catch (Exception e) { // Catch parsing errors too
            log.error("Error parsing expiration claim: {}", expClaim, e);
            throw new TokenValidationFailureException("Invalid expiration claim format", e);
        }
    }

    public void validateNotBefore(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();
        Object nbfClaim = claims.get("nbf");
        if (nbfClaim == null) {
            throw new TokenValidationFailureException("NotBefore claim is missing");
        }

        try {
            Instant nbf = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(nbfClaim.toString()));
            if (now.isBefore(nbf.minus(Constants.CLOCK_SKEW_TOLERANCE))) {
                String tokenId = claims.get("jti").toString();
                log.error("At not before validation, Token with id {} is not yet valid, possible replay attack or another security issue", tokenId);
                try {
                    tokenRevocationService.revokeToken(tokenId, "At not before validation, Token is not yet valid (possible replay attack)");
                } catch (Exception e) {
                    log.error("Failed to revoke not-yet-valid token {}: {}", tokenId, e.getMessage());
                }
                throw new TokenValidationFailureException("Token is not yet valid");
            }
        } catch (Exception e) {
            log.error("Error parsing NotBefore claim: {}", nbfClaim, e);
            throw new TokenValidationFailureException("Invalid NotBefore claim format", e);
        }
    }

    public void validateIssuanceTime(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();
        Object iatClaim = claims.get("iat");
        if (iatClaim == null) {
            throw new TokenValidationFailureException("IssuedAt claim is missing");
        }

        try {
            Instant iat = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(iatClaim.toString()));
            if (iat.isAfter(now.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
                String tokenId = claims.get("jti").toString();
                log.error("At issuance time validation, Token with id {} was issued in the future, possible replay attack or another security issue", tokenId);
                try {
                    tokenRevocationService.revokeToken(tokenId, "At issuance time validation, Token issuance time is not valid (possible replay attack or another security issue)");
                } catch (Exception e) {
                    log.error("Failed to revoke future-issued token {}: {}", tokenId, e.getMessage());
                }
                throw new TokenValidationFailureException("Token issuance time is not valid");
            }
        } catch (Exception e) {
            log.error("Error parsing IssuedAt claim: {}", iatClaim, e);
            throw new TokenValidationFailureException("Invalid IssuedAt claim format", e);
        }
    }

    public void validateIssuer(Map<String, Object> claims) throws TokenValidationFailureException {
        Object issuerClaim = claims.get("iss");
        if (issuerClaim == null) {
            throw new TokenValidationFailureException("Issuer claim is missing");
        }
        String issuer = issuerClaim.toString();
        if (!TokenConstants.DEFAULT_ISSUER.equals(issuer)) {
            String tokenId = claims.get("jti").toString();
            log.error("At issuer validation, Token with id {} issuer is not valid, expecting {} but found {}. Possible Issuer Spoofing/Impersonation, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_ISSUER, issuer);
            try {
                tokenRevocationService.revokeToken(tokenId, "At issuer validation, Token issuer is not valid (possible Issuer Spoofing/Impersonation, replay attack or another security issue)");
            } catch (Exception e) {
                log.error("Failed to revoke invalid-issuer token {}: {}", tokenId, e.getMessage());
            }
            throw new TokenValidationFailureException("Invalid token issuer");
        }
    }

    public void validateTokenType(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        Object tokenTypeClaim = claims.get("type");
        if (tokenTypeClaim == null) {
            throw new TokenValidationFailureException("Token type claim is missing");
        }
        String tokenType = tokenTypeClaim.toString();
        if (!expectedType.equals(tokenType)) {
            String tokenId = claims.get("jti").toString();
            log.error("At token type validation, Token type is not valid, expected {} but got {}. Possible Token Confusion/Type Mismatch attacks, replay attack or another security issue", expectedType, tokenType);
            try {
                tokenRevocationService.revokeToken(tokenId, "At token type validation, Token type is not valid (possible Token Confusion/Type Mismatch attacks, replay attack or another security issue)");
            } catch (Exception e) {
                log.error("Failed to revoke invalid-type token {}: {}", tokenId, e.getMessage());
            }
            throw new TokenValidationFailureException("Invalid token type");
        }
    }

    public void validateAudience(Map<String, Object> claims) throws TokenValidationFailureException {
        Object audienceClaim = claims.get("aud");
        if (audienceClaim == null) {
            throw new TokenValidationFailureException("Audience claim is missing");
        }
        String audience = audienceClaim.toString();
        if (!TokenConstants.DEFAULT_AUDIENCE.equals(audience)) {
            String tokenId = claims.get("jti").toString();
            log.error("At audience validation, Token with id {} audience is not valid, expecting {} but found {}. Possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_AUDIENCE, audience);
            try {
                tokenRevocationService.revokeToken(tokenId, "At audience validation, Token audience is not valid (possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue)");
            } catch (Exception e) {
                log.error("Failed to revoke invalid-audience token {}: {}", tokenId, e.getMessage());
            }
            throw new TokenValidationFailureException("Invalid audience");
        }
    }

    public void validateSubject(Map<String, Object> claims) throws TokenValidationFailureException {
        Object subjectClaim = claims.get("sub");
        if (subjectClaim == null || subjectClaim.toString().isBlank()) {
            String tokenId = claims.get("jti") != null ? claims.get("jti").toString() : "[unknown_jti]";
            log.error("At subject validation, Token with id {} has blank or missing subject.", tokenId);
            if (!"[unknown_jti]".equals(tokenId)) {
                try {
                    tokenRevocationService.revokeToken(tokenId, "Blank or missing subject");
                } catch (Exception e) {
                    log.error("Failed to revoke blank-subject token {}: {}", tokenId, e.getMessage());
                }
            }
            throw new TokenValidationFailureException("Invalid subject: blank or missing");
        }
        String subject = subjectClaim.toString();

        try {
            long userId = Long.parseLong(subject);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        String tokenId = claims.get("jti").toString();
                        log.error("At subject validation, User not found for subject (ID) {}. Token ID: {}. Possible Subject Impersonation/Authorization Bypass.", subject, tokenId);
                        try {
                            tokenRevocationService.revokeToken(tokenId, "At subject validation, Token subject (user) was not found (possible Subject Impersonation/Authorization Bypass)");
                        } catch (Exception e) {
                            log.error("Failed to revoke non-existent-user token {}: {}", tokenId, e.getMessage());
                        }
                        return new TokenValidationFailureException("User not found for subject: " + subject);
                    });

            if (!user.isEnabled()) {
                String tokenId = claims.get("jti").toString();
                log.error("At subject validation, User {} is not enabled. Token ID: {}. Possible Subject Impersonation/Authorization Bypass.", subject, tokenId);
                try {
                    tokenRevocationService.revokeToken(tokenId, "At subject validation, User is not enabled (possible Subject Impersonation/Authorization Bypass)");
                } catch (Exception e) {
                    log.error("Failed to revoke disabled-user token {}: {}", tokenId, e.getMessage());
                }
                throw new TokenValidationFailureException("User is not enabled: " + subject);
            }
            log.trace("Subject validation successful for user ID: {}", subject);
        } catch (NumberFormatException e) {
            String tokenId = claims.get("jti").toString();
            log.error("At subject validation, Subject '{}' is not a valid user ID format. Token ID: {}.", subject, tokenId);
            try {
                tokenRevocationService.revokeToken(tokenId, "Invalid subject - not a valid user ID format");
            } catch (Exception ex) {
                log.error("Failed to revoke invalid-subject-format token {}: {}", tokenId, ex.getMessage());
            }
            throw new TokenValidationFailureException("Invalid subject - not a valid user ID format", e);
        }
    }
}

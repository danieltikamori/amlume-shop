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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.InvalidTokenException;
import me.amlu.shop.amlume_shop.exceptions.TokenRevokedException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.security.repository.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenClaimValidator {
    private final HttpServletRequest httpServletRequest;
    private final TokenRevocationService tokenRevocationService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final Cache<String, Boolean> revokedTokensCache;
    private final UserRepository userRepository;

    public void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException {

        // Extract the token ID from the claims
        String tokenId = claims.get("jti").toString();
        // First check the cache
        Boolean isRevoked = revokedTokensCache.getIfPresent(tokenId);
        if (Boolean.TRUE.equals(isRevoked)) {
            throw new TokenRevokedException("Token has been revoked");
        }

        // If not in cache, check the database
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            // Add to cache for future checks
            revokedTokensCache.put(tokenId, true);
            throw new TokenRevokedException("Token has been revoked");
        }
    }

    public void validateRequiredClaims(Map<String, Object> claims) throws InvalidTokenException {
        List<String> requiredClaims = Arrays.asList("exp", "iat", "nbf", "sub", "iss");
        List<String> missingClaims = requiredClaims.stream()
                .filter(claim -> !claims.containsKey(claim))
                .toList(); // Unmodifiable list

        if (!missingClaims.isEmpty()) {
            // Token is missing required claims
            // Revoke the token as it's invalid
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "Missing required claims: " + String.join(", ", missingClaims) + ". Possible token tampering.");
            log.error("Missing required claims: {}", String.join(", ", missingClaims));
            throw new InvalidTokenException("Missing required claims: " + String.join(", ", missingClaims) + ". Possible token tampering.");
        }
    }

    public void validateExpiration(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();

        Object expClaim = claims.get("exp");
        if (expClaim == null) {
            throw new TokenValidationFailureException("Expiration claim is missing");
        }

        Instant exp = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
        if (now.isAfter(exp.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
            // Token has expired, so revoke it
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "Token has expired or possible replay attack detected");
            log.error("At expiration validation, Token with id {} has expired or possible replay attack detected", tokenId);
            throw new TokenValidationFailureException("Token has expired");
        }

    }

    public void validateNotBefore(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();

        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));

        // Consider clock skew
        if (now.isBefore(nbf.minus(Constants.CLOCK_SKEW_TOLERANCE))) {
            // Token is not yet valid, so revoke it as possible replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "At not before validation, Token is not yet valid (possible replay attack)");
            log.error("At not before validation, Token with id {} is not yet valid, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException("Token is not yet valid");
        }
    }

    public void validateIssuanceTime(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();

        Instant iat = Instant.from(DateTimeFormatter.ofPattern(TokenConstants.YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));

        // Check if the token was issued in the future (allowing for clock skew)
        if (iat.isAfter(now.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "At issuance time validation, Token issuance time is not valid (possible replay attack or another security issue)");
            log.error("At issuance time validation, Token with id {} was issued in the future, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException("Token issuance time is not valid");
        }
    }

    public void validateIssuer(Map<String, Object> claims) throws TokenValidationFailureException {
        String issuer = (String) claims.get("iss");
        if (!TokenConstants.DEFAULT_ISSUER.equals(issuer)) {
            // Token issuer is not valid, so revoke it as possible Issuer Spoofing/Impersonation, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "At issuer validation, Token issuer is not valid (possible Issuer Spoofing/Impersonation, replay attack or another security issue)");
            log.error("At issuer validation, Token with id {} issuer is not valid, expecting {} but found {}.Possible Issuer Spoofing/Impersonation, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_ISSUER, issuer);
            throw new TokenValidationFailureException("Invalid token issuer");
        }
    }

    public void validateTokenType(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        String tokenType = (String) claims.get("type");
        if (!expectedType.equals(tokenType)) {
            // Token type is not valid, so revoke it as possible Token Confusion/Type Mismatch attacks, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "At token type validation, Token type is not valid (possible Token Confusion/Type Mismatch attacks, replay attack or another security issue)");
            log.error("At token type validation, Token type is not valid, expected {} but got {}. Possible Token Confusion/Type Mismatch attacks, replay attack or another security issue", expectedType, tokenType);
            throw new TokenValidationFailureException("Invalid token type");
        }
    }

    public void validateAudience(Map<String, Object> claims) throws TokenValidationFailureException {
        String audience = (String) claims.get("aud");
        if (!TokenConstants.DEFAULT_AUDIENCE.equals(audience)) {
            // Token audience is not valid, so revoke it as possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "At audience validation, Token audience is not valid (possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue)");
            log.error("At audience validation, Token with id {} audience is not valid, expecting {} but found {}. Possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue", tokenId, TokenConstants.DEFAULT_AUDIENCE, audience);
            throw new TokenValidationFailureException("Invalid audience");
        }
    }

    public void validateSubject(Map<String, Object> claims) throws TokenValidationFailureException {
        String subject = (String) claims.get("sub");
        if (subject.isBlank()) {
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "isBlank subject");
            throw new TokenValidationFailureException("Invalid subject");
        }

        try {
            long userId = Long.parseLong(subject);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        // Token subject is not valid, so revoke it as possible Subject Impersonation/Authorization Bypass, replay attack or another security issue
                        String tokenId = claims.get("jti").toString();
                        tokenRevocationService.revokeToken(tokenId, "At subject validation, Token subject was not found (possible Subject Impersonation/Authorization Bypass, replay attack or another security issue)");
                        log.error("At subject validation, Token with id {} subject was not found, expecting {} but found {}. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId, userId, subject);

                        return new TokenValidationFailureException("User not found");
                    });
            if (!user.isEnabled()) {
                String tokenId = claims.get("jti").toString();
                tokenRevocationService.revokeToken(tokenId, "At subject validation, User is not enabled (possible Subject Impersonation/Authorization Bypass, replay attack or another security issue)");
                log.error("At subject validation, Token with id {} subject exists, but user is not enabled. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId);

                throw new TokenValidationFailureException("User is not enabled");
            }
        } catch (NumberFormatException e) {
            String tokenId = claims.get("jti").toString();
            tokenRevocationService.revokeToken(tokenId, "Invalid subject - not a valid user ID");
            log.error("At subject validation, Token with id {} subject is not valid user ID, found {}. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId, subject);

            throw new TokenValidationFailureException("Invalid subject - not a valid user ID", e);
        }
    }
}

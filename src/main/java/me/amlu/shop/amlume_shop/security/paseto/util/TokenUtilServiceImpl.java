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

import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.exceptions.UserScopeMissingException;
import me.amlu.shop.amlume_shop.security.paseto.PasetoClaims;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility service for token validation and claim extraction
 */

@Service
public class TokenUtilServiceImpl implements me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService {

    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter
            .ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z)
            .withZone(ZoneOffset.UTC);
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TokenUtilServiceImpl.class);

    private final UserRepository userRepository;

    public TokenUtilServiceImpl(UserRepository userRepository) {

        this.userRepository = userRepository;
    }


    @NullMarked
    @Override
    public User getUserByUserId(String userId) {
        Optional<User> userOptional = userRepository.findById(Long.parseLong(userId)); // import java.util.Optional
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return userOptional.get();
    }

    /**
     * Extracts and validates user ID from claims
     */
    @Override
    public String extractUserId(Map<String, Object> claims) throws TokenValidationFailureException {
        Object sub = claims.get(TokenConstants.CLAIM_SUBJECT);
        if (sub == null) {
            throw new TokenValidationFailureException("Missing subject claim");
        }
        return sub.toString();
    }

    @Override
    public String extractUserScope(Map<String, Object> claims) {
        Object scope = claims.get(TokenConstants.CLAIM_SCOPE);
        if (scope == null) {
            throw new UserScopeMissingException("User scope is missing");
        }
        return scope.toString();
    }

    /**
     * Generates a token ID
     *
     * @return the generated token ID
     */
    @Override
    public String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Extracts token ID from claims
     */
    @Override
    public String extractTokenId(Map<String, Object> claims) throws TokenValidationFailureException {
        Object jti = claims.get(PasetoClaims.PASETO_ID);
        if (jti == null) {
            throw new TokenValidationFailureException("Missing token ID claim");
        }
        return jti.toString();
    }


    /**
     * Helper method to extract Instant claims and format them to PASETO standard format
     */
    @Override
    public Instant extractClaimInstant(Map<String, Object> claims, String claimName)
            throws TokenValidationFailureException {
        Object claim = claims.get(claimName);
        if (claim == null) {
            throw new TokenValidationFailureException(
                    String.format("Missing %s claim", claimName)
            );
        }
        try {
            return Instant.from(INSTANT_FORMATTER.parse(claim.toString()));
        } catch (DateTimeParseException e) {
            throw new TokenValidationFailureException(
                    String.format("Invalid %s claim format", claimName)
            );
        }

    }

    /**
     * Extracts the session ID from the provided claims map.
     *
     * @param claims the map containing token claims
     * @return the session ID as a string if present, otherwise null
     */
    @Override
    public String extractSessionId(Map<String, Object> claims) {
        Object sessionId = claims.get(PasetoClaims.SESSION_ID);
        if (sessionId == null) {
            return null;
        }
        return sessionId.toString();
    }

    /**
     * Takes an object representing an Instant in the PASETO standard format
     * (yyyy-MM-dd'T'HH:mm:ss'Z') and returns the parsed Instant.
     *
     * @param claimInstant the object to parse, must be a string in the PASETO standard format
     * @return the parsed Instant
     */
    @Override
    public Instant getFormattedInstant(Object claimInstant) {
        return Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claimInstant.toString()));
    }
}

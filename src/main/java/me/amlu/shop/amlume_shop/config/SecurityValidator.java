/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.servlet.http.HttpSession;
import me.amlu.shop.amlume_shop.security.paseto.TokenValidationService;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityValidator {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SecurityValidator.class);
    private HttpSession httpSession;
    private TokenValidationService tokenValidationService;

    /**
     * Validates the given authentication object.
     * <p>
     * This method performs two checks:
     * 1. Session validation: checks if the session ID is valid and not expired.
     * 2. Token validation: checks if the Paseto token is valid and not expired.
     * <p>
     * If either of the validation checks fail, the method will log a warning message and return false.
     * If an unexpected exception occurs, the method will log an error message and return false (fail secure).
     * <p>
     *
     * @param authentication the authentication object to validate.
     * @return true if the authentication object is valid, false otherwise.
     */
    public boolean validateAuthentication(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Authentication validation failed: null or not authenticated");
                return false;
            }

            // Session validation
            String sessionId = httpSession.getId();
            if (isSessionInvalid(sessionId)) {
                log.warn("Session validation failed for session ID: {}", sessionId);
                return false;
            }

            // Token validation
            if (authentication.getCredentials() instanceof String token && !isValidToken(token)) {
                log.warn("Token validation failed: invalid or expired Paseto token");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Security validation error", e);
            return false; // Fail secure
        }
    }

    private boolean isValidToken(String token) {
        try {
            return tokenValidationService.isAccessTokenValid(token);
        } catch (Exception e) {
            log.error("Paseto token validation error", e);
            return false;
        }
    }

    private boolean isSessionInvalid(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return true;
        }

        try {
            // Check if session exists and is valid
            HttpSession session = httpSession;
            if (session == null || session.isNew()) {
                return true;
            }

            // Custom validation logic: check if the session has a valid user ID
            Object userId = session.getAttribute("userId");
            if (!(userId instanceof Long)) {
                return true;
            }

            // Custom validation logic: check if the session has not expired
            long sessionCreationTime = session.getCreationTime();
            long sessionMaxInactiveInterval = session.getMaxInactiveInterval();
            return sessionCreationTime + sessionMaxInactiveInterval * 1000L < System.currentTimeMillis();

            // Additional custom validation logic can be added here...
// Session is valid
//            return !httpSession.isNew() &&
//                    httpSession.getLastAccessedTime() + httpSession.getMaxInactiveInterval() * 1000L < System.currentTimeMillis();
        } catch (IllegalStateException e) {
            log.warn("Session validation error", e);
            return true; // Fail secure
        }
    }
}

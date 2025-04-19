/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.service.SecurityAuditService;
import me.amlu.shop.amlume_shop.security.service.SecurityNotificationService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static me.amlu.shop.amlume_shop.commons.Constants.IP_HEADERS;

@Component
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFailureHandler.class);

    private final FailedLoginAttemptService failedLoginService;
    private final UserService userService;
    private final SecurityAuditService securityAuditService;
    private final SecurityNotificationService securityNotificationService;

    public AuthenticationFailureHandler(FailedLoginAttemptService failedLoginService, UserService userService, SecurityAuditService securityAuditService, SecurityNotificationService securityNotificationService) {
        this.failedLoginService = failedLoginService;
        this.userService = userService;
        this.securityAuditService = securityAuditService;
        this.securityNotificationService = securityNotificationService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username"); // Standard parameter name
        String clientIp = getClientIp(request); // Helper method to get IP

        String identifier = username != null ? username : clientIp; // Lock by username if available, else by IP

        try {
            long attemptCount = failedLoginService.recordFailure(identifier);
            securityAuditService.logFailedLogin(username, clientIp, exception.getMessage()); // Audit

            if (failedLoginService.isBlocked(identifier)) {
                log.warn("Blocking identifier '{}' after {} failed attempts.", identifier, attemptCount);
                // Optionally trigger persistent DB lock if locking by username
                if (username != null) {
                    try {
                        User user = userService.findUserByUsername(username);
                        userService.lockUserAccount(user.getUserId()); // Lock in DB
                        securityAuditService.logAccountLocked(String.valueOf(user.getUserId()), username, clientIp);
                        securityNotificationService.sendAccountLockedEmail(user); // Notify
                    } catch (UserNotFoundException e) {
                        log.warn("User '{}' not found during account lock process.", username);
                    } catch (Exception e) {
                        log.error("Error during persistent account lock for user '{}'", username, e);
                    }
                }
                // Set specific error for locked state
                exception = new LockedException("Account is locked due to too many failed attempts.");
            }

        } catch (Exception e) {
            log.error("Error processing failed login attempt for identifier '{}'", identifier, e);
            // Decide how to handle Redis errors here - maybe don't block?
        }

        // Set exception message for the UI
        request.getSession().setAttribute("errorMessage", exception.getMessage());

        // Use Spring's default failure handling (which can redirect) or customize further
        // super.setDefaultFailureUrl("/login?error"); // Ensure this is set
        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIp(HttpServletRequest request) {
        // Reuse logic from DeviceFingerprintServiceImpl or a common utility
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }
}

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
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.service.SecurityAuditService;
import me.amlu.shop.amlume_shop.security.service.SecurityNotificationService;
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
    private final UserService userService; // Still needed to check if user exists for logging/notifications
    private final SecurityAuditService securityAuditService;
    private final SecurityNotificationService securityNotificationService; // TODO: Notification will be implemented at AuthServer

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
            long attemptCount = failedLoginService.recordFailure(identifier); // Record local failure for IP/username rate limiting
            securityAuditService.logFailedLogin(username, clientIp, exception.getMessage()); // Audit

            if (failedLoginService.isBlocked(identifier)) {
                log.warn("Local rate limit: Blocking identifier '{}' after {} failed attempts.", identifier, attemptCount);
                // Set specific error for locked state (this is a local block, not necessarily an authserver account lock yet)
                exception = new LockedException("Access temporarily blocked due to too many failed attempts from this IP or for this username.");

                // The actual account lock is handled by authserver.
                // We don't call userService.lockUserAccount() here anymore.

                // Consider if this notification is still appropriate from amlume-shop,
                // or if authserver should be the sole notifier for account locks.
                // If authserver locks the account, it should ideally send the notification.
                // For now, let's comment out the direct notification from here as it implies amlume-shop did the lock.
                /*
                if (username != null) {
                    try {
                        User user = userService.findUserByEmail(username); // Check if user exists for notification context
                        // securityNotificationService.sendAccountLockedEmail(user); // Potentially redundant if authserver notifies
                        log.info("User {} exists. Authserver will handle account locking if threshold is met there.", username);
                    } catch (UserNotFoundException e) {
                        log.warn("User '{}' not found during local block notification consideration.", username);
                    }
                }
                */
            }

        } catch (Exception e) {
            log.error("Error processing local failed login attempt for identifier '{}'", identifier, e);
            // Decide how to handle Redis errors here - maybe don't block?
        }

        // Set exception message for the UI
        // This message will reflect either the original exception or the LockedException from local rate limiting.
        request.getSession().setAttribute("errorMessage", exception.getMessage());

        // Use Spring's default failure handling (which can redirect) or customize further
        // Ensure a default failure URL is configured if you rely on redirection.
        // Example: super.setDefaultFailureUrl("/login?error=true"); // Set this in SecurityConfig or here
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

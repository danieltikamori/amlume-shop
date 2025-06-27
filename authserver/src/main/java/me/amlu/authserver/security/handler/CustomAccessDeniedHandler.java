/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("Access Denied: {} for request URI: {}", accessDeniedException.getMessage(), request.getRequestURI());

        String targetUrl = "/login"; // Default redirect

        if (accessDeniedException instanceof InvalidCsrfTokenException ||
                accessDeniedException instanceof MissingCsrfTokenException) {
            log.warn("CSRF token issue detected. Redirecting to login with csrf_error.");
            // Specific error for CSRF issues, potentially during logout
            targetUrl = "/login?error=csrf_token_invalid";
        } else if (request.getRequestURI().equals("/logout") && request.getMethod().equalsIgnoreCase("POST")) {
            // If it's a POST to /logout and not a CSRF issue, it might be another session problem
            log.warn("Access denied on POST /logout, possibly due to session timeout before logout. Redirecting with session_expired_logout error.");
            targetUrl = "/login?error=session_expired_logout";
        } else {
            // Generic access denied
            log.warn("Generic access denied. Redirecting to login with access_denied error.");
            targetUrl = "/login?error=access_denied";
        }

        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}

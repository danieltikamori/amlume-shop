/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Enhanced authentication success handler that properly manages redirects
 * and ensures session persistence across requests.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Clear any authentication errors
        clearAuthenticationAttributes(request);

        // Log successful authentication
        log.debug("Authentication successful for user: {}",
                authentication.getName());

        // Ensure session is created and persisted
        HttpSession session = request.getSession(true);
        log.debug("Session ID after authentication: {}", session.getId());

        // Store authentication in session to ensure it persists
        // Create a proper SecurityContext instead of storing the Authentication directly
        org.springframework.security.core.context.SecurityContext securityContext =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        // Determine redirect URL - either from saved request or default to dashboard
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = "/dashboard";

        if (savedRequest != null) {
            targetUrl = savedRequest.getRedirectUrl();
            log.debug("Redirecting to saved request URL: {}", targetUrl);
        } else {
            log.debug("No saved request found, redirecting to default URL: {}", targetUrl);
        }

        // Perform redirect
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}

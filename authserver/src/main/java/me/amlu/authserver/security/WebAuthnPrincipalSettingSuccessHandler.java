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
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.util.Assert;

import java.io.IOException;

// Implement AuthenticationSuccessHandler directly for more control
public class WebAuthnPrincipalSettingSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(WebAuthnPrincipalSettingSuccessHandler.class);
    private final UserDetailsService userDetailsService;
    private final AuthenticationSuccessHandler delegate;
    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private String defaultTargetUrl;

    /**
     * Convenience constructor that takes a UserDetailsService and a default target URL.
     *
     * @param userDetailsService The service to load the full UserDetails.
     * @param defaultTargetUrl   The URL to redirect to on successful authentication.
     */
    public WebAuthnPrincipalSettingSuccessHandler(UserDetailsService userDetailsService, String defaultTargetUrl) {
        Assert.notNull(userDetailsService, "UserDetailsService cannot be null");
        Assert.hasText(defaultTargetUrl, "Default target URL cannot be blank");
        this.userDetailsService = userDetailsService;
        this.defaultTargetUrl = defaultTargetUrl;

        // Create a simple delegate handler that redirects to the default URL
        this.delegate = (request, response, auth) -> {
            redirectStrategy.sendRedirect(request, response, defaultTargetUrl);
        };
    }

    /**
     * Constructor that takes a UserDetailsService and a delegate success handler.
     *
     * @param userDetailsService The service to load the full UserDetails.
     * @param delegate           The delegate success handler to use after setting the principal.
     */
    public WebAuthnPrincipalSettingSuccessHandler(UserDetailsService userDetailsService,
                                                  AuthenticationSuccessHandler delegate) {
        Assert.notNull(userDetailsService, "UserDetailsService cannot be null");
        Assert.notNull(delegate, "Delegate AuthenticationSuccessHandler cannot be null");
        this.userDetailsService = userDetailsService;
        this.delegate = delegate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info(">>> WebAuthnPrincipalSettingSuccessHandler.onAuthenticationSuccess ENTERED. Authentication type: {}",
                authentication.getClass().getName());

        Authentication newAuth = authentication; // Default to original authentication

        if (authentication instanceof WebAuthnAuthentication webAuthnAuth) {
            Object principal = webAuthnAuth.getPrincipal();
            if (principal instanceof org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity userEntity) {
                String username = userEntity.getName(); // This should be the email
                log.debug("WebAuthn success for user entity name (email): {}. Loading full UserDetails.", username);
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails instanceof User appUser) {
                        // Create a new Authentication object with the full UserDetails (your User entity)
                        // Use UsernamePasswordAuthenticationToken as it's standard for UserDetails principals
                        newAuth = new UsernamePasswordAuthenticationToken(
                                appUser, // Your fully loaded User entity as the principal
                                null,    // Credentials are not needed post-authentication
                                appUser.getAuthorities()); // Use authorities from your User entity
                        SecurityContextHolder.getContext().setAuthentication(newAuth);
                        log.info("Replaced WebAuthnAuthentication principal with User entity for: {}", username);
                    } else {
                        log.warn("Loaded UserDetails for {} is not an instance of me.amlu.authserver.user.model.User. Principal not replaced with custom User type.", username);
                        // Keep original WebAuthnAuthentication if UserDetails is not the expected type
                        // newAuth = authentication; // Already defaulted
                    }
                } catch (UsernameNotFoundException e) {
                    log.error("Could not load UserDetails for username {} after successful WebAuthn authentication.", username, e);
                    // Throw an AuthenticationException to trigger the failure handler
                    throw new InternalAuthenticationServiceException("Failed to load user details post-WebAuthn success.", e);
                }
            } else {
                log.warn("WebAuthn principal is not of type PublicKeyCredentialUserEntity. Type: {}",
                        (principal != null ? principal.getClass().getName() : "null"));
            }
        } else {
            log.warn("Authentication object is not an instance of WebAuthnAuthentication. Type: {}",
                    authentication.getClass().getName());
        }

        // Clear any saved request to prevent redirection to unintended URLs
        this.requestCache.removeRequest(request, response);
        log.debug("Cleared saved request from RequestCache.");

        // Clear authentication attributes from the session
        clearAuthenticationAttributes(request);

        // Delegate to the configured success handler
        log.debug("Delegating to configured success handler");
        delegate.onAuthenticationSuccess(request, response, newAuth);
    }

    /**
     * Removes temporary authentication-related data which may have been stored in the
     * session during the authentication process.
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}

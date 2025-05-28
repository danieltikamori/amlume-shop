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
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.util.Assert;

import java.io.IOException;

public class WebAuthnPrincipalSettingSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnPrincipalSettingSuccessHandler.class);
    private final UserDetailsService userDetailsService; // Your AmlumeUserDetailsService
    private final AuthenticationSuccessHandler delegate;

    /**
     * Constructor that takes a UserDetailsService and a delegate AuthenticationSuccessHandler.
     *
     * @param userDetailsService The service to load the full UserDetails.
     * @param delegate           The success handler to delegate to after setting the principal.
     */
    public WebAuthnPrincipalSettingSuccessHandler(UserDetailsService userDetailsService, AuthenticationSuccessHandler delegate) {
        Assert.notNull(userDetailsService, "UserDetailsService cannot be null");
        Assert.notNull(delegate, "Delegate AuthenticationSuccessHandler cannot be null");
        this.userDetailsService = userDetailsService;
        this.delegate = delegate;
    }

    /**
     * Convenience constructor that takes a UserDetailsService and a default target URL.
     * It will create a SavedRequestAwareAuthenticationSuccessHandler as the delegate.
     *
     * @param userDetailsService The service to load the full UserDetails.
     * @param defaultTargetUrl   The URL to redirect to on successful authentication.
     */
    public WebAuthnPrincipalSettingSuccessHandler(UserDetailsService userDetailsService, String defaultTargetUrl) {
        Assert.notNull(userDetailsService, "UserDetailsService cannot be null");
        Assert.hasText(defaultTargetUrl, "Default target URL cannot be blank");
        this.userDetailsService = userDetailsService;
        SavedRequestAwareAuthenticationSuccessHandler concreteDelegate = new SavedRequestAwareAuthenticationSuccessHandler();
        concreteDelegate.setDefaultTargetUrl(defaultTargetUrl);
        this.delegate = concreteDelegate;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info(">>> WebAuthnPrincipalSettingSuccessHandler.onAuthenticationSuccess ENTERED. Authentication type: {}",
                (authentication != null ? authentication.getClass().getName() : "null"));

        Authentication processedAuthentication = authentication; // Start with the original

        if (authentication instanceof WebAuthnAuthentication webAuthnAuth &&
                authentication.getPrincipal() instanceof PublicKeyCredentialUserEntity webAuthnUserEntity) {

            String username = webAuthnUserEntity.getName(); // This is the email
            log.debug("WebAuthn success for user entity name (email): {}. Loading full UserDetails.", username);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username); // This should be your User entity

                if (userDetails instanceof User) { // Check if it's your User entity
                    log.debug("Successfully loaded User entity: {} for WebAuthn principal.", userDetails.getUsername());
                    // Create a new Authentication object with your User entity as the principal
                    UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(
                            userDetails, // Your User entity as principal
                            null,        // Credentials can be null
                            userDetails.getAuthorities() // Original authorities
                    );
                    newAuthentication.setDetails(authentication.getDetails()); // Preserve details

                    SecurityContextHolder.getContext().setAuthentication(newAuthentication);
                    processedAuthentication = newAuthentication; // Use the new authentication for the delegate
                    log.info("Replaced WebAuthnAuthentication principal with User entity for: {}", userDetails.getUsername());
                } else {
                    log.warn("UserDetailsService did not return the expected User entity type for {}. Principal not replaced. Type was: {}",
                            username, userDetails != null ? userDetails.getClass().getName() : "null");
                }
            } catch (Exception e) {
                log.error("Error loading UserDetails or creating new Authentication token for WebAuthn user {}: {}", username, e.getMessage(), e);
                // Decide how to handle: proceed with original auth, or fail?
                // For now, we'll proceed with the original 'authentication' object for the delegate.
            }
        } else {
            log.warn("onAuthenticationSuccess called with unexpected Authentication type: {}. Principal not replaced.",
                    authentication.getClass().getName());
        }

        // Delegate to the original/configured success handler for redirection etc.
        // Pass the potentially modified (or original if modification failed) authentication object.
        delegate.onAuthenticationSuccess(request, response, processedAuthentication);
    }
}

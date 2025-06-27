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

import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Security {@link AuthenticationProvider} for authenticating users
 * with a username and password against the Amlume user store.
 *
 * <p>This provider uses a {@link UserDetailsService} to load user details and a
 * {@link PasswordEncoder} to verify the provided password.
 *
 * <p>It is designed to be used with form-based login mechanisms where
 * {@link UsernamePasswordAuthenticationToken} is presented.
 *
 * <p>Usage:
 * This component is automatically registered by Spring's component scanning.
 * It should be configured in the Spring Security configuration to be part of the
 * authentication chain. For example:
 * <pre>{@code
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *     @Autowired
 *     private AmlumeUsernamePwdAuthenticationProvider amlumeUsernamePwdAuthenticationProvider;
 *
 *     @Autowired
 *     public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
 *         auth.authenticationProvider(amlumeUsernamePwdAuthenticationProvider);
 *     }
 *     // ... other security configurations
 * }
 * }</pre>
 */
@Component("amlumeUsernamePwdAuthenticationProvider") // Ensure this bean name is consistent if used elsewhere
public class AmlumeUsernamePwdAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(AmlumeUsernamePwdAuthenticationProvider.class); // Add logger
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    @Override
    @Timed(value = "authserver.authprovider.formlogin", description = "Time taken for form login authentication")
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String pwd = authentication.getCredentials().toString();
        // This will now correctly call AmlumeUserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername(username); // This loads your custom User

        if (passwordEncoder.matches(pwd, userDetails.getPassword())) {
            // Set the UserDetails object (your User entity) as the principal
            // It's generally recommended to clear credentials after successful authentication for security.
            // The principal should be the UserDetails object itself, or a simplified representation if needed.
            log.debug("User '{}' authenticated successfully.", username);
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } else {
            log.warn("Authentication failed for user '{}': Invalid password.", username);
            throw new BadCredentialsException("Invalid password!");
        }
    }

    /**
     * Constructs an {@code AmlumeUsernamePwdAuthenticationProvider}.
     *
     * @param userDetailsService The {@link UserDetailsService} responsible for loading user-specific data.
     *                           It is qualified with "amlumeUserDetailsService" to ensure the correct
     *                           implementation is injected if multiple are present.
     * @param passwordEncoder    The {@link PasswordEncoder} used for securely encoding and verifying passwords.
     */
    public AmlumeUsernamePwdAuthenticationProvider(
            @Qualifier("amlumeUserDetailsService") UserDetailsService userDetailsService, // Specify by bean name
            PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        log.info("AmlumeUsernamePwdAuthenticationProvider initialized with UserDetailsService: {} and PasswordEncoder: {}",
                userDetailsService.getClass().getSimpleName(), passwordEncoder.getClass().getSimpleName());
    }

    /**
     * Performs authentication with the provided {@link Authentication} object.
     *
     * <p>This method expects an {@link UsernamePasswordAuthenticationToken}. It retrieves
     * the username and password, loads the user details using the configured
     * {@link UserDetailsService}, and then verifies the password using the
     * {@link PasswordEncoder}.
     *
     * <p>If authentication is successful, a new {@link UsernamePasswordAuthenticationToken}
     * is returned with the {@link UserDetails} object as the principal and credentials cleared.
     * If authentication fails (e.g., invalid password), a {@link BadCredentialsException} is thrown.
     *
     * @param authentication The authentication request object, expected to be an instance of
     *                       {@link UsernamePasswordAuthenticationToken}.
     * @return A fully authenticated object including the principal (UserDetails), null credentials,
     * and authorities.
     * @throws AuthenticationException if authentication fails (e.g., user not found, bad credentials).
     */
    @Override
    public boolean supports(Class<?> authentication) {
        // This provider supports only UsernamePasswordAuthenticationToken
        log.debug("Checking support for authentication type: {}", authentication.getName());
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

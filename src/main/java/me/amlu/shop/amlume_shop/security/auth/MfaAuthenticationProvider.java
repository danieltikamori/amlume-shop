/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.auth;

import me.amlu.shop.amlume_shop.exceptions.MfaException; // Keep for MfaService potentially throwing it
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import me.amlu.shop.amlume_shop.user_management.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException; // Import AuthenticationException
import org.springframework.security.core.userdetails.UserDetails; // Keep UserDetails for compatibility
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Keep for UserDetailsService
// Removed PasswordEncoder import as password check is removed
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class MfaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(MfaAuthenticationProvider.class);

    private final UserDetailsService userDetailsService;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    // Removed mfaToken field, it's fetched per user

    public MfaAuthenticationProvider(UserDetailsService userDetailsService,
                                     // Removed PasswordEncoder passwordEncoder,
                                     MfaService mfaService,
                                     MfaTokenRepository mfaTokenRepository) {
        this.userDetailsService = userDetailsService;
        // this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        // this.mfaToken = mfaToken; // Removed assignment
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException { // Declare AuthenticationException
        // This provider should only act if the principal is already partially authenticated (password verified)
        // and needs MFA verification. The incoming 'authentication' object might be a
        // UsernamePasswordAuthenticationToken or a custom token indicating password success.
        // For simplicity, let's assume it receives MfaAuthenticationToken *after* password check.

        if (!(authentication instanceof MfaAuthenticationToken mfaAuthToken)) {
            // If it's not the specific token type this provider handles, return null
            // to let other providers try.
            return null;
        }

        String username = mfaAuthToken.getName();
        String mfaCode = mfaAuthToken.getMfaCode(); // Get MFA code from the token

        log.debug("Attempting MFA authentication for user: {}", username);

        // Load the User object, not just UserDetails, to check MFA status
        // Ensure UserDetailsService is configured to return your User implementation
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.warn("User not found during MFA authentication: {}", username);
            throw new BadCredentialsException("Invalid credentials"); // Or rethrow e
        }

        // --- Password check removed ---
        // The password should have been verified by a previous AuthenticationProvider (e.g., DaoAuthenticationProvider)
        // before this MFA provider is invoked.

        // Ensure the loaded principal is actually our User class
        if (!(userDetails instanceof User user)) {
            log.error("UserDetailsService did not return an instance of the expected User class for username: {}", username);
            throw new BadCredentialsException("Internal authentication configuration error");
        }

        // Check if MFA is actually enabled for the user. If not, something is wrong in the flow.
        if (!mfaService.isMfaEnabled(user)) {
            log.warn("MFA provider invoked for user {} but MFA is not enabled.", username);
            // This might indicate a logic error elsewhere. Depending on policy:
            // - Could throw an exception.
            // - Could treat as success (if password was already verified).
            // Let's throw for stricter security.
            throw new BadCredentialsException("MFA not enabled for user, but MFA provider was invoked.");
        }

        // MFA is enabled, now validate the code
        if (mfaCode == null || mfaCode.isBlank()) {
            log.warn("MFA code missing for user: {}", username);
            throw new BadCredentialsException("MFA code is required"); // Use BadCredentialsException
        }

        // Get the MFA secret from the MfaToken associated with the User
        String encryptedSecret = mfaTokenRepository.findByUser(user)
                .map(MfaToken::getSecret)
                .orElseThrow(() -> {
                    log.error("MFA secret not found for user {} even though MFA is enabled.", username);
                    return new BadCredentialsException("MFA configuration error for user");
                });

        try {
            if (!mfaService.verifyCode(encryptedSecret, mfaCode)) { // Pass encryptedSecret and code
                log.warn("Invalid MFA code provided for user: {}", username);
                // TODO: Consider calling handleFailedLogin here if MFA failures should lock account
                throw new BadCredentialsException("Invalid MFA code");
            }
        } catch (TooManyAttemptsException e) {
            log.warn("Too many MFA attempts for user: {}", username);
            throw new BadCredentialsException("Too many MFA attempts", e); // Wrap exception
        } catch (ExecutionException e) { // Catch potential underlying exceptions from verifyCode
            log.error("Error during MFA code verification for user: {}", username, e);
            throw new BadCredentialsException("Error verifying MFA code", e);
        } catch (MfaException e) { // Catch specific MFA exceptions from the service
            log.warn("MFA service error during verification for user {}: {}", username, e.getMessage());
            throw new BadCredentialsException("MFA verification failed: " + e.getMessage(), e);
        }

        log.info("MFA authentication successful for user: {}", username);

        // If MFA code is valid, return a fully authenticated token
        // Use the User object as the principal
        MfaAuthenticationToken fullyAuthenticatedToken = new MfaAuthenticationToken(
                user, // The Principal is the User object
                null, // Credentials typically cleared after authentication
                mfaCode, // Keep MFA code if needed later, or set to null
                user.getAuthorities() // Use authorities from the loaded User
        );
        // No need to call setAuthenticated(true), the constructor with authorities does this.

        return fullyAuthenticatedToken;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        // This provider supports the MfaAuthenticationToken
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
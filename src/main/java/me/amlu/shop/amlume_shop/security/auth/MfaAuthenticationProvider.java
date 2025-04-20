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

import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.exceptions.MfaRequiredException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import me.amlu.shop.amlume_shop.user_management.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class MfaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(MfaAuthenticationProvider.class);

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final FailedLoginAttemptService failedLoginAttemptService;
    // Removed mfaToken field, it's fetched per user

    public MfaAuthenticationProvider(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService,
                                     // Removed PasswordEncoder passwordEncoder,
                                     MfaService mfaService,
                                     MfaTokenRepository mfaTokenRepository, FailedLoginAttemptService failedLoginAttemptService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        // this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        // this.mfaToken = mfaToken; // Removed assignment
        this.failedLoginAttemptService = failedLoginAttemptService;
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
        String presentedPassword = (String) mfaAuthToken.getCredentials(); // Get password from the token
        String mfaCode = mfaAuthToken.getMfaCode(); // Get MFA code from the token

        log.debug("Attempting password and MFA authentication for user: {}", username);

        // Load the User object, not just UserDetails, to check MFA status
        // Ensure UserDetailsService is configured to return your User implementation
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.warn("User not found during MFA authentication: {}", username);
            failedLoginAttemptService.recordFailure(username);
            throw new BadCredentialsException("Invalid credentials"); // Or rethrow e
        }

        // --- Verify the password ---
        // Although verified by the previous AuthProvider, we will check again here.
        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            log.warn("Invalid password provided for user: {}", username);
            failedLoginAttemptService.recordFailure(username);
            throw new BadCredentialsException("Invalid credentials");
        }
        // Deprecated
        // --- Password check removed ---
        // The password should have been verified by a previous AuthenticationProvider (e.g., DaoAuthenticationProvider)
        // before this MFA provider is invoked.

        // Ensure the loaded principal is actually our User class
        if (!(userDetails instanceof User user)) {
            log.error("UserDetailsService did not return an instance of the expected User class for username: {}", username);
            throw new BadCredentialsException("Internal authentication configuration error");
        }

        // --- MFA Check ---
        // Check if MFA is actually enabled for the user. If not, something is wrong in the flow.
        if (mfaService.isMfaEnabled(user)) {
            log.debug("MFA is enabled for user: {}. Verifying code.", username);
            if (mfaCode == null || mfaCode.isBlank()) {
                log.warn("MFA code missing for user: {}", username);
                // Throw MfaRequiredException, which CustomAuthenticationFilter can catch
                throw new MfaRequiredException("MFA code is required");
            }

            // Get the MFA secret
            String encryptedSecret = mfaTokenRepository.findByUser(user)
                    .map(MfaToken::getSecret)
                    .orElseThrow(() -> {
                        log.error("MFA secret not found for user {} even though MFA is enabled.", username);
                        return new BadCredentialsException("MFA configuration error for user");
                    });

            try {
                if (!mfaService.verifyCode(encryptedSecret, mfaCode)) {
                    log.warn("Invalid MFA code provided for user: {}", username);
                    // Consider calling failedLoginService.recordFailure here
                    throw new BadCredentialsException("Invalid MFA code");
                }
                log.debug("MFA code verified successfully for user: {}", username);
            } catch (TooManyAttemptsException e) {
                log.warn("Too many MFA attempts for user: {}", username);
                throw new BadCredentialsException("Too many MFA attempts", e); // Wrap exception
            } catch (ExecutionException e) {
                log.error("Error during MFA code verification for user: {}", username, e);
                throw new BadCredentialsException("Error verifying MFA code", e);
            } catch (MfaException e) {
                log.warn("MFA service error during verification for user {}: {}", username, e.getMessage());
                throw new BadCredentialsException("MFA verification failed: " + e.getMessage(), e);
            }
            // MFA enabled and code is valid
        } else {
            log.debug("MFA is not enabled for user: {}. Proceeding without MFA check.", username);
            // MFA isn't enabled, password was correct, proceed
        }

        // --- Authentication Successful ---
        log.info("Authentication successful for user: {}", username);

        // Return fully authenticated token
        MfaAuthenticationToken fullyAuthenticatedToken = new MfaAuthenticationToken(
                user, // The Principal is the User object
                null, // Clear credentials
                null, // Clear MFA code after use
                user.getAuthorities() // Use authorities from the loaded User
        );
        // Set details if needed by downstream filters (like DeviceFingerprintVerificationFilter)
        // If the token generation needs claims, they might be added later.
        // For now, let's assume the User object is sufficient principal.

        return fullyAuthenticatedToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // This provider supports the MfaAuthenticationToken
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
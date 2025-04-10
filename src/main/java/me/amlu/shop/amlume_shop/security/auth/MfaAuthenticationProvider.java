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

import me.amlu.shop.amlume_shop.exceptions.MfaRequiredException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class MfaAuthenticationProvider implements AuthenticationProvider {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final MfaToken mfaToken;

    public MfaAuthenticationProvider(UserDetailsService userDetailsService,
                                     PasswordEncoder passwordEncoder,
                                     MfaService mfaService, MfaTokenRepository mfaTokenRepository, MfaToken mfaToken) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.mfaToken = mfaToken;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof MfaAuthenticationToken mfaToken)) {
            return null;
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        String mfaCode = mfaToken.getMfaCode();

        // Load the User object, not just UserDetails
        User user = (User) userDetailsService.loadUserByUsername(username); // Cast to your User class

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // Check if MFA is enabled for the user, using the User object
        if (mfaService.isMfaEnabled(user)) {  // Pass the User object
            if (mfaCode == null || mfaCode.isEmpty()) {
                throw new MfaRequiredException("MFA code is required");
            }

            // Get the encrypted secret from the MfaToken associated with the User
            String encryptedSecret = mfaTokenRepository.findByUser(user)
                    .map(MfaToken::getSecret)
                    .orElseThrow(() -> new BadCredentialsException("MFA secret not found for user")); // Handle this case appropriately

            try {
                if (!mfaService.verifyCode(encryptedSecret, mfaCode)) { // Pass encryptedSecret and code
                    throw new BadCredentialsException("Invalid MFA code");
                }
            } catch (TooManyAttemptsException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return new MfaAuthenticationToken(
                user,
                password,
                mfaCode,
                user.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

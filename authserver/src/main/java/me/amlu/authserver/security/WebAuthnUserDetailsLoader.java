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

import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Custom AuthenticationUserDetailsService for WebAuthn.
 * This service is used by WebAuthnAuthenticationProvider to load the application's
 * UserDetails (the User entity) based on the PublicKeyCredentialUserEntity
 * provided after successful WebAuthn assertion verification.
 */
@Component
public class WebAuthnUserDetailsLoader implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnUserDetailsLoader.class);

    private final UserRepository userRepository;

    public WebAuthnUserDetailsLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the application's UserDetails (User entity) based on the principal
     * from the provided PreAuthenticatedAuthenticationToken.
     * The principal is expected to be a PublicKeyCredentialUserEntity.
     *
     * @param token The authentication token containing the PublicKeyCredentialUserEntity principal.
     * @return The loaded UserDetails (User entity).
     * @throws UsernameNotFoundException if the user cannot be found or is disabled/locked.
     */
    @Override
    @Transactional(readOnly = true) // User lookup is a read operation
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        // The principal in the token provided to this service by WebAuthnAuthenticationProvider
        // will be the PublicKeyCredentialUserEntity from the WebAuthnAuthentication object.
        Object principal = token.getPrincipal();

        if (!(principal instanceof PublicKeyCredentialUserEntity webAuthnUserEntity)) {
            log.error("Principal is not a PublicKeyCredentialUserEntity: {}", principal != null ? principal.getClass().getName() : "null");
            // This should not happen if the provider is configured correctly, but handle defensively.
            throw new UsernameNotFoundException("Principal is not a WebAuthn user entity.");
        }

        // The ID of the PublicKeyCredentialUserEntity is the user handle (User.externalId)
        String userHandle = webAuthnUserEntity.getId().toBase64UrlString();
        // The name of the PublicKeyCredentialUserEntity is the username (email)
        String usernameFromWebAuthn = webAuthnUserEntity.getName();

        log.debug("Loading UserDetails for WebAuthn user handle: {}, username (email): {}", userHandle, usernameFromWebAuthn);

        // Find your application's User entity using the externalId (user handle)
        User user = userRepository.findByExternalId(userHandle)
                .orElseThrow(() -> {
                    log.error("Local user not found for WebAuthn user handle: {}", userHandle);
                    // This indicates a provisioning issue - the user authenticated with a passkey
                    // but no corresponding local user record was found.
                    // Throwing UsernameNotFoundException is standard for UserDetailsService.
                    return new UsernameNotFoundException("User not found for WebAuthn user handle: " + userHandle);
                });

        // Perform standard UserDetails checks on the loaded user
        if (!user.isEnabled()) {
            log.warn("User account disabled for WebAuthn user handle {}: userId={}", userHandle, user.getId());
            throw new UsernameNotFoundException("User account is disabled");
        }
        if (!user.isAccountNonLocked()) {
            log.warn("User account locked for WebAuthn user handle {}: userId={}", userHandle, user.getId());
            throw new UsernameNotFoundException("User account is locked");
        }
        if (!user.isAccountNonExpired()) {
            log.warn("User account expired for WebAuthn user handle {}: userId={}", userHandle, user.getId());
            throw new UsernameNotFoundException("User account has expired");
        }
        if (!user.isCredentialsNonExpired()) {
            log.warn("User credentials expired for WebAuthn user handle {}: userId={}", userHandle, user.getId());
            throw new UsernameNotFoundException("User credentials have expired");
        }


        log.debug("Successfully loaded local User entity for WebAuthn user handle {}: userId={}", userHandle, user.getId());

        // Return your application's User entity (which implements UserDetails).
        // This User entity will then be set as the principal in the final Authentication object
        // by the WebAuthnAuthenticationProvider.
        return user;
    }
}

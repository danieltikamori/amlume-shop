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

import me.amlu.authserver.config.WebAuthNProperties;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of {@link WebAuthnRelyingPartyOperations} that overrides
 * the user entity lookup logic to correctly handle different principal types,
 * especially {@link OAuth2User} principals after social login.
 */
@Component // Make it a Spring bean
public class CustomWebAuthnRelyingPartyOperations extends Webauthn4JRelyingPartyOperations {

    private static final Logger log = LoggerFactory.getLogger(CustomWebAuthnRelyingPartyOperations.class);

    private final UserRepository userRepository; // To find your local User entity

    public CustomWebAuthnRelyingPartyOperations(
            UserRepository userRepository,
            DbPublicKeyCredentialUserEntityRepository dbPublicKeyCredentialUserEntityRepository,
            UserCredentialRepository userCredentialRepository, // Spring injects your DbUserCredentialRepository
            WebAuthNProperties webAuthNProperties
    ) {
        super(
                dbPublicKeyCredentialUserEntityRepository,
                userCredentialRepository,
                PublicKeyCredentialRpEntity.builder()
                        .id(webAuthNProperties.getRpId())
                        .name(webAuthNProperties.getRpName())
                        .build(),
                webAuthNProperties.getAllowedOrigins()
        );
        this.userRepository = userRepository;
        log.info("CustomWebAuthnRelyingPartyOperations initialized with RP ID: {}", webAuthNProperties.getRpId());
    }

    /**
     * Overrides the default method to find the {@link PublicKeyCredentialUserEntity}
     * corresponding to the authenticated principal.
     * This method is crucial for correctly linking WebAuthn operations to your local
     * {@link User} entities, especially after OAuth2 login.
     *
     * @param authentication The current {@link Authentication} object from the SecurityContext.
     * @return The {@link PublicKeyCredentialUserEntity} for the authenticated user, or null if not found or cannot be mapped.
     */
    @Transactional(readOnly = true) // User lookup is read-only
    public PublicKeyCredentialUserEntity findUserEntity(Authentication authentication) {
        log.debug("CustomWebAuthnRelyingPartyOperations.findUserEntity called for authentication: {}", authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("findUserEntity: Authentication is null or not authenticated.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        User appUser = null;
        if (principal instanceof User castedUser) {
            appUser = castedUser;
            log.debug("findUserEntity: Principal is already User entity (ID: {}).", appUser.getId());
        } else if (principal instanceof OAuth2User oauth2User) {
            // Principal is an OAuth2User (e.g., after social login)
            // Resolve your User entity from OAuth2User, similar to your controller helper
            String email = oauth2User.getAttribute("email"); // Get the email attribute set by CustomOAuth2UserService
            if (email != null) {
                log.debug("findUserEntity: Looking up User by email from OAuth2User: {}", email);
                appUser = userRepository.findByEmail_Value(email).orElse(null);
                if (appUser == null) {
                    log.warn("findUserEntity: Local User not found for OAuth2 principal email: {}. This indicates a provisioning issue.", email);
                }
            } else {
                // This case should ideally not happen if CustomOAuth2UserService correctly sets the 'email' attribute
                log.warn("findUserEntity: Could not determine email from OAuth2User principal. Attributes: {}", oauth2User.getAttributes());
            }
        } else if (principal instanceof UserDetails userDetails) {
            // Handle other UserDetails types if necessary (e.g., if UserDetailsService returns a different implementation)
            log.debug("findUserEntity: Principal is UserDetails, looking up by username (email): {}", userDetails.getUsername());
            appUser = userRepository.findByEmail_Value(userDetails.getUsername()).orElse(null);
            if (appUser == null) {
                log.warn("findUserEntity: Local User not found for UserDetails username: {}.", userDetails.getUsername());
            }
        } else {
            log.warn("findUserEntity: Unsupported principal type in SecurityContext: {}", principal.getClass().getName());
            return null;
        }

        if (appUser == null) {
            log.warn("findUserEntity: Could not resolve local User entity from authenticated principal.");
            return null;
        }

        // Map your User entity to PublicKeyCredentialUserEntity using the helper from DbPublicKeyCredentialUserEntityRepository
        // Ensure mapUserToPublicKeyCredentialUserEntity is accessible (e.g., public static)
        PublicKeyCredentialUserEntity mappedUserEntity = DbPublicKeyCredentialUserEntityRepository.mapUserToPublicKeyCredentialUserEntity(appUser); // Assuming static helper

        if (mappedUserEntity == null) {
            log.error("findUserEntity: Failed to map local User (ID: {}) to PublicKeyCredentialUserEntity. Ensure user has externalId and email.", appUser.getId());
        } else {
            log.debug("findUserEntity: Successfully mapped User (ID: {}) to WebAuthn UserEntity (ID: {}, Name: {}, DisplayName: {}).",
                    appUser.getId(),
                    mappedUserEntity.getId().toBase64UrlString(),
                    mappedUserEntity.getName(),
                    mappedUserEntity.getDisplayName());
        }

        return mappedUserEntity;
    }

    @Override
    @Transactional // Add transactional if superclass method is, or if your logic needs it
    public PublicKeyCredentialCreationOptions createPublicKeyCredentialCreationOptions(PublicKeyCredentialCreationOptionsRequest request) {
        log.debug("CustomWebAuthnRelyingPartyOperations.createPublicKeyCredentialCreationOptions called.");

        final Authentication currentAuthentication = request.getAuthentication();

        if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
            log.error("Cannot create PublicKeyCredentialCreationOptions: No authenticated principal in request.");
            throw new IllegalStateException("Authenticated principal must be provided for registration options.");
        }

        // Our goal is to ensure the superclass uses the correct user identity.
        // The most crucial part is that `currentAuthentication.getName()` should yield
        // the correct username (i.e., email for your system) that your
        // `DbPublicKeyCredentialUserEntityRepository.findByUsername()` can use to find the *existing* user.

        // We still create a new request object to pass to super, but it will only implement getAuthentication().
        PublicKeyCredentialCreationOptionsRequest requestToPassToSuper = new PublicKeyCredentialCreationOptionsRequest() {
            @Override
            public Authentication getAuthentication() {
                return currentAuthentication;
            }
        };

        log.info("Calling super.createPublicKeyCredentialCreationOptions. Superclass will derive user info primarily from Authentication (name: {}). This name MUST be the user's email for correct lookup.",
                currentAuthentication.getName());

        // The `Webauthn4JRelyingPartyOperations` superclass will internally use its
        // configured `PublicKeyCredentialUserEntityRepository` (which is your `DbPublicKeyCredentialUserEntityRepository`)
        // and call its `findByUsername(authentication.getName())` or `findById(someGeneratedHandle)`.
        // It will also use its `UserCredentialRepository` (your `DbUserCredentialRepository`)
        // to find existing credentials to exclude.
        return super.createPublicKeyCredentialCreationOptions(requestToPassToSuper);
    }
}

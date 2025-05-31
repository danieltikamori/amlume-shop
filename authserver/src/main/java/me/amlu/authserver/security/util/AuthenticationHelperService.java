/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.util;

import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthenticationHelperService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationHelperService.class);
    private final UserRepository userRepository;

    public AuthenticationHelperService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the application-specific {@link User} entity from the given
     * Spring Security {@link Authentication} object.
     * <p>
     * This method handles different types of principals that might be present
     * in the Authentication object, such as a pre-loaded {@link User} entity,
     * an {@link OAuth2User}, or a generic {@link UserDetails}.
     *
     * @param authentication The Spring Security Authentication object.
     * @return The resolved {@link User} entity, or {@code null} if the user
     * cannot be resolved or if the authentication is invalid.
     */
    @Transactional(readOnly = true) // Good practice if this method involves DB lookups
    public User getAppUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("getAppUserFromAuthentication: Authentication is null or not authenticated.");
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User appUser) {
            log.debug("Principal is already an instance of User: {}", appUser.getUsername());
            return appUser;
        } else if (principal instanceof OAuth2User oauth2User) {
            log.debug("Principal is OAuth2User. Attributes: {}", oauth2User.getAttributes());
            // Assuming your CustomOAuth2UserService ensures 'email' attribute is reliably populated
            // or you have a consistent way to get the unique identifier.
            String email = oauth2User.getAttribute("email");

            // Fallback logic if email attribute is not directly available or needs provider-specific handling
            if (email == null && authentication instanceof OAuth2LoginAuthenticationToken oauthToken) {
                String registrationId = oauthToken.getClientRegistration().getRegistrationId();
                String login = oauth2User.getAttribute("login"); // e.g., GitHub username

                if (login != null && "github".equalsIgnoreCase(registrationId)) {
                    // This block is a fallback. Ideally, CustomOAuth2UserService
                    // should have already fetched the primary email and put it into
                    // the oauth2User attributes under the "email" key.
                    // If CustomOAuth2UserService guarantees the "email" attribute is populated,
                    // this specific GitHub fallback might be redundant or simplified.
                    log.warn("Email attribute missing for GitHub user (registrationId: {}), login: {}. " +
                            "Relying on CustomOAuth2UserService to have populated the 'email' attribute correctly. " +
                            "If not, this lookup might fail or use a placeholder.", registrationId, login);
                    // If CustomOAuth2UserService ensures 'email' is set, 'email' variable would already have it.
                    // If you still need to fetch it here (not recommended as it duplicates logic):
                    // email = fetchGitHubPrimaryEmail(oauthToken.getAccessToken().getTokenValue());
                }
            }

            if (email != null) {
                log.debug("Looking up User by email from OAuth2User: {}", email);
                return userRepository.findByEmail_Value(email)
                        .orElseGet(() -> {
                            log.warn("No local User found for OAuth2 principal with email: {}. " +
                                    "This could be a provisioning delay or an issue if the user should exist.", email);
                            return null;
                        });
            } else {
                log.warn("Could not determine email from OAuth2User principal to look up local User. Attributes: {}", oauth2User.getAttributes());
                return null;
            }
        } else if (principal instanceof UserDetails userDetails) {
            log.debug("Principal is UserDetails: {}", userDetails.getUsername());
            // This typically means form login or WebAuthn login where UserDetailsService returns your User entity
            return userRepository.findByEmail_Value(userDetails.getUsername()).orElse(null);
        }
        log.warn("Unsupported principal type in getAppUserFromAuthentication: {}", principal.getClass().getName());
        return null;
    }
}

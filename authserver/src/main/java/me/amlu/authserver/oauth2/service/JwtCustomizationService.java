/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.service;

import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for customizing JWT claims.
 * This service is called during the JWT encoding process to add custom claims
 * such as roles, user ID, and other profile information.
 */
@Service
public class JwtCustomizationService {

    private static final Logger log = LoggerFactory.getLogger(JwtCustomizationService.class);
    private final UserRepository userRepository;

    public JwtCustomizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Customizes the claims for a given JWT encoding context.
     * This method is timed using Micrometer to monitor its performance.
     *
     * @param context The {@link JwtEncodingContext} containing the claims to be customized.
     */
    @Timed(value = "authserver.jwt.customization", description = "Time taken to customize JWT claims")
    public void customizeToken(JwtEncodingContext context) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            log.debug("Customizing JWT access token for principal: {}", context.getPrincipal().getName());
            context.getClaims().claims((claims) -> {
                if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    // For client_credentials, scopes often map to roles/permissions.
                    // These are typically simple strings.
                    Set<String> clientScopesAsAuthorities = context.getRegisteredClient().getScopes();
                    claims.put("authorities", clientScopesAsAuthorities); // Use "authorities" for consistency
                    log.debug("Added client scopes as 'authorities' for client_credentials grant: {}", clientScopesAsAuthorities);
                } else { // For user-based grants (e.g., authorization_code, password, refresh_token)
                    Object principalObject = context.getPrincipal().getPrincipal();
                    User appUser = null;

                    if (principalObject instanceof User) {
                        appUser = (User) principalObject;
                        log.debug("Principal is an instance of User: {}", appUser.getEmail().getValue());
                    } else if (context.getPrincipal().getName() != null) {
                        // Fallback for other UserDetails implementations or if principal is just username string
                        // This might occur during refresh token grant if the full User object isn't in the Authentication
                        Optional<User> userOptional = userRepository.findByEmail_Value(context.getPrincipal().getName());
                        if (userOptional.isPresent()) {
                            appUser = userOptional.get();
                            log.debug("User resolved from repository for principal name: {}", appUser.getEmail().getValue());
                        } else {
                            log.warn("User not found in repository for principal name: {}. Cannot add detailed user claims.", context.getPrincipal().getName());
                        }
                    } else {
                        log.warn("Could not determine User object from principal to add user-specific claims or detailed authorities.");
                    }

                    if (appUser != null) {
                        putUserSpecificClaims(claims, appUser);

                        // Add roles (prefixed with ROLE_) and permissions to the "authorities" claim
                        Set<String> authoritiesAndPermissions = new HashSet<>();
                        // appUser.getAuthorities() from UserDetails returns Collection<? extends GrantedAuthority>
                        // These should be instances of your custom me.amlu.authserver.oauth2.model.Authority
                        // if your UserDetailsService and User entity are set up correctly.
                        for (GrantedAuthority ga : appUser.getAuthorities()) {
                            if (ga instanceof me.amlu.authserver.oauth2.model.Authority customAuthority) {
                                authoritiesAndPermissions.add(customAuthority.getAuthority()); // Adds "ROLE_XYZ"
                                customAuthority.getPermissions().forEach(permission ->
                                        authoritiesAndPermissions.add(permission.getName()));
                            } else {
                                // Fallback if it's a SimpleGrantedAuthority or other type
                                authoritiesAndPermissions.add(ga.getAuthority());
                                log.warn("GrantedAuthority for user {} was not of custom Authority type: {}. Only role name added.", appUser.getUsername(), ga.getClass());
                            }
                        }
                        claims.put("authorities", Collections.unmodifiableSet(authoritiesAndPermissions));
                        log.debug("Added 'authorities' (roles and permissions) for user {}: {}", appUser.getEmail().getValue(), authoritiesAndPermissions);

                    } else {
                        // If appUser is still null, add only basic authorities from the Authentication principal
                        // These would typically just be roles like "ROLE_USER"
                        Set<String> basicAuthorities = context.getPrincipal().getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet());
                        claims.put("authorities", basicAuthorities);
                        log.debug("Added basic 'authorities' from Authentication principal: {}", basicAuthorities);
                    }
                }
            });
        }
    }

    /**
     * Helper method to populate JWT claims with user-specific profile information.
     *
     * @param claims  The map of claims to populate.
     * @param appUser The {@link User} object containing the profile information.
     */
    private void putUserSpecificClaims(Map<String, Object> claims, User appUser) {
        claims.put("user_id_numeric", appUser.getId());
        claims.put("user_id_external", appUser.getExternalId()); // Add externalId
        claims.put("given_name", appUser.getGivenName());
        // Ensure surname is not null if it's encrypted and might return null from getter if not decrypted
        if (appUser.getSurname() != null) {
            claims.put("family_name", appUser.getSurname());
        }
        claims.put("full_name", appUser.getDisplayableFullName());
        claims.put("nickname", appUser.getNickname());
        claims.put("email", appUser.getEmail().getValue());
        claims.put("email_verified", appUser.getAccountStatus().isEmailVerified()); // Assuming AccountStatus has this
    }
}

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    private final WebClient amlumeShopWebClient; // Configured to talk to amlume-shop

    public JwtCustomizationService(UserRepository userRepository, WebClient amlumeShopWebClient) {
        this.userRepository = userRepository;
        this.amlumeShopWebClient = amlumeShopWebClient;
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

            // This part handles user-based grants
            if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                String username = context.getPrincipal().getName();

                // 1. Add standard user claims from authserver's own User entity
                userRepository.findByEmail_ValueAndDeletedAtIsNull(username).ifPresent(user -> {
                    context.getClaims().claims(claims -> {
                        putUserSpecificClaims(claims, user); // Your existing method
                    });
                });

                // 2. Fetch and add business roles from amlume-shop
                // In a real scenario, this would be an API call or a read from a shared DB.
                // Let's simulate fetching them.
                Set<String> businessRoles = fetchBusinessRolesForUser(username); // Simulated method call
            }

            context.getClaims().claims((claims) -> {
                if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                    // For client_credentials, scopes often map to roles/permissions.
                    // These are typically simple strings.
                    Set<String> clientScopesAsAuthorities = context.getRegisteredClient().getScopes();
                    claims.put("authorities", clientScopesAsAuthorities); // Use "roles" for consistency
                    log.debug("Added client scopes as 'roles' for client_credentials grant: {}", clientScopesAsAuthorities);
                } else { // For user-based grants (e.g., authorization_code, password, refresh_token)
                    Object principalObject = context.getPrincipal().getPrincipal();
                    User appUser = null;

                    if (principalObject instanceof User) {
                        appUser = (User) principalObject;
                        log.debug("Principal is an instance of User: {}", appUser.getEmail().getValue());
                    } else if (context.getPrincipal().getName() != null) {
                        // Fallback for other UserDetails implementations or if principal is just username string
                        // This might occur during refresh token grant if the full User object isn't in the Authentication
                        Optional<User> userOptional = userRepository.findByEmail_ValueAndDeletedAtIsNull(context.getPrincipal().getName());
                        if (userOptional.isPresent()) {
                            appUser = userOptional.get();
                            log.debug("User resolved from repository for principal name: {}", appUser.getEmail().getValue());
                        } else {
                            log.warn("User not found in repository for principal name: {}. Cannot add detailed user claims.", context.getPrincipal().getName());
                        }
                    } else {
                        log.warn("Could not determine User object from principal to add user-specific claims or detailed roles.");
                    }

                    if (appUser != null) {
                        putUserSpecificClaims(claims, appUser);

                        // Add roles (prefixed with ROLE_) and permissions to the "roles" claim
                        Set<String> authoritiesAndPermissions = new HashSet<>();
                        // appUser.getRoles() from UserDetails returns Collection<? extends GrantedAuthority>
                        // These should be instances of your custom me.amlu.authserver.oauth2.model.Authority
                        // if your UserDetailsService and User entity are set up correctly.
                        for (GrantedAuthority ga : appUser.getAuthorities()) {
                            if (ga instanceof me.amlu.authserver.role.model.Role customAuthority) {
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
                        log.debug("Added 'roles' (roles and permissions) for user {}: {}", appUser.getEmail().getValue(), authoritiesAndPermissions);

                        // Add the business roles to a custom claim
                        // This assumes businessRoles was fetched earlier in the method.
                        Set<String> businessRoles = fetchBusinessRolesForUser(appUser.getUsername()); // Re-fetch or pass from outer scope
                        claims.put("business_roles", businessRoles);
                        log.debug("Added custom 'business_roles' claim for user {}: {}", appUser.getUsername(), businessRoles);

                    } else {
                        // If appUser is still null, add only basic roles from the Authentication principal
                        // These would typically just be roles like "ROLE_USER"
                        Set<String> basicAuthorities = context.getPrincipal().getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet());
                        claims.put("authorities", basicAuthorities);
                        log.debug("Added basic 'roles' from Authentication principal: {}", basicAuthorities);
                    }
                }
            });
        }
    }

    // API Call - requires WebClient and a DTO from amlume-shop
    private Set<String> fetchBusinessRolesForUser(String username) {
        // In a real implementation, 'username' here would be the email or authServerSubjectId
        // that amlume-shop uses to identify its users.
        // You'd need to map this 'username' to the correct identifier for amlume-shop.
        // For example, if amlume-shop identifies users by authServerSubjectId:
        User authserverUser = userRepository.findByEmail_ValueAndDeletedAtIsNull(username).orElse(null);
        if (authserverUser == null || authserverUser.getExternalId() == null) {
            log.warn("Authserver user not found or has no externalId for business role fetch: {}", username);
            return Collections.emptySet();
        }
        String amlumeShopUserId = authserverUser.getExternalId(); // This is the authServerSubjectId in amlume-shop

        // amlume-shop needs an endpoint like /api/internal/users/{authServerSubjectId}/roles
        // that is secured for M2M communication (e.g., using @PreAuthorize("hasAuthority('SCOPE_internal_roles_read')")
        // and validating the client credentials token from authserver).
        try {
            return amlumeShopWebClient.get()
                    .uri("/api/internal/users/{id}/roles", amlumeShopUserId)
//                      .attributes(clientRegistrationId("amlume-shop-client-credentials")) // If using client credentials
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Set<String>>() {
                    })
                    .block(); // Use block() for synchronous behavior in this context, or refactor to reactive
        } catch (Exception e) {
            log.error("Failed to fetch business roles from amlume-shop for user {}: {}", username, e.getMessage());
            return Collections.emptySet(); // Fail-safe
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
            claims.put("family_name", appUser.getSurname()); // family_name is according to OIDC standard
        }
        claims.put("full_name", appUser.getDisplayableFullName());
        claims.put("nickname", appUser.getNickname());
        claims.put("email", appUser.getEmail().getValue());
        claims.put("email_verified", appUser.getAccountStatus().isEmailVerified()); // Assuming AccountStatus has this

        // --- Add device fingerprint claim ---
        if (appUser.isDeviceFingerprintingEnabled() && appUser.getDeviceFingerprintingInfo().getCurrentFingerprint() != null) {
            claims.put("device_fingerprint", appUser.getDeviceFingerprintingInfo().getCurrentFingerprint());
            log.debug("Added 'device_fingerprint' claim for user {}: {}", appUser.getEmail().getValue(), appUser.getDeviceFingerprintingInfo().getCurrentFingerprint());
        } else {
            log.debug("Device fingerprinting not enabled or current fingerprint not found for user {}. 'device_fingerprint' claim not added.", appUser.getEmail().getValue());
        }
    }
}

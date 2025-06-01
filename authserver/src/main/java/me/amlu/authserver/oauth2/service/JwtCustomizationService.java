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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
                    Set<String> roles = context.getRegisteredClient().getScopes();
                    claims.put("roles", roles);
                    log.debug("Added roles from client scopes for client_credentials grant: {}", roles);
                } else { // For user-based grants (e.g., authorization_code, password, refresh_token)
                    Set<String> roles = AuthorityUtils.authorityListToSet(context.getPrincipal().getAuthorities())
                            .stream()
                            .map(r -> r.replaceFirst("^ROLE_", ""))
                            .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                    claims.put("roles", roles);
                    log.debug("Added roles from principal authorities: {}", roles);

                    Object principal = context.getPrincipal().getPrincipal();
                    if (principal instanceof User appUser) {
                        putUserSpecificClaims(claims, appUser);
                        log.debug("Added user-specific claims for User principal: {}", appUser.getEmail().getValue());
                    } else if (context.getPrincipal().getName() != null) {
                        // Fallback for other UserDetails implementations or if principal is just username string
                        userRepository.findByEmail_Value(context.getPrincipal().getName()).ifPresent(user -> {
                            putUserSpecificClaims(claims, user);
                            log.debug("Added user-specific claims for user found by email: {}", user.getEmail().getValue());
                        });
                    } else {
                        log.warn("Could not determine User object from principal to add user-specific claims.");
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
        claims.put("given_name", appUser.getFirstName());
        claims.put("family_name", appUser.getLastName());
        claims.put("full_name", appUser.getDisplayableFullName());
        claims.put("nickname", appUser.getNickname());
        claims.put("email", appUser.getEmail().getValue()); // Ensure email is consistently added
    }
}

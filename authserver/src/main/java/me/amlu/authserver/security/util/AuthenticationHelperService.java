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
import me.amlu.authserver.user.service.UserLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;

@Component
public class AuthenticationHelperService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationHelperService.class);
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;

    public AuthenticationHelperService(UserRepository userRepository, UserLookupService userLookupService) {
        this.userRepository = userRepository;
        this.userLookupService = userLookupService;
    }


    /**
     * Defines the role hierarchy for the application.
     * Higher roles inherit permissions from lower roles.
     */
    public enum Role {
        USER(0),
        MODERATOR(1),
        ADMIN(2),
        SUPER_ADMIN(3),
        ROOT(4);

        private final int level;

        Role(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean hasAtLeastSameAuthorityAs(Role role) {
            return this.level >= role.level;
        }
    }

    /**
     * Checks if the authenticated user has at least the specified role level.
     * This implements a hierarchical permission model where higher roles
     * automatically have the permissions of lower roles.
     *
     * @param authentication The Spring Security Authentication object
     * @param minimumRole    The minimum role required
     * @return true if the user has at least the specified role level
     */
    public boolean hasMinimumRole(Authentication authentication, Role minimumRole) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check for each role at or above the minimum level
        for (Role role : Role.values()) {
            if (role.getLevel() >= minimumRole.getLevel() &&
                    authentication.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the authenticated user can manage the specified user.
     * Implements the principle that users can only manage users with lower roles.
     *
     * @param authentication The Spring Security Authentication object
     * @param targetUserId   The ID of the user to be managed
     * @return true if the authenticated user can manage the target user
     */
    public boolean canManageUser(Authentication authentication, Long targetUserId) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication);
        if (currentUser == null) {
            return false;
        }

        // Users can always manage themselves
        if (currentUser.getId().equals(targetUserId)) {
            return true;
        }

        // Find the target user
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) {
            return false;
        }

        // Get the highest role for each user
        Role currentUserHighestRole = getHighestRole(currentUser);
        Role targetUserHighestRole = getHighestRole(targetUser);

        // Can only manage users with lower role levels
        return currentUserHighestRole.getLevel() > targetUserHighestRole.getLevel();
    }

    /**
     * Gets the highest role assigned to a user.
     *
     * @param user The user to check
     * @return The highest role the user has, defaults to USER if no roles found
     */
    private Role getHighestRole(User user) {
        return user.getAuthorities().stream()
                .map(authority -> {
                    String roleName = authority.getAuthority().replace("ROLE_", "");
                    try {
                        return Role.valueOf(roleName);
                    } catch (IllegalArgumentException e) {
                        return Role.USER; // Default if role doesn't match enum
                    }
                })
                .max(Comparator.comparingInt(Role::getLevel))
                .orElse(Role.USER); // Default if no roles found

    }

    /**
     * Checks if the authenticated user has any of the specified roles.
     *
     * @param authentication The Spring Security Authentication object
     * @param roles          One or more role names to check
     * @return true if the user has at least one of the specified roles
     */
    public boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (String role : roles) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the authenticated user has all of the specified roles.
     *
     * @param authentication The Spring Security Authentication object
     * @param roles          One or more role names to check
     * @return true if the user has all of the specified roles
     */
    public boolean hasAllRoles(Authentication authentication, String... roles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return Arrays.stream(roles)
                .allMatch(role -> authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)));
    }

    /**
     * Checks if the authenticated user is the owner of the specified resource.
     *
     * @param authentication  The Spring Security Authentication object
     * @param resourceOwnerId The ID of the resource owner
     * @return true if the authenticated user is the owner of the resource
     */
    public boolean isResourceOwner(Authentication authentication, Long resourceOwnerId) {
        User user = userLookupService.getAppUserFromAuthentication(authentication);
        return user != null && user.getId().equals(resourceOwnerId);
    }

    /**
     * Checks if the authenticated user has admin privileges or is the owner of the resource.
     * This is useful for operations where either admins or resource owners should have access.
     *
     * @param authentication  The Spring Security Authentication object
     * @param resourceOwnerId The ID of the resource owner
     * @return true if the user is an admin or the resource owner
     */
    public boolean isAdminOrResourceOwner(Authentication authentication, Long resourceOwnerId) {
        return hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN", "ROOT") ||
                isResourceOwner(authentication, resourceOwnerId);
    }

}

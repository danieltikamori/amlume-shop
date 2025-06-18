/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import me.amlu.authserver.common.Roles;
import me.amlu.authserver.user.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service for handling role hierarchy and role-based authorization checks.
 */
@Service
public class RoleHierarchyService {

    // Role hierarchy levels (higher number = higher privilege)
    private static final Map<String, Integer> ROLE_LEVELS = new HashMap<>();

    static {
        ROLE_LEVELS.put(Roles.USER, 1);
        ROLE_LEVELS.put(Roles.ADMIN, 2);
        ROLE_LEVELS.put(Roles.SUPER_ADMIN, 3);
        ROLE_LEVELS.put(Roles.ROOT, 4);
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param user The user to check
     * @param role The role to check for
     * @return true if the user has the role
     */
    public boolean hasRole(User user, String role) {
        if (user == null) {
            return false;
        }

        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }

    /**
     * Checks if a user has any of the specified roles.
     *
     * @param user  The user to check
     * @param roles The roles to check for
     * @return true if the user has any of the roles
     */
    public boolean hasAnyRole(User user, String... roles) {
        if (user == null || roles == null || roles.length == 0) {
            return false;
        }

        Collection<String> userRoles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an authentication has a specific role.
     *
     * @param authentication The authentication to check
     * @param role           The role to check for
     * @return true if the authentication has the role
     */
    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }

    /**
     * Checks if an authentication has any of the specified roles.
     *
     * @param authentication The authentication to check
     * @param roles          The roles to check for
     * @return true if the authentication has any of the roles
     */
    public boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || !authentication.isAuthenticated() || roles == null || roles.length == 0) {
            return false;
        }

        Collection<String> authRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        for (String role : roles) {
            if (authRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a user has at least the specified role level.
     * This implements a hierarchical permission model where higher roles
     * automatically have the permissions of lower roles.
     *
     * @param user        The user to check
     * @param minimumRole The minimum role required
     * @return true if the user has at least the specified role level
     */
    public boolean hasMinimumRole(User user, String minimumRole) {
        if (user == null || minimumRole == null) {
            return false;
        }

        Integer minimumLevel = ROLE_LEVELS.get(minimumRole);
        if (minimumLevel == null) {
            return false;
        }

        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(ROLE_LEVELS::get)
                .filter(Objects::nonNull)
                .anyMatch(level -> level >= minimumLevel);
    }

    /**
     * Checks if an authentication has at least the specified role level.
     *
     * @param authentication The authentication to check
     * @param minimumRole    The minimum role required
     * @return true if the authentication has at least the specified role level
     */
    public boolean hasMinimumRole(Authentication authentication, String minimumRole) {
        if (authentication == null || !authentication.isAuthenticated() || minimumRole == null) {
            return false;
        }

        Integer minimumLevel = ROLE_LEVELS.get(minimumRole);
        if (minimumLevel == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(ROLE_LEVELS::get)
                .filter(Objects::nonNull)
                .anyMatch(level -> level >= minimumLevel);
    }

    /**
     * Checks if a user can manage another user based on role hierarchy.
     * A user can manage another user if they have a higher role level.
     *
     * @param manager The user who wants to manage
     * @param target  The user to be managed
     * @return true if the manager can manage the target
     */
    public boolean canManage(User manager, User target) {
        if (manager == null || target == null) {
            return false;
        }

        // Users can always manage themselves
        if (manager.getId().equals(target.getId())) {
            return true;
        }

        int managerHighestLevel = getHighestRoleLevel(manager);
        int targetHighestLevel = getHighestRoleLevel(target);

        return managerHighestLevel > targetHighestLevel;
    }

    /**
     * Gets the highest role level for a user.
     *
     * @param user The user
     * @return The highest role level
     */
    public int getHighestRoleLevel(User user) {
        if (user == null) {
            return 0;
        }

        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(ROLE_LEVELS::get)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}

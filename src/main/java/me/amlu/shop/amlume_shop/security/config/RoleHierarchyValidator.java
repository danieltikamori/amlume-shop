/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import me.amlu.shop.amlume_shop.user_management.AppRole;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** Role hierarchy:
 * ADMIN
 *   └── CATEGORY_MANAGER
 *         └── PRODUCT_EDITOR
 *               └── USER
 *   └── SELLER
 *         └── PRODUCT_EDITOR
 *               └── USER
 */

@Component
public class RoleHierarchyValidator {

    private static final Logger log = LoggerFactory.getLogger(RoleHierarchyValidator.class);

//    private RoleHierarchy roleHierarchy;

    private static final Map<AppRole, Set<AppRole>> ROLE_HIERARCHY = new EnumMap<>(AppRole.class);
    private static final Map<AppRole, Set<AppRole>> INCOMPATIBLE_ROLES = new EnumMap<>(AppRole.class);

    // Initialize role hierarchies and incompatibilities
    static {
        // Define role hierarchies (higher roles include lower roles)
        ROLE_HIERARCHY.put(AppRole.ROLE_ROOT, Set.of(
                AppRole.ROLE_SUPER_ADMIN,
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_CATEGORY_MANAGER,
                AppRole.ROLE_SELLER,
                AppRole.ROLE_PRODUCT_EDITOR,
                AppRole.ROLE_USER,
                AppRole.ROLE_CUSTOMER
        ));

        ROLE_HIERARCHY.put(AppRole.ROLE_SUPER_ADMIN, Set.of(
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_CATEGORY_MANAGER,
                AppRole.ROLE_SELLER,
                AppRole.ROLE_PRODUCT_EDITOR,
                AppRole.ROLE_USER,
                AppRole.ROLE_CUSTOMER
        ));

        ROLE_HIERARCHY.put(AppRole.ROLE_ADMIN, Set.of(
                AppRole.ROLE_CATEGORY_MANAGER,
                AppRole.ROLE_SELLER,
                AppRole.ROLE_PRODUCT_EDITOR,
                AppRole.ROLE_USER
        ));

        ROLE_HIERARCHY.put(AppRole.ROLE_CATEGORY_MANAGER, Set.of(
                AppRole.ROLE_PRODUCT_EDITOR,
                AppRole.ROLE_USER
        ));

        ROLE_HIERARCHY.put(AppRole.ROLE_SELLER, Set.of(
                AppRole.ROLE_PRODUCT_EDITOR,
                AppRole.ROLE_USER
        ));

        ROLE_HIERARCHY.put(AppRole.ROLE_PRODUCT_EDITOR, Set.of(
                AppRole.ROLE_USER
        ));

        // Define incompatible role combinations

        INCOMPATIBLE_ROLES.put(AppRole.ROLE_SELLER, Set.of(
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_SUPER_ADMIN,
                AppRole.ROLE_ROOT,
                AppRole.ROLE_CATEGORY_MANAGER,
                AppRole.ROLE_CUSTOMER // A seller cannot be an admin, customer, etc.
        ));

        INCOMPATIBLE_ROLES.put(AppRole.ROLE_CATEGORY_MANAGER, Set.of(
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_SUPER_ADMIN,
                AppRole.ROLE_ROOT,
                AppRole.ROLE_SELLER,
                AppRole.ROLE_CUSTOMER // A category manager cannot be an admin, seller, customer, etc.
        ));

        INCOMPATIBLE_ROLES.put(AppRole.ROLE_PRODUCT_EDITOR, Set.of(
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_SUPER_ADMIN,
                AppRole.ROLE_ROOT,
                AppRole.ROLE_CUSTOMER // A product editor cannot be an admin, customer, etc.
        ));

        INCOMPATIBLE_ROLES.put(AppRole.ROLE_CUSTOMER, Set.of(
                AppRole.ROLE_ADMIN,
                AppRole.ROLE_SUPER_ADMIN,
                AppRole.ROLE_ROOT,
                AppRole.ROLE_CATEGORY_MANAGER,
                AppRole.ROLE_SELLER,
                AppRole.ROLE_PRODUCT_EDITOR // A customer cannot be an admin, category manager, seller, or product editor
        ));

        // Define more role hierarchies and incompatibilities

    }


    public boolean isRoleAssignmentInvalid(Set<UserRole> newRoles, Authentication authentication) {
        try {
            // Get user's existing authorities
            Set<AppRole> existingRoles = authentication.getAuthorities().stream()
                    .map(authority -> AppRole.valueOf(authority.getAuthority().replace("ROLE_", "")))
                    .collect(Collectors.toSet());

            // Convert new roles to AppRole
            Set<AppRole> proposedRoles = newRoles.stream()
                    .map(UserRole::getRoleName)
                    .collect(Collectors.toSet());

            log.debug("Validating roles - Existing: {}, Proposed: {}", existingRoles, proposedRoles);

            // Validate role combinations
            // Check for invalid combinations first
            if (hasInvalidRoleCombination(proposedRoles)) {
                log.warn("Invalid role combination detected: {}", proposedRoles);
                return true; // Is invalid
            }

            // Check for privilege escalation
            if (hasPrivilegeEscalation(existingRoles, proposedRoles)) {
                log.warn("Privilege escalation detected - Existing: {}, Proposed: {}",
                        existingRoles, proposedRoles);
                return true; // Is invalid
            }
            return false; // Is valid

        } catch (Exception e) {
            log.error("Error validating role assignment", e);
            return true; // Is invalid
        }
    }

    private boolean hasInvalidRoleCombination(Set<AppRole> roles) {
        // Check each role against incompatible roles
        for (AppRole role : roles) {
            Set<AppRole> incompatible = INCOMPATIBLE_ROLES.getOrDefault(role, Collections.emptySet());

            // If any of the incompatible roles are present, return true
            if (roles.stream().anyMatch(incompatible::contains)) {
                log.warn("Found incompatible role combination: {} conflicts with {}",
                        role, incompatible);
                return true;
            }
        }
        return false;
    }

    private boolean hasPrivilegeEscalation(Set<AppRole> existing, Set<AppRole> proposed) {
        // If user has ADMIN role, they can assign any role
        if (existing.contains(AppRole.ROLE_ADMIN) || existing.contains(AppRole.ROLE_SUPER_ADMIN) || existing.contains(AppRole.ROLE_ROOT)) {
            return false;
        }

        // Check if any proposed role is higher than existing roles
        for (AppRole proposedRole : proposed) {
            if (!canAssignRole(existing, proposedRole)) {
                log.warn("Privilege escalation detected: {} cannot be assigned with existing roles {}",
                        proposedRole, existing);
                return true;
            }
        }
        return false;
    }

    private boolean canAssignRole(Set<AppRole> existingRoles, AppRole proposedRole) {
        // Check if any existing role has hierarchy over the proposed role
        return existingRoles.stream()
                .anyMatch(existingRole -> {
                    Set<AppRole> allowedRoles = ROLE_HIERARCHY.getOrDefault(existingRole, Collections.emptySet());
                    return existingRole.equals(proposedRole) || allowedRoles.contains(proposedRole);
                });
    }

    // Utility method to check if a role has authority over another role
    public boolean hasAuthorityOver(AppRole higherRole, AppRole lowerRole) {
        if (higherRole == lowerRole) {
            return true;
        }
        Set<AppRole> subordinatAppRoles = ROLE_HIERARCHY.getOrDefault(higherRole, Collections.emptySet());
        return subordinatAppRoles.contains(lowerRole);
    }

    // Method to get all subordinate roles for a given role
    public Set<AppRole> getSubordinatAppRoles(AppRole role) {
        return ROLE_HIERARCHY.getOrDefault(role, Collections.emptySet());
    }
}


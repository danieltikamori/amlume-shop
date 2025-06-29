/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleHierarchyValidator {

    private static final Logger log = LoggerFactory.getLogger(RoleHierarchyValidator.class);

    // Use String for role names consistently
    private static final Map<String, Set<String>> ROLE_HIERARCHY = new HashMap<>();
    private static final Map<String, Set<String>> INCOMPATIBLE_ROLES = new HashMap<>();

    // Initialize role hierarchies and incompatibilities
    static {
        // Define role hierarchies (higher roles include lower roles)
        // Use String literals for role names as they come from GrantedAuthority.getAuthority()
        ROLE_HIERARCHY.put("ROLE_ROOT", Set.of(
                "ROLE_SUPER_ADMIN",
                "ROLE_ADMIN",
                "ROLE_USER" // Assuming a basic hierarchy for authserver
        ));

        ROLE_HIERARCHY.put("ROLE_SUPER_ADMIN", Set.of(
                "ROLE_ADMIN",
                "ROLE_USER"
        ));

        ROLE_HIERARCHY.put("ROLE_ADMIN", Set.of(
                "ROLE_USER"
        ));

        // Define incompatible role combinations
        // For authserver, typically roles are additive and hierarchy handles permissions.
        // If you have mutually exclusive roles, define them here.
        // Example: if 'ROLE_USER' and 'ROLE_ADMIN' are mutually exclusive:
        // INCOMPATIBLE_ROLES.put("ROLE_USER", Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"));
        // INCOMPATIBLE_ROLES.put("ROLE_ADMIN", Set.of("ROLE_USER")); // And so on for other admin roles
    }

    /**
     * Validates a proposed set of roles for a user against defined role hierarchies and incompatibilities.
     * This method is crucial for preventing privilege escalation and invalid role combinations.
     *
     * @param newRoles       The set of proposed role names (e.g., "ROLE_ADMIN", "ROLE_USER").
     * @param authentication The current authentication object, representing the user performing the assignment.
     * @return true if the role assignment is invalid, false otherwise.
     */
    public boolean isRoleAssignmentInvalid(Set<String> newRoles, Authentication authentication) {
        try {
            // Get user's existing roles (roles) as Strings
            Set<String> existingRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority) // getAuthority() returns String
                    .collect(Collectors.toSet());

            log.debug("Validating roles - Existing: {}, Proposed: {}", existingRoles, newRoles);

            // 1. Check for invalid combinations
            if (hasInvalidRoleCombination(newRoles)) {
                log.warn("Invalid role combination detected: {}", newRoles);
                return true; // Is invalid
            }

            // 2. Check for privilege escalation
            if (hasPrivilegeEscalation(existingRoles, newRoles)) {
                log.warn("Privilege escalation detected - Existing: {}, Proposed: {}",
                        existingRoles, newRoles);
                return true; // Is invalid
            }
            return false; // Is valid

        } catch (Exception e) {
            log.error("Error validating role assignment", e);
            return true; // Is invalid
        }
    }

    /**
     * Checks if the proposed set of roles contains any incompatible combinations.
     *
     * @param roles The set of role names to check.
     * @return true if an invalid combination is found, false otherwise.
     */
    private boolean hasInvalidRoleCombination(Set<String> roles) {
        // Check each role against incompatible roles
        for (String role : roles) {
            Set<String> incompatible = INCOMPATIBLE_ROLES.getOrDefault(role, Collections.emptySet());
            // If any of the incompatible roles are present in the 'roles' set, return true
            if (roles.stream().anyMatch(incompatible::contains)) {
                log.warn("Found incompatible role combination: {} conflicts with {}",
                        role, incompatible);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the proposed role assignment constitutes a privilege escalation.
     * An escalation occurs if the user attempts to assign a role that is "higher"
     * than what their existing roles permit.
     *
     * @param existingRoles The set of roles the user currently possesses.
     * @param proposedRoles The set of roles the user is attempting to assign.
     * @return true if privilege escalation is detected, false otherwise.
     */
    private boolean hasPrivilegeEscalation(Set<String> existingRoles, Set<String> proposedRoles) {
        // If the user has a ROOT or SUPER_ADMIN role, they can assign any role (no escalation check needed)
        if (existingRoles.contains("ROLE_ROOT") || existingRoles.contains("ROLE_SUPER_ADMIN")) {
            return false;
        }

        // Check if any proposed role is higher than what existing roles permit
        for (String proposedRole : proposedRoles) {
            // If the proposed role is not assignable by any of the existing roles, it's an escalation
            if (!canAssignRole(existingRoles, proposedRole)) {
                log.warn("Privilege escalation detected: User with roles {} cannot assign role {}",
                        existingRoles, proposedRole);
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a user with a given set of existing roles can assign a specific proposed role.
     * This checks if any of the existing roles has authority over the proposed role in the hierarchy.
     *
     * @param existingRoles The set of roles the user currently possesses.
     * @param proposedRole  The role name (String) the user is attempting to assign.
     * @return true if the user can assign the proposed role, false otherwise.
     */
    private boolean canAssignRole(Set<String> existingRoles, String proposedRole) {
        // Check if any existing role has hierarchy over the proposed role
        return existingRoles.stream()
                .anyMatch(existingRole -> {
                    // An existing role can assign itself or any of its subordinate roles
                    return existingRole.equals(proposedRole) || hasAuthorityOver(existingRole, proposedRole);
                });
    }

    /**
     * Utility method to check if a higher role has authority over a lower role in the hierarchy.
     *
     * @param higherRole The role name (String) of the potentially higher role.
     * @param lowerRole  The role name (String) of the potentially lower role.
     * @return true if higherRole has authority over lowerRole (including if they are the same role), false otherwise.
     */
    public boolean hasAuthorityOver(String higherRole, String lowerRole) {
        if (higherRole.equals(lowerRole)) {
            return true;
        }
        Set<String> subordinateRoles = ROLE_HIERARCHY.getOrDefault(higherRole, Collections.emptySet());
        return subordinateRoles.contains(lowerRole);
    }

    /**
     * Method to get all subordinate roles for a given role.
     *
     * @param role The role name (String) for which to get subordinates.
     * @return A set of subordinate role names (String).
     */
    public Set<String> getSubordinateRoles(String role) {
        return ROLE_HIERARCHY.getOrDefault(role, Collections.emptySet());
    }
}

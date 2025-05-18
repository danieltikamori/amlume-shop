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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import me.amlu.shop.amlume_shop.security.service.RoleService;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See the SensitiveData annotation for more details.
 * This class is used to validate the SensitiveData annotation.
 * Usage:
 *
 * @SensitiveData(rolesAllowed = {"ADMIN", "USER"})
 * private String sensitiveField;
 */

@Component
public class DynamicSensitiveDataValidator implements ConstraintValidator<SensitiveData, Object> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DynamicSensitiveDataValidator.class);

    private final RoleService roleService;

    private Set<String> staticRolesAllowed;

    public DynamicSensitiveDataValidator(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void initialize(SensitiveData constraintAnnotation) {
        try {
            this.staticRolesAllowed = Set.of(constraintAnnotation.rolesAllowed())
                    .stream()
                    .map(role -> "ROLE_" + role.toUpperCase()) // Ensure ROLE_ prefix for comparison
                    .collect(Collectors.toSet());
            log.debug("Initialized static roles: {}", staticRolesAllowed);
        } catch (Exception e) {
            log.error("Error initializing validator", e);
            this.staticRolesAllowed = Collections.emptySet();
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // Get authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Access denied: User not authenticated");
                return false;
            }

            // Get user roles (Set<String>)
            Set<String> userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // Log current user and their roles for debugging
            log.debug("Validating access for user: {} with roles: {}",
                    authentication.getName(), userRoles);

            // Check static roles
            boolean hasStaticRole = !Collections.disjoint(staticRolesAllowed, userRoles);
            log.debug("Static role check result: {}", hasStaticRole);

            // Get dynamic roles (Set<UserRole>)
            Set<UserRole> dynamicUserRolesObject = getDynamicRoles(value);

            // Convert dynamic roles (Set<UserRole>) to a Set<String> of authority names
            Set<String> dynamicRolesAsString = dynamicUserRolesObject.stream()
                    .map(userRole -> userRole.getRoleName().name()) // Get AppRole enum, then its string name
                    .collect(Collectors.toSet());
            log.debug("Dynamic roles (as strings) for resource: {}", dynamicRolesAsString);

            // Check dynamic roles (comparing Set<String> with Set<String>)
            boolean hasDynamicRole = !Collections.disjoint(dynamicRolesAsString, userRoles);
            log.debug("Dynamic role check result: {}", hasDynamicRole);

            boolean hasAccess = hasStaticRole || hasDynamicRole;

            // Log the result
            if (!hasAccess) {
                log.warn("Access denied for user: {}. Required static roles: {}, dynamic roles (strings): {}, user roles: {}",
                        authentication.getName(),
                        staticRolesAllowed,
                        dynamicRolesAsString, // Log the string representation
                        userRoles);
            } else {
                log.debug("Access granted for user: {}", authentication.getName());
            }

            return hasAccess;

        } catch (Exception e) {
            log.error("Error during validation", e);
            return false; // Fail secure
        }
    }

    private Set<UserRole> getDynamicRoles(Object value) {
        try {
            if (value == null) {
                return Collections.emptySet();
            }
            // Ensure roleService is not null
            if (roleService == null) {
                log.error("RoleService is not injected. Cannot retrieve dynamic roles.");
                return Collections.emptySet();
            }

            Set<UserRole> dynamicRoles = roleService.getDynamicRolesForResource(value);
            log.debug("Retrieved dynamic roles for resource type {}: {}",
                    value.getClass().getSimpleName(),
                    dynamicRoles.stream()
                            .map(userRole -> userRole.getRoleName().name()) // Log the AppRole enum name
                            .collect(Collectors.toSet()));
            return dynamicRoles;
        } catch (Exception e) {
            log.error("Error retrieving dynamic roles for resource: {}",
                    value != null ? value.getClass().getSimpleName() : "null", e);
            return Collections.emptySet();
        }
    }
}

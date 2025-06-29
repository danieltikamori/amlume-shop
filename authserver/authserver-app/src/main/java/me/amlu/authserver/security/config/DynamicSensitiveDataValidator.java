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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.security.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that the current user has the required static or dynamic roles to access a field.
 * This validator is associated with the {@link SensitiveData} annotation.
 * It checks the user's authorities against a static list from the annotation and a dynamic
 * list determined by the {@link RoleService} based on the annotated field's value.
 *
 * @SensitiveData(rolesAllowed = {"ADMIN", "USER"})
 * private String sensitiveField;
 * @see SensitiveData
 */
@Component
public class DynamicSensitiveDataValidator implements ConstraintValidator<SensitiveData, Object> {

    private static final Logger log = LoggerFactory.getLogger(DynamicSensitiveDataValidator.class);

    private final RoleService roleService;

    private Set<String> staticRolesAllowed;

    public DynamicSensitiveDataValidator(RoleService roleService) {
        Assert.notNull(roleService, "RoleService cannot be null.");
        this.roleService = roleService;
    }

    @Override
    public void initialize(SensitiveData constraintAnnotation) {
        // Pre-process the static roles from the annotation to include the "ROLE_" prefix
        // for consistent comparison with Spring Security's GrantedAuthority strings.
        this.staticRolesAllowed = Arrays.stream(constraintAnnotation.rolesAllowed())
                .map(role -> "ROLE_" + role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        log.debug("Initialized validator with static roles allowed: {}", staticRolesAllowed);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Access denied: User not authenticated");
            return false;
        }

        // Get the user's roles as a Set of strings (e.g., "ROLE_ADMIN", "ROLE_USER")
        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 1. Check against the static roles defined in the annotation.
        if (!Collections.disjoint(staticRolesAllowed, userAuthorities)) {
            log.debug("Access granted for user '{}' based on static role match. User roles: {}, Allowed static roles: {}",
                    authentication.getName(), userAuthorities, staticRolesAllowed);
            return true; // User has a required static role.
        }

        // 2. If no static role match, check for dynamic roles based on the resource.
        Set<String> dynamicAuthorities = getDynamicAuthorities(value);
        if (!Collections.disjoint(dynamicAuthorities, userAuthorities)) {
            log.debug("Access granted for user '{}' based on dynamic role match. User roles: {}, Required dynamic roles: {}",
                    authentication.getName(), userAuthorities, dynamicAuthorities);
            return true; // User has a required dynamic role.
        }

        // 3. If neither check passes, deny access.
        log.warn("Access DENIED for user '{}'. User roles {} do not match required static roles {} or dynamic roles {}.",
                authentication.getName(), userAuthorities, staticRolesAllowed, dynamicAuthorities);
        return false;
    }

    /**
     * Retrieves dynamic roles for the given resource and converts them into a Set of authority strings.
     *
     * @param resource The resource object being validated.
     * @return A Set of authority strings (e.g., "ROLE_OWNER") for the resource, or an empty set if none.
     */
    private Set<String> getDynamicAuthorities(Object resource) {
        if (resource == null) {
            return Collections.emptySet();
        }
        try {
            // Get the domain-specific Role objects
            Set<Role> dynamicRoles = roleService.getDynamicRolesForResource(resource);

            // Convert them to standard authority strings (e.g., "ROLE_...")
            return dynamicRoles.stream()
                    .map(role -> "ROLE_" + role.getName().toUpperCase(Locale.ROOT)) // Assuming role.getName() returns "OWNER", etc.
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Error retrieving dynamic roles for resource of type {}: {}",
                    resource.getClass().getSimpleName(), e.getMessage(), e);
            return Collections.emptySet(); // Fail secure
        }
    }
}

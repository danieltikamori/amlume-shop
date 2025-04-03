/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import me.amlu.shop.amlume_shop.security.service.RoleService;
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
 * @SensitiveData(rolesAllowed = {"ADMIN", "USER"})
 * private String sensitiveField;
 */

@Slf4j
@Component
public class DynamicSensitiveDataValidator implements ConstraintValidator<SensitiveData, Object> {

//    @Autowired
    private RoleService roleService; // Make sure this service is properly implemented

    private Set<String> staticRolesAllowed;

    @Override
    public void initialize(SensitiveData constraintAnnotation) {
        try {
            this.staticRolesAllowed = Set.of(constraintAnnotation.rolesAllowed())
                    .stream()
                    .map(role -> "ROLE_" + role.toUpperCase())
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

            // Get user roles
            Set<String> userRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // Log current user and their roles for debugging
            log.debug("Validating access for user: {} with roles: {}",
                    authentication.getName(), userRoles);

            // Check static roles
            boolean hasStaticRole = !Collections.disjoint(staticRolesAllowed, userRoles);
            log.debug("Static role check result: {}", hasStaticRole);

            // Get and check dynamic roles
            Set<UserRole> dynamicRoles = getDynamicRoles(value);
            boolean hasDynamicRole = !Collections.disjoint(dynamicRoles, userRoles);
            log.debug("Dynamic role check result: {}", hasDynamicRole);

            boolean hasAccess = hasStaticRole || hasDynamicRole;

            // Log the result
            if (!hasAccess) {
                log.warn("Access denied for user: {}. Required static roles: {}, dynamic roles: {}, user roles: {}",
                        authentication.getName(),
                        staticRolesAllowed,
                        dynamicRoles,
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

            Set<UserRole> dynamicRoles = roleService.getDynamicRolesForResource(value);
            log.debug("Retrieved dynamic roles for resource type {}: {}",
                    value.getClass().getSimpleName(),
                    dynamicRoles.stream()
                            .map(UserRole::getRoleName)
                            .collect(Collectors.toSet()));
            return dynamicRoles;
        } catch (Exception e) {
            log.error("Error retrieving dynamic roles for resource: {}",
                    value != null ? value.getClass().getSimpleName() : "null", e);
            return Collections.emptySet();
        }
    }
}

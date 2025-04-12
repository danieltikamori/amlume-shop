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
import me.amlu.shop.amlume_shop.exceptions.SecurityConfigurationException;
import me.amlu.shop.amlume_shop.security.service.RoleAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * To further enhance this implementation, consider the following options:
 * Adding role-based access control at the database level
 * Implementing role-based access control at the application level
 * Implementing role-based access control at the API level
 * <p>
 * Caching role assignments
 * <p>
 * Implementing role hierarchy
 * <p>
 * Adding rate limiting
 * <p>
 * Implementing role expiration
 * <p>
 * Adding support for temporary role elevation
 * <p>
 * Implementing role-based access control at the API gateway level
 */

@Component
public class SecureAppRoleValidator implements ConstraintValidator<SensitiveData, Object> {

    private Set<String> rolesAllowed;
    private static final int MAX_ROLES = 20; // Prevent role explosion

    private static final Logger log = LoggerFactory.getLogger(SecureAppRoleValidator.class);

    private RoleAuditService auditService;

    @Override
    public void initialize(SensitiveData constraintAnnotation) {
        String[] roles = constraintAnnotation.rolesAllowed();

        // Validate role configuration
        if (roles.length > MAX_ROLES) {
            throw new SecurityConfigurationException("Too many roles configured");
        }

        this.rolesAllowed = Arrays.stream(roles)
                .map(this::sanitizeAppRole)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logAccessDenied("No authentication");
                return false;
            }

            boolean hasAccess = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(rolesAllowed::contains);

            // Audit access attempts
            auditService.logAccessAttempt(
                    authentication.getName(),
                    value.getClass().getName(),
                    hasAccess
            );

            if (!hasAccess) {
                logAccessDenied("Insufficient privileges");
            }

            return hasAccess;

        } catch (Exception e) {
            log.error("Security validation error", e);
            return false; // Fail secure
        }
    }

    /**
     * Sanitizes a given role by removing any potentially dangerous characters.
     * The result will only contain alphanumeric characters and underscores.
     *
     * @param role the role to sanitize
     * @return a sanitized version of the role
     */
    public String sanitizeAppRole(String role) {
        // Remove any potentially dangerous characters
        return role.replaceAll("[^A-Za-z0-9_]", "");
    }

    private void logAccessDenied(String reason) {
        log.warn("Access denied: {} for resource at {}",
                reason,
                SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

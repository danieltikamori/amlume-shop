/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/**
 * SensitiveDataValidator - Role-based Access Control Validator
 * <p>
 * This class implements a custom validation mechanism for protecting sensitive data
 * in the application through role-based access control (RBAC). It works in conjunction
 * with the @SensitiveData annotation to enforce access restrictions based on user roles.
 * <p>
 * Key Features:
 * - Validates access to sensitive data based on user roles
 * - Integrates with Spring Security's authentication mechanism
 * - Supports multiple role-based authorization checks
 * <p>
 * Usage Example:
 * {@code
 *
 * @SensitiveData(rolesAllowed = {"ADMIN", "MANAGER"})
 * private String sensitiveField;
 * }
 * <p>
 * Validation Process:
 * 1. Checks if the user is authenticated
 * 2. Retrieves the user's granted authorities
 * 3. Compares user's roles against the allowed roles
 * 4. Grants or denies access based on role matching
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * @see me.amlu.shop.amlume_shop.config.SensitiveData
 * @see ConstraintValidator
 * @see org.springframework.security.core.Authentication
 * @since 2025-02-22
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SensitiveDataValidator implements ConstraintValidator<SensitiveData, Object> {
    private Set<String> rolesAllowedSet;

    @Override
    public void initialize(SensitiveData constraintAnnotation) {
        this.rolesAllowedSet = Arrays.stream(constraintAnnotation.rolesAllowed())
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rolesAllowedSet::contains);
    }
}

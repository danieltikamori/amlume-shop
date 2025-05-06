/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.aspect;

import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.user_management.AppRole;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAspect.class);
    private final UserService userService;

    public AuthorizationAspect(UserService userService) {
        this.userService = userService;
    }

    // Pointcut definition: targets any method annotated with @RequiresRole
    @Pointcut("@annotation(me.amlu.shop.amlume_shop.security.aspect.RequiresRole)")
    public void requiresRoleAnnotation() {
    }

    // Advice definition: runs *before* the method execution
    @Before("requiresRoleAnnotation()")
    public void checkRole(JoinPoint joinPoint) throws UnauthorizedException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole requiresRole = method.getAnnotation(RequiresRole.class);
        AppRole[] requiredRoles = Objects.requireNonNull(requiresRole).value();

        if (requiredRoles == null || requiredRoles.length == 0) {
            // Should not happen if annotation is used correctly, but good practice
            log.warn("Method {} annotated with @RequiresRole but no roles specified.", method.getName());
//            return; // Or throw an exception indicating misconfiguration
            throw new IllegalStateException("Method annotated with @RequiresRole but no roles specified.");
        }

        User currentUser = userService.getCurrentUser(); // This throws if not authenticated

        // --- Check if the user has *any* of the required roles ---
        // This assumes User has a method like getRoles() returning Set<UserRole>
        // and UserRole has getRoleName() returning AppRole. Adjust if your structure differs.
        Set<AppRole> userRoles = currentUser.getRoles().stream()
                .map(UserRole::getRoleName) // Extract the AppRole enum
                .collect(Collectors.toSet());

        boolean authorized;
        if (requiresRole.requireAll()) {
            // Check if user has ALL required roles
            authorized = Arrays.stream(requiredRoles)
                    .allMatch(userRoles::contains);
            if (!authorized) {
                log.warn("Authorization failed: User '{}' does not have ALL required roles {} for method {}",
                        currentUser.getUsername(), Arrays.toString(requiredRoles), method.getName());
                throw new UnauthorizedException("User does not have ALL the required permissions: " + Arrays.toString(requiredRoles));
            }

        } else {
            // Check if user has ANY of the required roles
            authorized = Arrays.stream(requiredRoles)
                    .anyMatch(userRoles::contains);
            if (!authorized) {
                log.warn("Authorization failed: User '{}' does not have required roles {} for method {}",
                        currentUser.getUsername(), Arrays.toString(requiredRoles), method.getName());
                throw new UnauthorizedException("User does not have the required permissions: " + Arrays.toString(requiredRoles));
            }
        }

        log.debug("Authorization successful: User '{}' has required roles for method {}",
                currentUser.getUsername(), method.getName());

    }
}
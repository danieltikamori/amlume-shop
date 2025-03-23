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

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.security.service.UserServiceImpl;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(1) // Ensure authentication check runs before other aspects
public class AuthenticationAspect {

    private final UserServiceImpl userService;

    public AuthenticationAspect(UserServiceImpl userService) {
        if (userService == null) {
            throw new IllegalArgumentException("UserService cannot be null");
        }
        this.userService = userService;
    }

    @Before("@annotation(requiresAuthentication) || @within(requiresAuthentication)")
    public void checkAuthentication(JoinPoint joinPoint, RequiresAuthentication requiresAuthentication)
            throws UnauthorizedException {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = signature.getDeclaringType().getSimpleName();

            log.debug("Checking authentication for method: {}.{}", className, methodName);

            userService.getCurrentUser();

            log.debug("Authentication successful for method: {}.{}", className, methodName);
        } catch (UnauthorizedException e) {
            log.warn("Authentication failed for {}.{}: {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getMessage());

            if (requiresAuthentication.strict()) {
                throw new UnauthorizedException(requiresAuthentication.message(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected error during authentication check for {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e);

            if (requiresAuthentication.strict()) {
                throw new UnauthorizedException(requiresAuthentication.message(), e);
            }
        }
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint joinPoint, RequiresRole requiresRole)
            throws UnauthorizedException {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = signature.getDeclaringType().getSimpleName();

            log.debug("Checking role '{}' for method: {}.{}",
                    requiresRole.value(), className, methodName);

            var user = userService.getCurrentUser();
            if (!userService.hasRole(user, requiresRole.value())) {
                log.warn("Role check failed for user {} accessing {}.{}",
                        user.getUsername(), className, methodName);
                throw new UnauthorizedException("Required role: " + requiresRole.value());
            }

            log.debug("Role check successful for method: {}.{}", className, methodName);
        } catch (UnauthorizedException e) {
            log.warn("Role check failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during role check", e);
            throw new UnauthorizedException("Role check failed due to system error");
        }
    }
}
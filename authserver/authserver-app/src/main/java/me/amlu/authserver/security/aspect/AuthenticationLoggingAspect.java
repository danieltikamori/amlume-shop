/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
public class AuthenticationLoggingAspect {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationLoggingAspect.class);

    //    @Around("execution(* com.yourapp.security.*.*(..)) && @annotation(Audited)")
    @Around("execution(* me.amlu.shop.amlume_shop.auth.service.UserAuthenticator.*(..))") // More specific pointcut
    public Object logAuthenticationActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        MDC.put("className", className);
        MDC.put("methodName", methodName);
        MDC.put("timestamp", Instant.now().toString());
        try {
            log.info("Starting authentication operation: {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            log.info("Successfully completed authentication operation");
            return result;
        } catch (Exception e) {
            log.error("Authentication operation failed", e);
            throw e;
        } finally {
            MDC.clear(); // Clear the MDC context
        }
    }
}

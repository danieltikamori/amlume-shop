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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Aspect to ensure consistent ObjectMapper usage
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ObjectMapperAspect {
    private final ObjectMapper objectMapper;

    @Around("execution(* com.fasterxml.jackson.databind.ObjectMapper.*(..)) && !within(me.amlu.shop.amlume_shop.config.*)")
    public Object ensureConsistentObjectMapper(ProceedingJoinPoint joinPoint) throws Throwable {
        if (joinPoint.getTarget() != objectMapper) {
            // Log warning about inconsistent ObjectMapper usage
            log.warn("Inconsistent ObjectMapper usage detected in {}", 
                    joinPoint.getSignature().getDeclaringTypeName());
            
            // Replace the target with our configured ObjectMapper
            return joinPoint.proceed(new Object[]{objectMapper});
        }
        return joinPoint.proceed();
    }
}


/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DeletionSecurityAspect {

    // TOCHECK: Update/modify this aspect for production
    @Before("execution(* me.amlu.service.*Service.permanentlyDelete*(..))")
    public void checkDeletionPermission(JoinPoint joinPoint) {
        // Get the currently authenticated user
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();

        // Check if the user has the required role or permission
        if (!currentUser.contains("ROLE_ADMIN") && !currentUser.contains("ROLE_ROOT")) {
            throw new SecurityException("Unauthorized deletion attempt.");
        }
    }
}
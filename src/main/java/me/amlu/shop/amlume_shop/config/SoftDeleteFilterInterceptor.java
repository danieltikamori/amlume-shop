/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component public class SoftDeleteFilterInterceptor implements HandlerInterceptor {

    @PersistenceContext  // Inject the EntityManager here
    private EntityManager entityManager;

   @Override
   public boolean preHandle(@Valid @NotNull HttpServletRequest request,
                            @NotNull HttpServletResponse response,
                            @NotNull Object handler) throws Exception {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       if (authentication != null && authentication.isAuthenticated()) {
           boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ROOT"));
           Session session = entityManager.unwrap(Session.class);
           if (isAdmin) {
               session.disableFilter("deletedFilter"); // Disable for admins
           } else {
               session.enableFilter("deletedFilter");  // Enable for other users
           }
       }
       return true; // Continue with request processing
   }
   // ... Might need postHandle or afterCompletion(implemented below) methods to clean up filters.
   @Override
   public void afterCompletion(@Valid @NotNull HttpServletRequest request,
                               @NotNull HttpServletResponse response,
                               @NotNull Object handler, Exception ex) throws Exception {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       if (authentication != null && authentication.isAuthenticated()) {
           Session session = entityManager.unwrap(Session.class);
           session.disableFilter("deletedFilter"); // Disable after request completion
       }
   }
}
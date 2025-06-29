/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * •Purpose: Logs authentication success/failure after the filter chain completes.
 * <p>
 * •Placement: As a standard @Component filter,
 * its order relative to the explicitly configured chain isn't guaranteed without @Order.
 * It likely runs after the SecurityFilterChain bean's filters.
 * This is generally acceptable for logging the outcome.
 * <p>
 * •Logic: Checks SecurityContextHolder after the chain.
 * Catches AuthenticationException.
 * <p>
 * •Issues/Improvements:•If an earlier filter catches AuthenticationException
 * and handles the response without re-throwing,
 * this filter's catch block won't execute.
 */

@DependsOn("springSecurityFilterChain") // Or @Order
@Component
public class AuthenticationLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // Add check to exclude anonymous user
            if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                log.info("Successful authentication for user: {}", authentication.getName());
            }

        } catch (AuthenticationException e) {
            log.warn("Failed authentication attempt from IP: {}",
                    request.getRemoteAddr());
            throw e;
        }
    }
}

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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A filter that inspects the SESSION cookie for invalid characters (like null bytes)
 * before it reaches the SessionRepositoryFilter. This prevents database errors
 * caused by malformed session IDs from scanners or bad clients.
 */
public class InvalidSessionCookieFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InvalidSessionCookieFilter.class);
    private static final String SESSION_COOKIE_NAME = "SESSION";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    String sessionId = cookie.getValue();
                    if (sessionId != null && sessionId.indexOf('\0') != -1) {
                        log.warn("Rejected request from IP {} due to invalid null byte in SESSION cookie.", request.getRemoteAddr());
                        response.setStatus(HttpStatus.BAD_REQUEST.value());
                        response.getWriter().write("Invalid session identifier.");
                        return; // Stop the filter chain
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

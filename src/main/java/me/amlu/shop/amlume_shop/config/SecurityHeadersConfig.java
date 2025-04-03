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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Set the filter order
    public FilterRegistrationBean<OncePerRequestFilter> securityHeadersFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NotNull HttpServletRequest request,
                                            @NotNull HttpServletResponse response,
                                            @NotNull FilterChain filterChain)
                    throws ServletException, IOException {

                // Prevent MIME-sniffing
                response.setHeader("X-Content-Type-Options", "nosniff");

                // Prevent clickjacking
                response.setHeader("X-Frame-Options", "DENY");

                // Enable browser's XSS filter
                response.setHeader("X-XSS-Protection", "1; mode=block");

                // Enforce HTTPS and prevent protocol downgrade attacks
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

                // Prevent caching of sensitive data
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache"); // For HTTP 1.0 compatibility

                // Control referrer information
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

                // Content Security Policy (CSP) - Customize this to fit your application's needs!
                response.setHeader("Content-Security-Policy",
                        "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline'; " + // Add 'unsafe-inline' if you use inline scripts
                                "style-src 'self' 'unsafe-inline'; " + // Add 'unsafe-inline' if you use inline styles
                                "img-src 'self' data:; " +
                                "font-src 'self'; " +
                                "object-src 'none'; " +
                                "frame-ancestors 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self';"
                );

                // Permissions Policy - Customize this to fit the application's needs!
                response.setHeader("Permissions-Policy",
                        "geolocation=(), " +
                                "midi=(), " +
                                "sync-xhr=(), " +
                                "microphone=(), " +
                                "camera=(), " +
                                "magnetometer=(), " +
                                "gyroscope=(), " +
                                "fullscreen=(), " +
                                "payment=()"
                );

                filterChain.doFilter(request, response);
            }
        });
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Set the filter order
        return registrationBean;
    }
}

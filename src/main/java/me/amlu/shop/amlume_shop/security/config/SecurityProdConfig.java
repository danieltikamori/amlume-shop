/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.filter.CsrfCookieFilter;
import me.amlu.shop.amlume_shop.security.handler.CustomAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Enable method security annotations
@EnableAsync
@Configuration
@Profile("prod")
public class SecurityProdConfig {

    @Value("${security.max-concurrent-sessions:2}")
    private final int maxConcurrentSessions;

    public SecurityProdConfig(@Value("${security.max-concurrent-sessions:2}") int maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
        http.sessionManagement(sessionConfig -> sessionConfig
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // STATELESS is preferred for token-based auth.
                        // IF_REQUIRED allows sessions but doesn't create one unless needed (e.g., by framework components or explicit use).
                        // Choose based on whether you rely on HttpSession state anywhere.
                        // Session fixation protection
                        .sessionFixation().migrateSession()
                        // Concurrent session control (only relevant if policy is not STATELESS)
                        .maximumSessions(this.maxConcurrentSessions)
                        .maxSessionsPreventsLogin(true)
                        .expiredUrl("/login?expired")) // Redirect if session expires (relevant for IF_REQUIRED/ALWAYS))
                .cors(corsConfig -> corsConfig.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration config = new CorsConfiguration();
                        config.setAllowedOrigins(Collections.singletonList("https://localhost:4200"));
                        config.setAllowedMethods(Collections.singletonList("*"));
                        config.setAllowCredentials(true);
                        config.setAllowedHeaders(Collections.singletonList("*"));
                        config.setExposedHeaders(List.of("Authorization"));
                        config.setMaxAge(3600L);
                        return config;
                    }
                }))
                .csrf(csrfConfig -> csrfConfig.csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
                        .ignoringRequestMatchers("/contact", "/register")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .requiresChannel(rcc -> rcc.anyRequest().requiresSecure()) // Only HTTPS
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/myAccount").hasRole("USER")
                        .requestMatchers("/myOrders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/myProfile").authenticated()
                        .requestMatchers("/myAddress").hasRole("USER")
                        .requestMatchers("/user").authenticated()
                        .requestMatchers("/notifications", "/contact",
                                // Authentication endpoints
                                "/register",
                                "/api/auth/register",
                                "/api/auth/login", // Endpoint handled by CustomAuthenticationFilter. The path is in Constants.java
//                                "/api/auth/login", // Fallback?
                                "/api/auth/qrcode", // Needs authentication
                                "/api/auth/logout", // Needs authentication (to revoke token)

                                // Public assets and error pages
                                "/public/**",
                                "/error",
                                "/login", // Allow access to login page if using formLogin

                                // Actuator endpoints (secure appropriately in production)
                                "/actuator/**"
                        ).permitAll());
        http.oauth2ResourceServer(rsc -> rsc.jwt(jwtConfigurer ->
                jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        http.exceptionHandling(ehc -> ehc.accessDeniedHandler(new CustomAccessDeniedHandler()));
        return http.build();
    }

}

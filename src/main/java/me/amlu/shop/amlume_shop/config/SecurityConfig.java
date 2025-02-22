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

import me.amlu.shop.amlume_shop.filter.CustomAuthenticationFilter;
import me.amlu.shop.amlume_shop.filter.DeviceFingerprintVerificationFilter;
import me.amlu.shop.amlume_shop.filter.MfaAuthenticationFilter;
import me.amlu.shop.amlume_shop.security.auth.MfaAuthenticationProvider;
import me.amlu.shop.amlume_shop.security.handler.AuthenticationFailureHandler;
import me.amlu.shop.amlume_shop.security.paseto.PasetoAuthenticationFilter;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl;
import org.apache.catalina.filters.RateLimitFilter;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableReactiveMethodSecurity
@EnableAsync
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final PasetoTokenServiceImpl pasetoTokenService;
    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final MfaAuthenticationFilter mfaAuthenticationFilter;
    private final MfaAuthenticationProvider mfaAuthenticationProvider;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter; // Inject the fingerprint filter


//    @Value("${PASETO_SECRET_KEY}")
//    private String pasetoKey;
//
////    // Instead of using SecretKeyConstant class
////    @Bean
////    public PasetoTokenService pasetoTokenService() {
////        return new PasetoTokenService(pasetoKey);
////    }


    public SecurityConfig(RateLimitFilter rateLimitFilter, PasetoTokenServiceImpl pasetoTokenService, CustomAuthenticationFilter customAuthenticationFilter, MfaAuthenticationFilter mfaAuthenticationFilter, MfaAuthenticationProvider mfaAuthenticationProvider, AuthenticationFailureHandler authenticationFailureHandler, DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter) {
        this.rateLimitFilter = rateLimitFilter;
        this.pasetoTokenService = pasetoTokenService;
        this.customAuthenticationFilter = customAuthenticationFilter;
        this.mfaAuthenticationFilter = mfaAuthenticationFilter;
        this.mfaAuthenticationProvider = mfaAuthenticationProvider;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.deviceFingerprintVerificationFilter = deviceFingerprintVerificationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
//                .cors(cors -> cors.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/**")) // Important: Ignore CSRF for auth endpoints

//                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login") // Your login page
                        .failureHandler(authenticationFailureHandler) // Use the custom authentication failure handler
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Or STATELESS if using JWTs exclusively
                        .maximumSessions(1) // Allow only one session per user
                        .maxSessionsPreventsLogin(true) // Prevent new login when max sessions reached
                        .expiredUrl("/login?expired")) // Redirect when session expires
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession() // Protect against session fixation
                        .sessionAuthenticationErrorUrl("/login?error") // Authentication error redirect
                        .invalidSessionUrl("/login?invalid")) // Redirect for invalid sessions
                .authenticationProvider(mfaAuthenticationProvider)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(deviceFingerprintVerificationFilter, UsernamePasswordAuthenticationFilter.class) // Fingerprint filter FIRST
                .addFilterBefore(new PasetoAuthenticationFilter(pasetoTokenService), UsernamePasswordAuthenticationFilter.class) // Then Paseto
                .addFilterBefore(customAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mfaAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(new PasetoAuthenticationFilter(pasetoTokenService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://amlu.me", "https://shop.amlu.me", "https://tikamori.com", "http://localhost:3000", "http://localhost:8080", "http://localhost:8081", "http://localhost:6379"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // Renamed to avoid duplicate bean name
    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> sessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,    // saltLength
                32,    // hashLength
                1,     // parallelism
                1 << 14, // memory
                2       // iterations
        );
    }

}

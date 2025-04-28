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

import me.amlu.shop.amlume_shop.filter.*;
import me.amlu.shop.amlume_shop.security.auth.MfaAuthenticationProvider;
import me.amlu.shop.amlume_shop.security.handler.AuthenticationFailureHandler;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Enable method security annotations
@EnableAsync
public class SecurityConfig {

    @Value("${security.max-concurrent-sessions:2}")
    private final int maxConcurrentSessions;

    private final AuthenticationConfiguration authenticationConfiguration;
    private final GlobalRateLimitingFilter globalRateLimitingFilter; // Assuming this is correctly configured elsewhere
    private final PasetoTokenService pasetoTokenService; // Use interface
    private final CustomAuthenticationFilter customAuthenticationFilter; // Assumes this handles user/pass
    private final MfaAuthenticationFilter mfaAuthenticationFilter; // Handles MFA verification step
    private final MfaAuthenticationProvider mfaAuthenticationProvider; // Provider for MFA logic
    private final AuthenticationFailureHandler authenticationFailureHandler; // Handles login failures
    private final DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter; // Verifies device post-auth

    // Inject AuthenticationManager if custom filters need it
    // private final AuthenticationManager authenticationManager;

    // Inject allowed origins from properties
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(@Value("${security.max-concurrent-sessions:2}") int maxConcurrentSessions, AuthenticationConfiguration authenticationConfiguration, GlobalRateLimitingFilter globalRateLimitingFilter,
                          PasetoTokenService pasetoTokenService,
                          CustomAuthenticationFilter customAuthenticationFilter,
                          MfaAuthenticationFilter mfaAuthenticationFilter,
                          MfaAuthenticationProvider mfaAuthenticationProvider,
                          AuthenticationFailureHandler authenticationFailureHandler,
                          DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter
            /*, AuthenticationManager authenticationManager */) {
        this.maxConcurrentSessions = maxConcurrentSessions;
        this.authenticationConfiguration = authenticationConfiguration;
        this.globalRateLimitingFilter = globalRateLimitingFilter;
        this.pasetoTokenService = pasetoTokenService;
        this.customAuthenticationFilter = customAuthenticationFilter;
        this.mfaAuthenticationFilter = mfaAuthenticationFilter;
        this.mfaAuthenticationProvider = mfaAuthenticationProvider;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.deviceFingerprintVerificationFilter = deviceFingerprintVerificationFilter;
        // this.authenticationManager = authenticationManager;
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Order(100) // Define order if multiple SecurityFilterChain beans exist
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Configure CSRF for SPA (if applicable)
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // Setting to null uses default "_csrf"

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // Send CSRF token in cookie accessible by JS
                        .csrfTokenRequestHandler(requestHandler) // Use the request handler
                        .ignoringRequestMatchers("/api/auth/**") // Ignore CSRF for stateless auth endpoints
                )
                // If NOT using form login (only token/API based)
                .formLogin(AbstractHttpConfigurer::disable) // Disable default form login
                // If using form login (e.g., for initial login before token issuance)
                /*
                .formLogin(formLogin -> formLogin
                        .loginPage("/login") // Your custom login page URL
                        .loginProcessingUrl("/api/auth/login") // URL where login form is submitted
                        .failureHandler(authenticationFailureHandler) // Use the custom failure handler
                        .permitAll() // Allow access to login page and processing URL
                )
                */
                .sessionManagement(session -> session
                        // STATELESS is preferred for token-based auth.
                        // IF_REQUIRED allows sessions but doesn't create one unless needed (e.g., by framework components or explicit use).
                        // Choose based on whether you rely on HttpSession state anywhere.
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Or STATELESS
                        // Session fixation protection
                        .sessionFixation().migrateSession()
                        // Concurrent session control (only relevant if policy is not STATELESS)
                        .maximumSessions(this.maxConcurrentSessions)
                        .maxSessionsPreventsLogin(true)
                        .sessionRegistry(sessionRegistry()) // Register the session registry
                        .expiredUrl("/login?expired") // Redirect if session expires (relevant for IF_REQUIRED/ALWAYS)
                )
                // Register the custom MFA Authentication Provider
                .authenticationProvider(mfaAuthenticationProvider)

                // --- Filter Chain Order ---
                // 1. Global Rate Limiting (Very Early)
                .addFilterBefore(globalRateLimitingFilter, UsernamePasswordAuthenticationFilter.class) // Or CsrfFilter.class if CSRF is earlier

                // 2. PASETO Token Authentication (Attempt token auth first)
                .addFilterBefore(new PasetoAuthenticationFilter(pasetoTokenService), UsernamePasswordAuthenticationFilter.class)

                // 3. Custom Username/Password Authentication (If token fails or not present)
                // This filter should attempt authentication and set SecurityContext if successful (potentially partially if MFA needed)
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Place before default user/pass filter

//                // (REMOVED) 4. MFA Authentication Filter (Runs if prior auth succeeded but requires MFA)
//                // This filter checks for MFA requirement and validates the MFA code header/parameter.
//                // It should run AFTER the filter that establishes initial authentication (e.g., CustomAuthenticationFilter or PasetoAuthenticationFilter).
//                .addFilterAfter(mfaAuthenticationFilter, CustomAuthenticationFilter.class) // Run after custom user/pass filter

                // 5. Device Fingerprint Verification (Runs AFTER all authentication is successful)
                // Placed after AuthorizationFilter to ensure SecurityContext is fully populated.
                .addFilterAfter(deviceFingerprintVerificationFilter, AuthorizationFilter.class)

                // --- Authorization Rules ---
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        // Authentication endpoints
                                        "/api/auth/v1/register",
                                        "/api/auth/v1/login", // Endpoint handled by CustomAuthenticationFilter. The path is in Constants.java
//                                "/api/auth/login", // Fallback?
                                        "/api/auth/v1/mfa/validate", // Endpoint handled by MfaAuthenticationFilter? Or controller? Needs clarity.
//                                "/api/auth/v1/mfa/enable", // Needs authentication - Logic handled by provider during login
                                        "/api/auth/v1/qrcode", // Needs authentication
                                        "/api/auth/logout", // Needs authentication (to revoke token)

                                        // Public assets and error pages
                                        "/public/**",
                                        "/error",
                                        "/login", // Allow access to login page if using formLogin

                                        // Actuator endpoints (secure appropriately in production)
                                        "/actuator/**"
                                ).permitAll()
                                //Secure MFA enablement/QR code/logout endpoints
                                .requestMatchers(
                                        "/api/auth/v1/mfa/enable",
                                        "/api/auth/v1/qrcode",
                                        "/api/auth/logout"
                                        // Add other authenticated-only auth endpoints if any
                                ).authenticated()
                                // Default is to deny - all other requests must be authenticated
                                .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use the injected allowedOrigins list
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-MFA-Code", "User-Agent", /* Add other custom headers */ "Screen-Width", "Screen-Height"));
        configuration.setExposedHeaders(Arrays.asList("X-XSRF-TOKEN", "Retry-After", "Mfa-Status")); // Expose the necessary headers
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache CORS preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }

    // Bean for Session Registry (needed for concurrent session control)
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // Bean to publish session events (needed for SessionRegistry)
    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    /**
     * Bean for HaveIBeenPwned password checker.
     * This checks if the password has been compromised using the HaveIBeenPwned API.
     *
     * @return CompromisedPasswordChecker instance.
     */
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker(); // Use the HaveIBeenPwned service for compromised password checking
    }

    /**
     * Bean for RoleHierarchy.
     * This defines the role hierarchy for authorization checks.
     *
     * @return RoleHierarchy instance.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        // Define role hierarchy - adjust based on your actual roles
        // Example: ADMIN can do everything a MANAGER can, MANAGER can do everything a USER can.
        String hierarchy = """
                ROLE_ROOT > ROLE_SUPER_ADMIN
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_MANAGER > ROLE_SELLER_MANAGER
                ROLE_SELLER_MANAGER > ROLE_SELLER
                ROLE_SELLER > ROLE_CUSTOMER
                ROLE_CUSTOMER > ROLE_USER
                """;
        // Add other relationships as needed, e.g.:
        // ROLE_ADMIN > ROLE_MODERATOR
        // ROLE_SELLER_MANAGER > ROLE_SELLER_STAFF
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    // Optional: Expose AuthenticationManager bean if needed by custom filters
    /*
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // Configure providers if needed (e.g., UserDetailsService)
        // authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        authenticationManagerBuilder.authenticationProvider(mfaAuthenticationProvider); // Add MFA provider
        return authenticationManagerBuilder.build();
    }
    */
}
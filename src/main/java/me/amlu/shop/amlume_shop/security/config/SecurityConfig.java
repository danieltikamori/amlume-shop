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

import me.amlu.shop.amlume_shop.security.handler.CustomAccessDeniedHandler;
import me.amlu.shop.amlume_shop.security.oauth2.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Keep method security
@Profile("local")
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;


    // --- Simplified Constructor ---
    public SecurityConfig(CustomOidcUserService customOidcUserService, CorsConfigurationSource corsConfigurationSource, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.customOidcUserService = customOidcUserService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }


    @Bean
    @Order(100) // Keep order if other SecurityFilterChain beans might exist
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // --- Session Management: STATELESS for APIs ---
                .sessionManagement(sessionConfig -> sessionConfig
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // --- CORS Configuration ---
                .cors(corsConfig -> corsConfig.configurationSource(this.corsConfigurationSource))
                // --- CSRF: Disable for stateless JWT-based APIs ---
                .csrf(AbstractHttpConfigurer::disable)
                // --- Remove Form Login ---
                // .formLogin(AbstractHttpConfigurer::disable) // Disable default form login
                // --- Remove Custom Filters (if they handled authentication/tokens) ---
                // Remove .addFilterBefore / .addFilterAfter for custom auth filters

                // --- Security Headers (Keep or adjust) ---
                .headers(headers -> headers // Keep security headers
                                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                                .contentSecurityPolicy(csp -> csp.policyDirectives("script-src 'self'; object-src 'none';")) // Adjust CSP as needed
                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                                .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                        // .referrerPolicy(...)
                )

                // --- OAuth2 Resource Server Configuration ---
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(this.jwtAuthenticationConverter)
                        )
                )
                // --- For Opaque tokens - recommended for critical applications -
                // --- disable the other oauth2ResourceServer block ---
//                .oauth2ResourceServer(rsc ->
//                        rsc.opaqueToken(otc ->
//                                otc.authenticationConverter(new KeycloakOpaqueRoleConverter())
//                                        .introspectionUri(introspectionUri).introspectionClientCredentials(this.clientId,this.clientSecret)))

                // --- Authorization Rules ---
                // Enables OAuth2 login with the configured client
                .oauth2Login(oauth2Login -> oauth2Login
                                .userInfoEndpoint(userInfo -> userInfo
                                        .oidcUserService(this.customOidcUserService) // Use your custom service
                                )
                        // Optional: Configure login page, success/failure handlers if needed
                        // .loginPage("/login") // If you have a custom login initiation page
                        // .defaultSuccessUrl("/home", true) // Redirect after successful login
                )
                .authorizeHttpRequests((requests) -> requests
                        // --- Public Endpoints ---
                        .requestMatchers(
                                "/api/auth/v1/register", // Keep if amlume-shop has its own reg form calling authserver API
                                // "/api/auth/v1/login", // REMOVE: Login is via OAuth2 redirect
                                "/api/auth/**", // Review what's under here, some might need auth
                                "/login", // Spring Security's default login page path, or your custom one
                                "/public/**",
                                "/error",
                                "/actuator/**", // Secure actuator in production!
                                "/",
                                "/index.html"
                                // Add any other truly public endpoints here
                        ).permitAll()
                        // --- Specific Role-Based Rules ---
                        // Ensure role names match what Keycloak provides (e.g., "admin", "user")
                        // The jwtAuthenticationConverter adds the "ROLE_" prefix.
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Use "ADMIN" if role is "admin"
                        .requestMatchers("/api/auth/v1/register/admin").hasRole("ADMIN") // Secure admin registration
                        .requestMatchers("/myAccount").hasRole("USER")
                        .requestMatchers("/myOrders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/myProfile").authenticated()
                        .requestMatchers("/myAddress").hasRole("USER")
                        .requestMatchers("/user").authenticated()
                        // Example using Keycloak scopes if configured
                        // .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders.read")
                        // --- Default Rule: Any other request needs authentication ---
                        .anyRequest().authenticated()
                )

                // --- Exception Handling ---
                .exceptionHandling(ehc -> ehc.accessDeniedHandler(new CustomAccessDeniedHandler())); // Keep custom access denied handler

        return http.build();
    }

    // REMOVE: SessionRegistry and HttpSessionEventPublisher beans if using STATELESS

    // Keep RoleHierarchy if your @PreAuthorize/@Secured annotations rely on it
    @Bean
    public RoleHierarchy roleHierarchy() {
        // Ensure this hierarchy matches the roles defined in Keycloak and mapped by jwtAuthenticationConverter
        String hierarchy = """
                ROLE_ROOT > ROLE_SUPER_ADMIN
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_MANAGER > ROLE_SELLER_MANAGER
                ROLE_SELLER_MANAGER > ROLE_SELLER
                ROLE_SELLER > ROLE_CUSTOMER
                ROLE_CUSTOMER > ROLE_USER
                """;
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    // --- Custom Converter Class ---

    /**
     * Custom converter to extract roles from Keycloak's JWT structure.
     * Looks within 'realm_access' -> 'roles'.
     * You can extend this to also look in 'resource_access'.
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Check for realm_access claim
            final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptySet(); // No realm access claim found
            }

            // Extract roles from realm_access.roles
            final Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");

            if (realmRoles == null || realmRoles.isEmpty()) {
                return Collections.emptySet(); // No roles within realm_access
            }

            // Map roles to GrantedAuthority objects with "ROLE_" prefix
            return realmRoles.stream()
                    .map(roleName -> "ROLE_" + roleName.toUpperCase()) // Add prefix and uppercase
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            // --- Optional: Include Resource Access Roles ---
            // Uncomment and adapt if you need client-specific roles
            /*
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Stream<String> resourceRolesStream = Stream.empty();

            if (resourceAccess != null && !resourceAccess.isEmpty()) {
                // Example: Get roles for a specific client ID
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("your-client-id"); // Replace with your actual client ID
                if (clientAccess != null) {
                    Collection<String> clientRoles = (Collection<String>) clientAccess.get("roles");
                    if (clientRoles != null) {
                        resourceRolesStream = clientRoles.stream();
                    }
                }
                // You could iterate over all clients in resourceAccess if needed
            }

            // Combine realm and resource roles
            return Stream.concat(realmRoles.stream(), resourceRolesStream)
                    .map(roleName -> "ROLE_" + roleName.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            */
            // --- End Optional Resource Access ---
        }
    }
}

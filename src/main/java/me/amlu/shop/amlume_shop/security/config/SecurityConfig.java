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

import me.amlu.shop.amlume_shop.exceptions.CustomAccessDeniedHandler;
// REMOVE: Custom filter imports if they are no longer needed (most likely)
// import me.amlu.shop.amlume_shop.filter.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
// REMOVE: AuthenticationManager imports if not explicitly needed by remaining custom components
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Import for disabling CSRF/formLogin concisely
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
// REMOVE: Session registry beans if using STATELESS policy
// import org.springframework.security.core.session.SessionRegistry;
// import org.springframework.security.core.session.SessionRegistryImpl;
// import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
// import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Keep method security
// REMOVE: @EnableAsync if only used for removed components
@Profile("local") // Keep profile if needed
public class SecurityConfig {

    // REMOVE: maxConcurrentSessions if using STATELESS
    // private final int maxConcurrentSessions;

    // REMOVE: AuthenticationConfiguration, custom filters, AuthenticationManager if not needed
    // private final AuthenticationConfiguration authenticationConfiguration;
    // private final GlobalRateLimitingFilter globalRateLimitingFilter;
    // private final DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter;
    // private final AuthenticationManager authenticationManager;

    // --- Values for Opaque Tokens

//        @Value("${spring.security.oauth2.resourceserver.opaque.introspection-uri}")
//    String introspectionUri;
//
//    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-client-id}")
//    String clientId;
//
//    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-client-secret}")
//    String clientSecret;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    // --- Simplified Constructor ---
    public SecurityConfig(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        // Remove injections for components that are being removed
    }

    // REMOVE: authenticationManager bean if not needed

    @Bean
    @Order(100) // Keep order if other SecurityFilterChain beans might exist
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // --- Session Management: STATELESS for APIs ---
                .sessionManagement(sessionConfig -> sessionConfig
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // --- CORS Configuration ---
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource())) // Keep CORS
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
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Configure role mapping
                        )
                )
                // --- For Opaque tokens - recommended for critical applications -
                // --- disable the other oauth2ResourceServer block ---
//                .oauth2ResourceServer(rsc ->
//                        rsc.opaqueToken(otc ->
//                                otc.authenticationConverter(new KeycloakOpaqueRoleConverter())
//                                        .introspectionUri(introspectionUri).introspectionClientCredentials(this.clientId,this.clientSecret)))
                // --- Authorization Rules ---
                .authorizeHttpRequests((requests) -> requests
                        // --- Public Endpoints ---
                        .requestMatchers(
                                "/api/auth/v1/register",
                                "/api/auth/v1/login",
                                "/login",
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
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Use "ADMIN" if Keycloak role is "admin"
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins); // Use property
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow Authorization header for Bearer token
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Retry-After")); // Adjust exposed headers
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures the JwtAuthenticationConverter to extract roles from the Keycloak token's
     * 'realm_access.roles' and potentially 'resource_access.<client-id>.roles' claims,
     * adding the "ROLE_" prefix.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        // Configure a custom converter to extract authorities
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        // Optional: Set principal name claim if different from 'sub' (e.g., 'preferred_username')
        // jwtConverter.setPrincipalClaimName("preferred_username");

        return jwtConverter;
    }


    // REMOVE: SessionRegistry and HttpSessionEventPublisher beans if using STATELESS

    // REMOVE: compromisedPasswordChecker bean

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

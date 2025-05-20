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

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.core.convert.converter.Converter;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.oauth2.jwt.Jwt;
    import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.CorsConfigurationSource;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

    import java.util.Arrays;
    import java.util.Collection;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Configuration
    public class ShopWebSecurityComponentsConfig {

        // Inject allowedOrigins directly here if needed, or pass from SecurityConfig if it's cleaner
        @Value("${cors.allowed-origins}")
        private List<String> allowedOrigins;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(allowedOrigins);
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
            configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Retry-After"));
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
            jwtConverter.setJwtGrantedAuthoritiesConverter(new SecurityConfig.KeycloakRealmRoleConverter());

            // Optional: Set principal name claim if different from 'sub' (e.g., 'preferred_username')
            // jwtConverter.setPrincipalClaimName("preferred_username");

            return jwtConverter;
        }

        // If RoleHierarchy is used by the filter chain or its components, move it here too.
        // For now, let's assume it's primarily for @EnableMethodSecurity and can stay in SecurityConfig.
    }

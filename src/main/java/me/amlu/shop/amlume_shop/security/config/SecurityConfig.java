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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.amlu.shop.amlume_shop.filter.DeviceFingerprintVerificationFilter;
import me.amlu.shop.amlume_shop.security.handler.CustomAccessDeniedHandler;
import me.amlu.shop.amlume_shop.security.oauth2.CustomOidcUserService;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
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
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/**
 * <p>
 * Security configuration for the amlume-shop application.
 * This class sets up Spring Security to use OAuth2 for authentication and authorization,
 * with JWTs as bearer tokens. It also configures session management, CORS, CSRF,
 * security headers, and role hierarchies.
 * </p>
 * <p>
 * This configuration is specifically active for the "local" Spring profile.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Keep method security
@Profile("local")
public class SecurityConfig {

    // Define a constant for the CSP nonce attribute name
    public static final String CSP_NONCE_ATTRIBUTE = "nonce";

    private final CustomOidcUserService customOidcUserService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ClientRegistrationRepository clientRegistrationRepository; // Inject this
    private final DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter;

    /**
     * Constructs a new {@code SecurityConfig} with the necessary dependencies.
     * Spring automatically injects these beans.
     *
     * @param customOidcUserService               The custom OIDC user service for handling user information from the OIDC provider.
     * @param corsConfigurationSource             The CORS configuration source for handling cross-origin requests.
     * @param jwtAuthenticationConverter          The converter responsible for extracting authorities from JWTs.
     * @param clientRegistrationRepository        The repository for OAuth2 client registrations, used for logout.
     * @param deviceFingerprintVerificationFilter The filter for verifying device fingerprints to enhance security.
     */
    public SecurityConfig(CustomOidcUserService customOidcUserService, CorsConfigurationSource corsConfigurationSource, JwtAuthenticationConverter jwtAuthenticationConverter, ClientRegistrationRepository clientRegistrationRepository, DeviceFingerprintVerificationFilter deviceFingerprintVerificationFilter) {
        this.customOidcUserService = customOidcUserService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.deviceFingerprintVerificationFilter = deviceFingerprintVerificationFilter;
    }


    /**
     * Configures the security filter chain for HTTP requests.
     * This bean defines the security rules, authentication mechanisms, and other security-related settings.
     *
     * @param http The {@link HttpSecurity} object to configure.
     * @return A {@link SecurityFilterChain} configured with the specified security rules.
     * @throws Exception If an error occurs during configuration.
     * @usage This is the primary method for defining web security rules.
     */
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
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny) // Prevent clickjacking, unless you need iframes
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER_WHEN_DOWNGRADE))
                        // OWASP recommends using XXssProtectionHeaderWriter.HeaderValue.DISABLED.
                        // If XXssProtectionHeaderWriter.HeaderValue.DISABLED, will specify that X-XSS-Protection is disabled
                        .contentSecurityPolicy(contentSecurityPolicyConfig -> contentSecurityPolicyConfig
                                        // IMPORTANT: 'nonce-{random-value}' should be generated per request
                                        // and added to both the CSP header and any inline script/style tags.
                                        // The 'strict-dynamic' keyword is crucial when using nonces as it allows
                                        // scripts loaded by trusted scripts (via nonce) to execute.
                                        .policyDirectives(
                                                "default-src 'self';" +
                                                        "script-src 'self' 'nonce-" + CSP_NONCE_ATTRIBUTE + "' 'strict-dynamic';" +
                                                        "style-src 'self' 'nonce-" + CSP_NONCE_ATTRIBUTE + "';" +
                                                        "img-src 'self' data:;" + // Allow self-hosted images and data URIs
                                                        "font-src 'self';" +
                                                        "connect-src 'self';" +
                                                        "object-src 'none';" + // Block plugins like Flash
                                                        "base-uri 'self';" + // Restrict base URL to self
                                                        "form-action 'self';" + // Restrict form submissions to self
                                                        "frame-ancestors 'self';" + // Prevent clickjacking by restricting embedding
                                                        "upgrade-insecure-requests;" + // Upgrade HTTP requests to HTTPS
                                                        "block-all-mixed-content;" // Block mixed content in secure contexts
                                                // Uncomment and configure for reporting CSP violations:
                                                // "report-uri /csp-report-endpoint;"
                                        )
                                // Optional: Start in report-only mode during development/testing
                                // .reportOnly()
                        )
                )
                // This filter is a good approach. Ensure it's correctly adding the nonce to the CSP header.
//                .addFilterBefore(new CspNonceFilter(), HeaderWriterFilter.class)
                // Add a filter to generate and expose the nonce
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                            throws ServletException, IOException {
                        String nonce = generateNonce();
                        // Store the nonce in request attribute so it can be accessed by your templating engine (e.g., Thymeleaf)
                        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
                        // Add the nonce to the response header for Content-Security-Policy
                        response.setHeader("Content-Security-Policy", getCspHeader(nonce));
                        filterChain.doFilter(request, response);
                    }

                    private String generateNonce() {
                        SecureRandom secureRandom = new SecureRandom();
                        byte[] nonceBytes = new byte[16]; // 16 bytes for 128 bits, good enough for nonce
                        secureRandom.nextBytes(nonceBytes);
                        return Base64.getEncoder().encodeToString(nonceBytes);
                    }

                    private String getCspHeader(String nonce) {
                        return "default-src 'self';" +
                                "script-src 'self' 'nonce-" + nonce + "' 'strict-dynamic';" +
                                "style-src 'self' 'nonce-" + nonce + "';" +
                                "img-src 'self' data:;" +
                                "font-src 'self';" +
                                "connect-src 'self';" +
                                "object-src 'none';" +
                                "base-uri 'self';" +
                                "form-action 'self';" +
                                "frame-ancestors 'self';" +
                                "upgrade-insecure-requests;" +
                                "block-all-mixed-content;";
                        // Add report-uri if needed
                        // "report-uri /csp-report-endpoint;";
                    }
                }, org.springframework.security.web.header.HeaderWriterFilter.class) // Place before HeaderWriterFilter


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
                                "/api/auth/register", // Endpoint for user registration (if amlume-shop handles it directly)
                                "/api/auth/**", // Review carefully: some /api/auth endpoints might require authentication.
                                // Ensure only truly public ones are here.
                                "/login**", // Spring Security's default login page path, or your custom one. Includes /login and /login?logout
                                "/public/**", // Static resources or public API endpoints
                                "/error", // Error page
                                "/actuator/health", "/actuator/info", // Basic actuator endpoints, secure others in production
                                "/",
                                "/index.html"
                                // Add any other truly public endpoints here
                        ).permitAll()
                        // --- Specific Role-Based Rules ---
                        // Ensure role names match what Keycloak provides (e.g., "admin", "user")
                        // The jwtAuthenticationConverter adds the "ROLE_" prefix.
                        .requestMatchers("/admin/**").hasRole("ADMIN") // Requires 'ROLE_ADMIN'
                        .requestMatchers("/api/auth/register/admin").hasRole("ADMIN") // Requires 'ROLE_ADMIN' for admin registration
                        .requestMatchers("/myAccount").hasRole("USER") // Requires 'ROLE_USER'
                        .requestMatchers("/myOrders").hasAnyRole("USER", "ADMIN") // Requires 'ROLE_USER' or 'ROLE_ADMIN'
                        .requestMatchers("/myProfile").authenticated() // Requires any authenticated user
                        .requestMatchers("/myAddress").hasRole("USER")
                        .requestMatchers("/user").authenticated()
                        // Example using Keycloak scopes if configured
                        // .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders.read")
                        // --- Default Rule: Any other request needs authentication ---
                        .anyRequest().authenticated()
                )

                // --- Device fingerprint ---
                .addFilterAfter(deviceFingerprintVerificationFilter, BearerTokenAuthenticationFilter.class)

                // --- Logout Configuration ---
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                )
                // --- Exception Handling ---
                .exceptionHandling(ehc -> ehc.accessDeniedHandler(new CustomAccessDeniedHandler())); // Keep custom access denied handler

        return http.build();
    }

    // REMOVE: SessionRegistry and HttpSessionEventPublisher beans if using STATELESS

    /**
     * Defines the role hierarchy for the application.
     * This allows for implicit granting of permissions based on role seniority.
     * For example, a 'ROLE_ADMIN' implicitly has all permissions of 'ROLE_USER'.
     * This hierarchy is used by Spring Security's expression-based access control.
     *
     * @return A {@link RoleHierarchy} instance defining the role relationships.
     */
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

    /**
     * Custom converter to extract {@link GrantedAuthority} from a JWT.
     * It processes two claims: "authorities" (for technical roles/permissions from the auth server)
     * and "business_roles" (for application-specific business roles).
     * All extracted roles are prefixed with "ROLE_" if not already present, to align with Spring Security's convention.
     */
    static class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

            // 1. Add authorities from authserver (roles and permissions)
            // This claim now contains both ROLE_ names and PERMISSION_ names from authserver
            List<String> authserverAuthorities = jwt.getClaimAsStringList("authorities");
            if (authserverAuthorities != null) {
                authserverAuthorities.stream()
                        .map(SimpleGrantedAuthority::new) // Converts "ROLE_USER", "PROFILE_READ_OWN" to GrantedAuthority
                        .forEach(grantedAuthorities::add);
            }

            // 2. Add business roles from amlume-shop (if still separate from authserver's roles)
            // This claim contains roles like "ROLE_SELLER", "ROLE_CUSTOMER", etc.
            List<String> businessRoles = jwt.getClaimAsStringList("business_roles");
            if (businessRoles != null) {
                // Ensure roles are prefixed with "ROLE_" for Spring Security's hasRole() method
                businessRoles.stream().map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new) // Assuming "ROLE_" prefix is already there
                        .forEach(grantedAuthorities::add);
            }

            return grantedAuthorities;
        }
    }

    /**
     * Configures the {@link JwtAuthenticationConverter} to use the custom
     * {@link CustomJwtGrantedAuthoritiesConverter}. This converter is responsible for
     * mapping JWT claims to Spring Security {@link GrantedAuthority} objects.
     *
     * @return A configured {@link JwtAuthenticationConverter}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Configures the {@link LogoutSuccessHandler} for OIDC client-initiated logout.
     * This handler redirects the user to a specified URI after successful logout from the
     * Authorization Server.
     *
     * @return An {@link OidcClientInitiatedLogoutSuccessHandler} instance.
     * @important The {@code {baseUrl}} placeholder will be replaced by Spring Security with the application's base URL.
     */
    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        // Set the URI where the user should be redirected after logout from the Authorization Server
        // {baseUrl} will be replaced by the base URL of your amlume-shop application (e.g., https://shop.amlu.me)
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout=true");
        return oidcLogoutSuccessHandler;
    }
}

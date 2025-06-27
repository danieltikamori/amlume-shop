/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import me.amlu.authserver.oauth2.service.JwtCustomizationService;
import me.amlu.authserver.security.CustomWebAuthnRelyingPartyOperations;
import me.amlu.authserver.security.WebAuthnPrincipalSettingSuccessHandler;
import me.amlu.authserver.security.handler.CustomAccessDeniedHandler;
import me.amlu.authserver.security.handler.CustomAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.webauthn.authentication.HttpSessionPublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Enable method security annotations
@EnableAsync
@Configuration
@Profile("prod")
public class SecurityProdConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityProdConfig.class);
    private final PersistentTokenRepository persistentTokenRepository;
    private final CustomAccessDeniedHandler customAccessDeniedHandler; // ADDED
    private final CustomAuthenticationSuccessHandler successHandler;
    private final WebAuthnAuthenticationProvider webAuthnAuthenticationProvider;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private final CustomWebAuthnRelyingPartyOperations relyingPartyOperations;
    private final AdminServerProperties adminServerProperties;
    private final JwtCustomizationService jwtCustomizationService;

    @Value("${spring.security.rememberme.key}")
    private String rememberMeKey;

    @Value("${security.max-concurrent-sessions:2}")
    private final int maxConcurrentSessions;

    public SecurityProdConfig(PersistentTokenRepository persistentTokenRepository,
                              CustomAccessDeniedHandler customAccessDeniedHandler,
                              CustomAuthenticationSuccessHandler successHandler,
                              WebAuthnAuthenticationProvider webAuthnAuthenticationProvider,
                              @Qualifier("customOidcUserService") OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
                              @Qualifier("customOAuth2UserService") OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
                              CustomWebAuthnRelyingPartyOperations relyingPartyOperations,
                              AdminServerProperties adminServerProperties,
                              JwtCustomizationService jwtCustomizationService,
                              @Value("${security.max-concurrent-sessions:2}") int maxConcurrentSessions) {
        this.persistentTokenRepository = persistentTokenRepository;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.successHandler = successHandler;
        this.webAuthnAuthenticationProvider = webAuthnAuthenticationProvider;
        this.oidcUserService = oidcUserService;
        this.oauth2UserService = oauth2UserService;
        this.relyingPartyOperations = relyingPartyOperations;
        this.adminServerProperties = adminServerProperties;
        this.jwtCustomizationService = jwtCustomizationService;
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http.with(new OAuth2AuthorizationServerConfigurer(), Customizer.withDefaults());
        RequestMatcher authorizationServerEndpointsMatcher = http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults())
                .getEndpointsMatcher();

        http
                .securityMatcher(authorizationServerEndpointsMatcher)
                // FIXED: Enforce HTTPS for all authorization server endpoints
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/openid-configuration").permitAll() // OIDC discovery is public
                        .anyRequest().authenticated() // All other AS endpoints require HTTPS and authentication
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .userDetailsService(userDetailsService)
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        String adminContextPath = this.adminServerProperties.getContextPath();
        SavedRequestAwareAuthenticationSuccessHandler adminSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        adminSuccessHandler.setTargetUrlParameter("redirectTo");
        adminSuccessHandler.setDefaultTargetUrl(adminContextPath + "/");

        http
                .securityMatcher(adminContextPath + "/**")
                // FIXED: Enforce HTTPS for all admin endpoints
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(adminContextPath + "/assets/**").permitAll()
                        .requestMatchers(adminContextPath + "/login").permitAll()
                        .anyRequest().hasAnyRole("ADMIN", "SUPER_ADMIN", "ROOT")
                )
                .formLogin(formLogin -> formLogin
                        .loginPage(adminContextPath + "/login")
                        .successHandler(adminSuccessHandler)
                )
                .logout(logout -> logout.logoutUrl(adminContextPath + "/logout"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                PathPatternRequestMatcher.withDefaults().matcher(adminContextPath + "/instances"),
                                PathPatternRequestMatcher.withDefaults().matcher(adminContextPath + "/actuator/**")
                        )
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          UserDetailsService userDetailsService,
                                                          @Qualifier("amlumeUsernamePwdAuthenticationProvider") AuthenticationProvider amlumeUsernamePwdAuthenticationProvider,
                                                          OidcAuthorizationCodeAuthenticationProvider oidcAuthenticationProvider,
                                                          OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider
    ) throws Exception {

        ProviderManager mainAuthenticationManager = new ProviderManager(
                this.webAuthnAuthenticationProvider,
                amlumeUsernamePwdAuthenticationProvider,
                oidcAuthenticationProvider,
                oauth2LoginAuthenticationProvider
        );
        http.authenticationManager(mainAuthenticationManager);

        AuthenticationSuccessHandler webAuthnSuccessHandler = new WebAuthnPrincipalSettingSuccessHandler(userDetailsService, successHandler);
        PublicKeyCredentialRequestOptionsFilter requestOptionsFilter = new PublicKeyCredentialRequestOptionsFilter(this.relyingPartyOperations);
        WebAuthnAuthenticationFilter webAuthnAuthenticationFilter = getWebAuthnAuthenticationFilter(mainAuthenticationManager, webAuthnSuccessHandler);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/images/**", "/css/**", "/js/**", "/favicons/**",
                                "/.well-known/appspecific/com.chrome.devtools.json",
                                "/", "/login", "/error",
                                "/webauthn/authenticate/options", "/login/webauthn/authenticate",
                                "/login/webauthn/options", "/login/webauthn",
                                "/webauthn/register/options", "/webauthn/register",
                                "/api/register/**"
                        ).permitAll()
                        .requestMatchers("/dashboard", "/api/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .addFilterAfter(requestOptionsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(webAuthnAuthenticationFilter, PublicKeyCredentialRequestOptionsFilter.class)
                .rememberMe(rememberMe -> rememberMe
                        .tokenRepository(persistentTokenRepository)
                        .userDetailsService(userDetailsService)
                        .key(this.rememberMeKey)
                        .tokenValiditySeconds((int) Duration.ofDays(30).toSeconds())
                )
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.oidcUserService)
                                .userService(this.oauth2UserService)
                        )
                        .successHandler(successHandler)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                // --- CORRECTED ---
                // Using PathPatternRequestMatcher for consistency and clarity
                .csrf(cfg -> cfg.ignoringRequestMatchers(
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/options"),
                        PathPatternRequestMatcher.withDefaults().matcher("/login/webauthn/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/webauthn/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/instances"),
                        PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                ))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/login?expired")
                )
                .requestCache(cache -> cache.requestCache(new HttpSessionRequestCache()))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }

    private static WebAuthnAuthenticationFilter getWebAuthnAuthenticationFilter(ProviderManager mainAuthenticationManager, AuthenticationSuccessHandler webAuthnSuccessHandler) {
        WebAuthnAuthenticationFilter webAuthnAuthenticationFilter = new WebAuthnAuthenticationFilter();
        webAuthnAuthenticationFilter.setAuthenticationManager(mainAuthenticationManager);
        webAuthnAuthenticationFilter.setRequestOptionsRepository(new HttpSessionPublicKeyCredentialRequestOptionsRepository());
        webAuthnAuthenticationFilter.setAuthenticationSuccessHandler(webAuthnSuccessHandler);
        return webAuthnAuthenticationFilter;
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new RestClientAuthorizationCodeTokenResponseClient();
    }

    @Bean
    public OidcAuthorizationCodeAuthenticationProvider oidcAuthenticationProvider(OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        return new OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient, this.oidcUserService);
    }

    @Bean
    public OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider(OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        return new OAuth2LoginAuthenticationProvider(accessTokenResponseClient, this.oauth2UserService);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // In production, be more specific with allowed origins
        config.addAllowedOrigin("https://shop.amlu.me");
        config.addAllowedOrigin("https://auth.amlu.me");
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return jwtCustomizationService::customizeToken;
    }
}

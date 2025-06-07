/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.webauthn4j.converter.jackson.WebAuthnJSONModule;
import com.webauthn4j.converter.util.ObjectConverter;
import jakarta.annotation.PostConstruct;
import me.amlu.authserver.oauth2.JpaRegisteredClientRepositoryAdapter;
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.oauth2.service.JwtCustomizationService;
import me.amlu.authserver.security.CustomAccessDeniedHandler;
import me.amlu.authserver.security.CustomWebAuthnRelyingPartyOperations;
import me.amlu.authserver.security.WebAuthnPrincipalSettingSuccessHandler;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.repository.UserRepository;
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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.webauthn.authentication.*;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Profile("!prod")
@Configuration
@EnableWebSecurity
/**
 * Security configuration for the local development profile.
 * Configures both the Authorization Server and the default application security chain.
 */
public class LocalSecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(LocalSecurityConfig.class);
    private final PersistentTokenRepository persistentTokenRepository;
    private final ObjectMapper objectMapper;
    private final JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter;
    private final WebAuthNProperties webAuthNProperties;
    private final DataSource dataSource; // Remove?
    private final PasswordEncoder passwordEncoder;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final WebAuthnAuthenticationProvider webAuthnAuthenticationProvider;
    //    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private final OAuth2AuthorizedClientService authorizedClientService; // Remove?
    private final CustomWebAuthnRelyingPartyOperations relyingPartyOperations;
    private final JwtCustomizationService jwtCustomizationService;


    // Inject secrets for OAuth2 client seeding from application-local.yml (which are loaded from Vault)
    @Value("${oauth2.clients.amlumeapi.secret}")
    private String amlumeapiClientSecret;

    @Value("${oauth2.clients.amlumeintrospect.secret}")
    private String amlumeintrospectClientSecret;

    @Value("${oauth2.clients.shopClient.secret}") // For amlume-shop
    private String shopClientSecret;

    @Value("${oauth2.clients.postmanClient.secret}") // For Postman testing
    private String postmanClientSecret;

    @Value("${spring.security.rememberme.key:${spring.security.webauthn.rpId}_RememberMeKey2024}")
    private String rememberMeKey;

    @Value("${spring.initial-root-user.email}")
    private String initialRootUserEmail;

    @Value("${spring.initial-root-user.password}")
    private String initialRootUserPassword;

    /**
     * Constructs the LocalSecurityConfig.
     *
     * @param objectMapper                         The Jackson ObjectMapper for JSON processing.
     * @param jpaRegisteredClientRepositoryAdapter Adapter for managing OAuth2 registered clients in the database.
     * @param webAuthNProperties                   Properties related to WebAuthn configuration.
     * @param persistentTokenRepository            Repository for persistent remember-me tokens.
     * @param dataSource                           The application's DataSource.
     * @param passwordEncoder                      The password encoder.
     * @param customAccessDeniedHandler            Custom handler for access denied exceptions.
     * @param webAuthnAuthenticationProvider       The Spring Security WebAuthn authentication provider.
     * @param relyingPartyOperations               Custom WebAuthn Relying Party operations.
     * @param oidcUserService                      Custom service for loading OIDC user information.
     * @param oauth2UserService                    Custom service for loading OAuth2 user information.
     */
    public LocalSecurityConfig(ObjectMapper objectMapper,
                               JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter,
                               WebAuthNProperties webAuthNProperties,
                               PersistentTokenRepository persistentTokenRepository,
                               DataSource dataSource,
                               PasswordEncoder passwordEncoder,
                               CustomAccessDeniedHandler customAccessDeniedHandler,
                               WebAuthnAuthenticationProvider webAuthnAuthenticationProvider,
//                               WebAuthnRelyingPartyOperations relyingPartyOperations,
                               CustomWebAuthnRelyingPartyOperations relyingPartyOperations,
                               @Qualifier("customOidcUserService") OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
                               @Qualifier("customOAuth2UserService") OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
                               OAuth2AuthorizedClientService authorizedClientService, JwtCustomizationService jwtCustomizationService
    ) {
        this.objectMapper = objectMapper;
        this.jpaRegisteredClientRepositoryAdapter = jpaRegisteredClientRepositoryAdapter;
        this.webAuthNProperties = webAuthNProperties;
        this.persistentTokenRepository = persistentTokenRepository;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.webAuthnAuthenticationProvider = webAuthnAuthenticationProvider;
        this.relyingPartyOperations = relyingPartyOperations;
        this.oidcUserService = oidcUserService;
        this.oauth2UserService = oauth2UserService;
        this.authorizedClientService = authorizedClientService;
        this.jwtCustomizationService = jwtCustomizationService;
    }


    /**
     * Configures the SecurityFilterChain for the OAuth2 Authorization Server endpoints.
     * This chain has the highest precedence and handles requests to /oauth2/* and /.well-known/*.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
            throws Exception {

        // --- Use the recommended 'http.with()' approach ---
        http.with(new OAuth2AuthorizationServerConfigurer(), Customizer.withDefaults());

        // Get the RequestMatcher for the authorization server endpoints.
        // This is used to ensure this SecurityFilterChain only applies to these specific endpoints.
        RequestMatcher authorizationServerEndpointsMatcher =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                        .oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
                        .getEndpointsMatcher();

        http
                // No explicit securityMatcher needed if applyDefaultSecurity handles it.
                // If you keep securityMatcher, ensure it includes .well-known paths or remove it
                // and let applyDefaultSecurity define the matcher.
                // For simplicity, let's rely on applyDefaultSecurity for matching.
                .securityMatcher(authorizationServerEndpointsMatcher) // CRITICAL: Restrict this chain to AS endpoints
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .userDetailsService(userDetailsService)
                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(Customizer.withDefaults()));
        http.authorizeHttpRequests((authorize) -> authorize.requestMatchers("/.well-known/openid-configuration").permitAll());

        return http.build();
    }

    // The JpaRegisteredClientRepositoryAdapter bean (defined as @Service)
    // will be automatically picked up by Spring Authorization Server.
    // No explicit InMemoryRegisteredClientRepository bean for client definitions is needed here.

    /**
     * Configures the default SecurityFilterChain for the application, handling all requests
     * not matched by the authorizationServerSecurityFilterChain. This includes form login,
     * WebAuthn login, OAuth2/OIDC login, and general application resource security.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @Order(2)
//    @DependsOn({"userDetailsService", "relyingPartyOperations", "amlumeUsernamePwdAuthenticationProvider", "webAuthnAuthenticationProvider", "dbUserCredentialRepository"})
//    @DependsOn({"userDetailsService", "relyingPartyOperations", "amlumeUsernamePwdAuthenticationProvider", "webAuthnAuthenticationProvider", "userCredentialRepository"})
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService, // Already injected via constructor
                                                          OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService, // Already injected via constructor
                                                          UserDetailsService userDetailsService,
                                                          PersistentTokenRepository persistentTokenRepository,
                                                          @Qualifier("amlumeUsernamePwdAuthenticationProvider") AuthenticationProvider amlumeUsernamePwdAuthenticationProvider,
                                                          OidcAuthorizationCodeAuthenticationProvider oidcAuthenticationProvider,
                                                          OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider
    ) throws Exception {

        // Theoretically Spring Security's WebAuthnConfigurer automatically makes its necessary endpoints (e.g., /webauthn/register/options, /webauthn/register, /webauthn/authenticate/options, /webauthn/authenticate) accessible.

        // One way to potentially mitigate the "unchecked" warning is to assign the configurer to a typed variable first.
//        WebAuthnConfigurer<HttpSecurity> webAuthnConfigurer = new WebAuthnConfigurer<>();

        http.authorizeHttpRequests(
                authorize -> authorize
                        .requestMatchers(
                                // Static resources

                                PathPatternRequestMatcher.withDefaults().matcher("/images/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/line-awesome/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/webjars/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/css/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/js/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/img/**"),
//                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/**"),
                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/favicon.ico"),
                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/favicon-96x96.png"),
                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/favicon.svg"),
                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/apple-touch-icon.png"),
                                PathPatternRequestMatcher.withDefaults().matcher("/favicons/site.webmanifest"),



                                // Public pages and essential endpoints
                                PathPatternRequestMatcher.withDefaults().matcher("/"),
                                PathPatternRequestMatcher.withDefaults().matcher("/login"), // Allows GET for login page
                                PathPatternRequestMatcher.withDefaults().matcher("/error"),

                                // --- WebAuthn Endpoints (MUST BE PERMITTED FOR ANONYMOUS ACCESS TO START LOGIN/REGISTRATION) ---
                                // Spring Security's default WebAuthn endpoints for login
//                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/authenticate/options"), // Passkey login options
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/webauthn/authenticate/options"), // Client's options URL for login
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/authenticate"),      // Passkey login verification. Client's assertion URL
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/options"),      // Passkey login verification. Client's assertion URL
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn"),      // Passkey login verification. Client's assertion URL

                                // Spring Security's default WebAuthn endpoints for registration (if you use them directly from a public page)
                                // If registration is only for authenticated users (via /api/profile/passkeys), these might not need to be public here.
                                // For now, let's assume they might be used publicly or by the default registration page.
//                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/register/options"),
//                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/register"),
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/webauthn/register/options"), // For profile page registration
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/webauthn/register"), // For profile page registration


                                // Your API registration endpoint
                                PathPatternRequestMatcher.withDefaults().matcher("/api/register/**")
                        ).permitAll()

                        // Secure other application paths
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/dashboard")).authenticated()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/profile/passkeys/**")).authenticated()
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/profile/**")).authenticated()
                        .anyRequest().authenticated()
        );

        // --- AuthenticationManager for WebAuthn & Form Login ---
        // This ProviderManager will be used by our manually configured WebAuthnAuthenticationFilter.
        // It includes the WebAuthn provider and your form login provider.
        ProviderManager mainAuthenticationManager = new ProviderManager(
                this.webAuthnAuthenticationProvider,
                amlumeUsernamePwdAuthenticationProvider,
                oidcAuthenticationProvider,       // ADD OIDC PROVIDER
                oauth2LoginAuthenticationProvider // ADD OAUTH2 PROVIDER
        );

        // Set this as the shared AuthenticationManager for HttpSecurity
        // This is important so filters like UsernamePasswordAuthenticationFilter and OAuth2LoginAuthenticationFilter
        // use this manager, and also so the manually added WebAuthn filters can get it.
        http.authenticationManager(mainAuthenticationManager);

        // --- Custom Success Handler for WebAuthn ---
        AuthenticationSuccessHandler webAuthnSuccessHandler =
                new WebAuthnPrincipalSettingSuccessHandler(userDetailsService, "/dashboard");

        // --- Manually Create and Configure WebAuthn Login Filters ---

        // 1. PublicKeyCredentialRequestOptionsFilter (for generating authentication options)
        // This filter, by default, processes POST requests to "/login/webauthn/options".
        // Your client-side JS in login.jte needs to call this URL.
        PublicKeyCredentialRequestOptionsFilter requestOptionsFilter =
                new PublicKeyCredentialRequestOptionsFilter(this.relyingPartyOperations); // Takes RelyingPartyOperations
        // No setFilterProcessesUrl method. It uses its default or one set by WebAuthnConfigurer.
        // We are not using WebAuthnConfigurer here.
        log.info("Configured PublicKeyCredentialRequestOptionsFilter (default processes: POST /login/webauthn/options)");

        // 2. WebAuthnAuthenticationFilter (for processing authentication assertion)
        WebAuthnAuthenticationFilter webAuthnAuthenticationFilter = new WebAuthnAuthenticationFilter(); // Use no-arg constructor
        webAuthnAuthenticationFilter.setAuthenticationManager(mainAuthenticationManager);
        PublicKeyCredentialRequestOptionsRepository requestOptRepo = new HttpSessionPublicKeyCredentialRequestOptionsRepository();
        webAuthnAuthenticationFilter.setRequestOptionsRepository(requestOptRepo);
        webAuthnAuthenticationFilter.setAuthenticationSuccessHandler(webAuthnSuccessHandler);
        // Default processing URL is POST /login/webauthn. This matches your client JS.
        log.info("Configured WebAuthnAuthenticationFilter to process: POST /login/webauthn");

        // --- Registration filters are omitted as registration is handled by PasskeyController ---


        log.info("Manually configuring WebAuthn login filters with custom success handler.");

        http
                .userDetailsService(userDetailsService)
                // In LocalSecurityConfig.java, inside defaultSecurityFilterChain method
// UserDetailsService userDetailsService is already injected as a parameter to this method.

//                .webAuthn(webauthn -> webauthn
//                        .userDetailsService(userDetailsService) // Explicitly pass the injected UserDetailsService
                // You might also need to explicitly set relyingPartyOperations if it's not picked up:
//                 .relyingPartyOperations(relyingPartyOperations(userEntities, userCredentials, webAuthNProperties)) // Assuming these are available or injectable
//        )
//                .webAuthn(webauthn -> { // Explicitly configure WebAuthn
//                    webauthn.userDetailsService(userDetailsService); // Pass the injected UserDetailsService
//                    webauthn.relyingPartyOperations(relyingPartyOperations); // Pass the injected RelyingPartyOperations
//                    // We can add other WebAuthn customizer settings here if needed
//                })

                // Remove .webAuthn() DSL as we are adding filters manually
                // .removeConfigurer(WebAuthnConfigurer.class) // More specific way to remove if needed

                // Add WebAuthn filters manually in the correct order
                // These are typically added around where form login or other auth filters are.
                // Adding after UsernamePasswordAuthenticationFilter is a common placement.
                .addFilterAfter(requestOptionsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(webAuthnAuthenticationFilter, PublicKeyCredentialRequestOptionsFilter.class)

                .formLogin(formLogin -> formLogin
                                .loginPage("/login") // Specify your custom login page
                                .loginProcessingUrl("/login")
                                .permitAll() // Allow access to the login page path
//                                .successHandler((request, response, auth) -> response.setStatus(200))
//                                .failureHandler((request, response, ex) -> response.setStatus(401))
                                // --- Explicitly configure success and failure URLs ---
                                // Redirect to a simple success page or the root on success
//                                .defaultSuccessUrl("/login?form_login_success=true", true) // Redirect to root after successful form login
                                .defaultSuccessUrl("/dashboard", true)
//                        .defaultSuccessUrl("/", true) // Redirect to root after successful form login
                                // Redirect back to login page with error parameter on failure
                                .failureUrl("/login?error=true")
                )

                // --- Remember-Me Configuration ---
                .rememberMe(rememberMe -> rememberMe
                                .tokenRepository(persistentTokenRepository) // Use the bean
                                .userDetailsService(userDetailsService)     // Crucial for re-authenticating
                                .key(this.rememberMeKey) // Use a unique, secure key. Consider externalizing.
//                        .key(this.webAuthNProperties.getRpId() + "_RememberMeKey2024") // Use a unique, secure key. Consider externalizing.
                                .tokenValiditySeconds((int) Duration.ofDays(30).toSeconds()) // e.g., 30 days
                )
                .oauth2Login(oauth2Login -> oauth2Login
                                .loginPage("/login")
                                .userInfoEndpoint(userInfo -> userInfo
                                        .oidcUserService(this.oidcUserService)
                                        .userService(this.oauth2UserService)
                                )
                                // Redirect to login page with success param. The 'true' flag forces the redirect, ignoring any saved request
                                .defaultSuccessUrl("/dashboard", true)
//                        .defaultSuccessUrl("/login?oauth2_login_success=true", true)
//                                .failureUrl("/login?oauth2_error=true")
                )
                .logout(logout -> logout
//                                .logoutSuccessUrl("/")
                                .logoutSuccessUrl("/login?logout=true")
                                .permitAll()
                )

                .csrf(cfg -> cfg.ignoringRequestMatchers(
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn"),
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/options"),
                        PathPatternRequestMatcher.withDefaults().matcher("/login/webauthn/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/webauthn/**"),

                        // If using default registration filters, permit their URLs:
                        // PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/registration/options"),
                        // PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login/webauthn/registration"),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**") // If your API clients handle CSRF differently or are stateless
                ))
                // --- Exception Handling for Access Denied ---
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(customAccessDeniedHandler)
                );
        // For stateless APIs (token-based), CSRF might be less relevant for those specific endpoints.
        // Consider CSRF for /api/profile if it's used by a browser-based frontend with sessions.

        // No explicit matcher needed here as it's the fallback chain
        return http.build();
    }


    /**
     * Provides the OAuth2AccessTokenResponseClient used by OAuth2/OIDC authentication providers
     * to exchange authorization codes for access tokens.
     *
     * @return The configured OAuth2AccessTokenResponseClient.
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        return new RestClientAuthorizationCodeTokenResponseClient();
    }

    /**
     * Provides the OIDC Authorization Code Authentication Provider.
     * This provider handles the final step of the OIDC Authorization Code Flow,
     * exchanging the code for tokens and loading the OIDC user information.
     *
     * @param accessTokenResponseClient The client for exchanging authorization codes.
     * @return The configured OidcAuthorizationCodeAuthenticationProvider.
     */
    @Bean
    public OidcAuthorizationCodeAuthenticationProvider oidcAuthenticationProvider(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient
            // Note: oidcUserService and authorizedClientService are injected via the constructor
            // and are available as fields (this.oidcUserService, this.authorizedClientService).
            // oidcUserService and authorizedClientService are available as fields
    ) {
        return new OidcAuthorizationCodeAuthenticationProvider(
                accessTokenResponseClient,
                this.oidcUserService
        );
    }

    @Bean
    /**
     * Provides the OAuth2 Login Authentication Provider.
     * This provider handles the final step of the OAuth2 Authorization Code Flow,
     * exchanging the code for tokens and loading the OAuth2 user information.
     *
     * @param accessTokenResponseClient The client for exchanging authorization codes.
     * @return The configured OAuth2LoginAuthenticationProvider.
     */
    public OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient
    ) {
        // Note: oauth2UserService is injected via the constructor and is available as a field (this.oauth2UserService).
        return new OAuth2LoginAuthenticationProvider(
                accessTokenResponseClient,
                this.oauth2UserService
        );
    }


    /**
     * Seeds initial OAuth2 registered clients and default user authorities into the database
     * if they do not already exist. This is primarily for local development setup.
     */
    @PostConstruct
    @Transactional // Ensure seeding is atomic
    public void seedInitialOAuth2Clients() {
        // Check if clients already exist to avoid duplicates on every startup
        if (this.jpaRegisteredClientRepositoryAdapter.findByClientId("amlumeapi") == null) {
            log.info("Seeding initial OAuth2 registered clients into the database...");

            String defaultRedirectBase = this.webAuthNProperties.getAllowedOrigins().stream()
                    .filter(o -> o.contains("localhost:9000") || o.contains("authserver")) // Prefer authserver's own origin for its test clients
                    .findFirst()
                    .orElse("http://localhost:9000"); // Fallback to authserver's typical local port

            /*
             * •Purpose: Machine-to-Machine (M2M) communication.
             * •Grant Type: client_credentials
             * •Client Authentication: client_secret_basic
             * •Token Format: SELF_CONTAINED (JWT)
             * •Safety:
             * This is a standard and secure configuration for an M2M client that can keep its secret.
             * The secret (amlumeapiClientSecret) should be strong and stored securely (Vault is good).
             */
            RegisteredClient clientCredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeapi")
                    .clientIdIssuedAt(Instant.now())
                    .clientSecret(this.amlumeapiClientSecret)  // USE VAULT-INJECTED SECRET
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scopes(scopeConfig -> scopeConfig.addAll(List.of(OidcScopes.OPENID, "ADMIN", "USER")))
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            /*
             * Client: introspectClient (amlumeintrospect)
             * ----------------------------------------------------------------------------------------------------
             * • Purpose: Machine-to-Machine (M2M) client specifically designed to receive an OPAQUE (REFERENCE)
             *   access token. This type of token is not self-contained and requires resource servers that
             *   consume it to call back to this authorization server's introspection endpoint
             *   (e.g., /oauth2/introspect) for validation and to retrieve token details.
             *
             * • Grant Type: client_credentials
             *   Standard for M2M communication where the client authenticates itself directly.
             *
             * • Client Authentication: client_secret_basic
             *   The client authenticates using its clientId and clientSecret via an HTTP Basic auth header.
             *
             * • Token Format Received by this Client: REFERENCE (Opaque)
             *   The access token issued to this client will be an opaque string.
             *
             * • Safety & Considerations:
             *   - Client-Side: The configuration of this client itself (as a confidential client using
             *     client_secret_basic) is standard. The security of its `clientSecret` is paramount and
             *     should be managed securely (e.g., via Vault).
             *   - Ecosystem Impact (Resource Intensity): The primary concern with opaque tokens is the
             *     impact on resource servers and the authorization server itself. Resource servers
             *     must make a network call to the introspection endpoint for every request that uses
             *     an opaque token. This can:
             *       - Increase latency for API requests.
             *       - Increase the load on the authorization server (authserver).
             *       - Create a tight coupling between resource servers and the authorization server.
             *     Self-contained JWTs, on the other hand, can be validated locally by resource servers
             *     once they have the public key, reducing these dependencies and overheads.
             *
             * • Recommendation (Considering the preference to avoid resource-intensive opaque tokens):
             *   - KEEP THIS CLIENT IF:
             *     1. You specifically need to test the introspection endpoint functionality of this `authserver`.
             *     2. You have existing or planned resource servers that are *only* designed to work with
             *        opaque tokens and perform introspection.
             *     3. You have a security policy that mandates opaque tokens for certain types of clients or resources
             *        despite the performance trade-offs (e.g., for immediate revocation needs that JWTs handle differently).
             *
             *   - CONSIDER REMOVING OR COMMENTING IT OUT IF:
             *     1. Your primary architectural goal is to use self-contained JWTs for your resource servers
             *        (like `amlume-shop`) to minimize resource intensity and reduce calls to `authserver`.
             *        In this scenario, a client that *only* receives opaque tokens might be of limited practical
             *        use for your main application flows.
             *     2. You want to actively discourage the adoption of opaque tokens within your ecosystem to
             *        maintain performance and reduce the load on `authserver`'s introspection endpoint.
             *     3. The overhead of maintaining and testing flows involving opaque tokens is not justified
             *        by a clear business or technical requirement for them.
             *
             *   If you are aiming for a JWT-based architecture for your resource servers, this client
             *   might primarily serve as a test case for the introspection feature rather than a client
             *   used by typical services in your ecosystem.
             */
            RegisteredClient introspectClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeintrospect")
                    .clientIdIssuedAt(Instant.now())
                    .clientSecret(this.amlumeintrospectClientSecret) // USE VAULT-INJECTED SECRET
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scopes(scopeConfig -> scopeConfig.add(OidcScopes.OPENID))
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .accessTokenFormat(OAuth2TokenFormat.REFERENCE).build()).build();

            /*
             * •Purpose: Testing user-interactive flows, likely with Postman.
             * •Grant Types: authorization_code, refresh_token
             * •Client Authentication: client_secret_post, client_secret_basic (Confidential Client)
             * •Token Format: SELF_CONTAINED (JWT)
             * •PKCE: Not explicitly required (it's a confidential client).•Redirect URIs: https://oauth.pstmn.io/v1/callback and a custom one.
             * •Safety: Good for a confidential client. The secret (postmanClientSecret) needs to be managed.
             * •Recommendation:
             *  •Keep as is for testing confidential client flows.
             *  •Consider adding PKCE: While not mandatory for confidential clients, enabling PKCE (.clientSettings(ClientSettings.builder().requireProofKey(true).build())) adds an extra layer of security against authorization code interception, even for confidential clients. This is a good hardening step.
             */
            // Client for Postman or general testing
            RegisteredClient postmanAuthCodeClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("postman-client") // DISTINCT CLIENT ID
                    .clientIdIssuedAt(Instant.now())
                    .clientSecret(this.postmanClientSecret) // USE VAULT-INJECTED SECRET
                    .clientAuthenticationMethods(methods -> methods.addAll(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST, ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://oauth.pstmn.io/v1/callback") // Standard Postman callback
                    .redirectUri(defaultRedirectBase + "/login/oauth2/code/custom-postman") // A specific callback for this client
                    .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .refreshTokenTimeToLive(Duration.ofHours(8)).reuseRefreshTokens(false)
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            /*
             * Purpose: Represents a public client (e.g., SPA, mobile app) that cannot securely store a client secret.
             * •Grant Types: authorization_code, refresh_token
             * •Client Authentication: none (Public Client)
             * •PKCE: clientSettings(ClientSettings.builder().requireProofKey(true).build()) - Excellent! This is critical for public clients.
             * •Token Format: SELF_CONTAINED (JWT)
             * •Safety: This is the safest configuration for a public client using the Authorization Code grant.
             * •Recommendation: Keep as is and use this pattern for any actual public clients.
             */
            RegisteredClient pkceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeapipublicclient")
                    .clientIdIssuedAt(Instant.now())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://oauth.pstmn.io/v1/callback")
                    .redirectUri(defaultRedirectBase + "/login/oauth2/code/custom-public-pkce")
                    .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder().requireProofKey(true).build()) // Enforce PKCE
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .refreshTokenTimeToLive(Duration.ofHours(8)).reuseRefreshTokens(false)
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            /*
             * •Purpose: This is the client registration for your amlume-shop application itself, which will act as an OAuth2 client to authserver.
             * •Grant Types: authorization_code, refresh_token
             * •Client Authentication: client_secret_post, client_secret_basic (Confidential Client, as amlume-shop is a backend server)
             * •Token Format: SELF_CONTAINED (JWT)•PKCE: Not explicitly required.
             * •Redirect URI: Custom for amlume-shop (http://localhost:9000/login/oauth2/code/amlumeclient - note port 9000, this should be the redirect URI registered with authserver and where amlume-shop expects the callback, typically amlume-shop's own address like http://localhost:8080/...).
             *  •Important: The redirectUri for shopClient in authserver's configuration must point to amlume-shop's actual callback endpoint (e.g., http://localhost:8080/login/oauth2/code/amlumeclient if amlume-shop runs on 8080). The current defaultRedirectBase in your seeding logic seems to point to authserver's own base URL.
             * •Safety: Good for a confidential client. The secret (shopClientSecret) must be securely managed by amlume-shop (e.g., loaded from its own Vault instance or environment variables).
             * •Recommendation:
             *  •Correct the redirectUri: Ensure it points to amlume-shop's callback endpoint.
             *  •Highly Recommended: Add PKCE: Even though amlume-shop is a confidential client, adding PKCE (.clientSettings(ClientSettings.builder().requireProofKey(true).build())) provides robust protection against authorization code interception attacks. This is a best practice.
             */
            RegisteredClient shopClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeclient") // This is the ID amlume-shop will use
                    .clientIdIssuedAt(Instant.now())
                    .clientSecret(this.shopClientSecret) // USE VAULT-INJECTED SECRET
                    .clientAuthenticationMethods(methods -> methods.addAll(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST, ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    // IMPORTANT: This redirect URI MUST match what amlume-shop configures
                    .redirectUri("http://localhost:8080/login/oauth2/code/amlumeclient")
//                    .redirectUri(defaultRedirectBase + "/login/oauth2/code/amlumeclient")
                    .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder().requireProofKey(true).build()) // ADDED PKCE
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(10))
                            .refreshTokenTimeToLive(Duration.ofHours(8))
                            .reuseRefreshTokens(false) // Good: enables refresh token rotation
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                            .build()).build();


            this.jpaRegisteredClientRepositoryAdapter.save(clientCredClient);
            this.jpaRegisteredClientRepositoryAdapter.save(introspectClient);
            this.jpaRegisteredClientRepositoryAdapter.save(postmanAuthCodeClient);
            this.jpaRegisteredClientRepositoryAdapter.save(pkceClient);
            this.jpaRegisteredClientRepositoryAdapter.save(shopClient);
            log.info("Finished seeding OAuth2 clients using secrets from Vault.");
//            log.info("Finished seeding OAuth2 clients.");
        } else {
            log.info("OAuth2 registered clients already seem to exist. Skipping seeding.");
        }

        AuthorityRepository authorityRepository = this.jpaRegisteredClientRepositoryAdapter.getAuthorityRepository();
        if (authorityRepository.findByAuthority("ROLE_USER").isEmpty()) {
            authorityRepository.save(new Authority("ROLE_USER"));
            log.info("Seeded default ROLE_USER authority.");
        }
        if (authorityRepository.findByAuthority("ROLE_ADMIN").isEmpty()) {
            authorityRepository.save(new Authority("ROLE_ADMIN"));
            log.info("Seeded default ROLE_ADMIN authority.");
        }
        if (authorityRepository.findByAuthority("ROLE_SUPER_ADMIN").isEmpty()) {
            authorityRepository.save(new Authority("ROLE_SUPER_ADMIN"));
            log.info("Seeded default ROLE_SUPER_ADMIN authority.");
        }
        if (authorityRepository.findByAuthority("ROLE_ROOT").isEmpty()) {
            authorityRepository.save(new Authority("ROLE_ROOT"));
            log.info("Seeded default ROLE_ROOT authority.");
        }

        // Seed initial root user

        UserRepository userRepository = this.jpaRegisteredClientRepositoryAdapter.getUserRepository();

        String rootEmail = this.initialRootUserEmail; // Or from config
        String rootPassword = this.initialRootUserPassword; // LOAD SECURELY, e.g., from Vault/Env for the seeder

        if (userRepository.findByEmail_Value(rootEmail).isEmpty()) {
            User rootUser = User.builder()
                    .firstName("Root")
                    .lastName("Admin")
                    .email(new EmailAddress(rootEmail))
                    .password(new HashedPassword(passwordEncoder.encode(rootPassword)))
                    .externalId(User.generateWebAuthnUserHandle())
                    .build();
            rootUser.enableAccount(); // Ensure account is enabled

            authorityRepository.findByAuthority("ROLE_ROOT").ifPresent(rootUser::assignAuthority);
            // Optionally assign ROLE_ADMIN, ROLE_SUPER_ADMIN as well if your hierarchy doesn't imply it
            authorityRepository.findByAuthority("ROLE_SUPER_ADMIN").ifPresent(rootUser::assignAuthority);
            authorityRepository.findByAuthority("ROLE_ADMIN").ifPresent(rootUser::assignAuthority);

            userRepository.save(rootUser);
            log.info("***************************************************************************");
            log.info("ROOT USER SEEDED: {} / (password in config/env)", rootEmail);
            log.info("***************************************************************************");
        } else {
            log.info("Root user {} already exists. Skipping seeding.", rootEmail);
        }
    }

    /**
     * Generates an RSA key pair and wraps it in a JWKSource for signing JWTs.
     *
     * @return The JWKSource containing the RSA key.
     */
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

    /**
     * Generates an RSA key pair.
     *
     * @return The generated KeyPair.
     */
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

    /**
     * Provides a JwtDecoder for decoding JWTs issued by this authorization server.
     *
     * @param jwkSource The JWKSource containing the signing key.
     * @return The configured JwtDecoder.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Provides the AuthorizationServerSettings bean, configuring the authorization server's metadata endpoints.
     *
     * @return The configured AuthorizationServerSettings.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * Configures CORS for the application.
     */
    // --- CORS ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("http://127.0.0.1:8080"); // amlume-shop
        config.addAllowedOrigin("http://localhost:8080"); // amlume-shop
        config.addAllowedOrigin("http://localhost:9000"); // authserver itself for some flows
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // --- OAuth2TokenCustomizer ---

    /**
     * Customizes the claims included in the issued JWT access tokens.
     * Delegates the customization logic to {@link JwtCustomizationService}.
     *
     * @return The OAuth2TokenCustomizer for JWT tokens.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return jwtCustomizationService::customizeToken;
    }

    /**
     * Populates JWT claims with user profile information.
     *
     * @param claims  The map of claims to populate.
     * @param appUser The User object containing the profile information.
     */
    private void putClaims(Map<String, Object> claims, User appUser) {
        claims.put("user_id_numeric", appUser.getId());
        claims.put("given_name", appUser.getFirstName());
        claims.put("family_name", appUser.getLastName());
        claims.put("full_name", appUser.getDisplayableFullName());
        claims.put("nickname", appUser.getNickname());
        claims.put("email", appUser.getEmail().getValue());
    }

    /*
     * @Deprecated
     * Registers Jackson modules required for WebAuthn serialization/deserialization
     * with the application's ObjectMapper.
     * This is crucial for handling WebAuthn-related JSON data correctly.
     * <p>
     * You need to create the persistent_logins table in your MySQL database. If you didn't use setCreateTableOnStartup(true), you must create it manually or via a migration script.
     * <p>
     * CREATE TABLE persistent_logins (
     * username VARCHAR(255) NOT NULL, -- Ensure this matches the length of your User's email/username
     * series VARCHAR(64) PRIMARY KEY,
     * token VARCHAR(64) NOT NULL,
     * last_used TIMESTAMP NOT NULL
     * );
     * <p>
     * Note: username column length should be sufficient for your usernames (emails). VARCHAR(255) is usually safe for emails.
     */
//    @PostConstruct
//    public void registerWebAuthnJacksonModules() {
//        log.info("LocalSecurityConfig: Operating on ObjectMapper instance: {} (Hash: {})",
//                this.objectMapper, System.identityHashCode(this.objectMapper));
//        log.info("LocalSecurityConfig - Registered module IDs BEFORE (in LocalSecurityConfig's PostConstruct): {}",
//                this.objectMapper.getRegisteredModuleIds());
//
//        // All module registration is now centralized in JacksonConfig.java.
//        // This method is likely operating on the @Primary ObjectMapper injected by Spring.
//        // Re-registering modules here is redundant if JacksonConfig.java is comprehensive.
//        // It's safer to remove these lines to ensure a single point of configuration.
//
//        // ObjectConverter objectConverter = new ObjectConverter();
//        // this.objectMapper.registerModule(new WebAuthnJSONModule(objectConverter));
//        // log.info("Registered WebAuthnJSONModule (from webauthn4j) via LocalSecurityConfig.");
//
//        // this.objectMapper.registerModule(new WebauthnJackson2Module());
//        // log.info("Registered WebauthnJackson2Module (from Spring Security) via LocalSecurityConfig.");
//
//        // ClassLoader classLoader = LocalSecurityConfig.class.getClassLoader();
//        // List<com.fasterxml.jackson.databind.Module> securityModules = org.springframework.security.jackson2.SecurityJackson2Modules.getModules(classLoader);
//        // this.objectMapper.registerModules(securityModules);
//        // log.info("Registered core Spring Security Jackson modules via LocalSecurityConfig.");
//
//        log.info("LocalSecurityConfig - Registered module IDs AFTER (in LocalSecurityConfig's PostConstruct): {}",
//                this.objectMapper.getRegisteredModuleIds());
//        log.warn("Review: Jackson module registration in LocalSecurityConfig @PostConstruct might be redundant if JacksonConfig.java provides the @Primary ObjectMapper.");
//    }

}

/*
 * What we Should Do (Recommendations for Safest Configurations):
 *
 * 1. For Public Clients (e.g., SPAs, Mobile Apps that will talk to authserver):
 * • Use the pattern of pkceClient (amlumeapipublicclient).
 * • clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
 * • authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
 * • authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN) (if refresh tokens are needed)
 * • clientSettings(ClientSettings.builder().requireProofKey(true).build()) (Mandatory PKCE)
 * • Specific, HTTPS redirect URIs.
 * • Self-contained JWTs are fine.
 *
 * 2. For Confidential Clients (like your amlume-shop backend, or other backend services):
 * • Use the pattern of shopClient (amlumeclient) or postmanAuthCodeClient.
 * • clientAuthenticationMethods(methods -> methods.addAll(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST, ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
 * • authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
 * • authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
 * • Highly Recommended: Add PKCE: .clientSettings(ClientSettings.builder().requireProofKey(true).build())
 * • Securely manage the clientSecret.
 * • Specific, HTTPS redirect URIs.
 * • Self-contained JWTs are fine.
 *
 * 3. For Machine-to-Machine (M2M) Communication:
 * • Use the pattern of clientCredClient (amlumeapi).
 * • clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
 * • authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
 * • Securely manage the clientSecret.
 * • Self-contained JWTs are fine.
 *
 * Specific Actions for LocalSecurityConfig.java:
 *
 * • shopClient (for amlume-shop):
 * • Verify and Correct redirectUri:
 * Change: .redirectUri(defaultRedirectBase + "/login/oauth2/code/amlumeclient")
 * To something like (assuming amlume-shop runs on 8080 locally):
 * .redirectUri("http://localhost:8080/login/oauth2/code/amlumeclient")
 * (This URI must match exactly what amlume-shop is configured to use in its
 * spring.security.oauth2.client.registration.amlumeclient.redirect-uri property).
 * • Add PKCE: Add this line to the shopClient configuration:
 * .clientSettings(ClientSettings.builder().requireProofKey(true).build())
 *
 * • postmanAuthCodeClient (for testing):
 * • Consider Adding PKCE (Optional but good practice): Add this line:
 * .clientSettings(ClientSettings.builder().requireProofKey(true).build())
 */

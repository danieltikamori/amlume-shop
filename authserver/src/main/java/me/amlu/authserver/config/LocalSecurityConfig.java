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
import me.amlu.authserver.oauth2.service.JpaUserDetailsService;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.passkey.repository.DbUserCredentialRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;
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
import java.util.*;
import java.util.stream.Collectors;

@Profile("!prod")
@Configuration
@EnableConfigurationProperties(LocalSecurityConfig.WebAuthNProperties.class) // Enable your properties class
@EnableWebSecurity
public class LocalSecurityConfig {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LocalSecurityConfig.class);

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

    private final PersistentTokenRepository persistentTokenRepository;

    private final ObjectMapper objectMapper;
    private final JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter;
    private final WebAuthNProperties webAuthNProperties;
    private final DataSource dataSource;

    public LocalSecurityConfig(ObjectMapper objectMapper,
                               JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter,
                               WebAuthNProperties webAuthNProperties, PersistentTokenRepository persistentTokenRepository, DataSource dataSource) {
        this.objectMapper = objectMapper;
        this.jpaRegisteredClientRepositoryAdapter = jpaRegisteredClientRepositoryAdapter;
        this.webAuthNProperties = webAuthNProperties;
        this.persistentTokenRepository = persistentTokenRepository;
        this.dataSource = dataSource;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        // --- Use the recommended 'http.with()' approach ---
        http.with(new OAuth2AuthorizationServerConfigurer(), Customizer.withDefaults());

        // Get the RequestMatcher for the authorization server endpoints.
        // This is used to ensure this SecurityFilterChain only applies to these specific endpoints.
        RequestMatcher authorizationServerEndpointsMatcher =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).getEndpointsMatcher();

        http
                .securityMatcher(authorizationServerEndpointsMatcher) // CRITICAL: Restrict this chain to AS endpoints
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          // WebAuthNProperties webAuthNProperties, // Already a field
                                                          // UserCredentialRepository userCredentialRepository, // Already a field or bean
                                                          // PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository, // Already a field or bean
                                                          OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, OidcUser> oidcUserService,
                                                          OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
                                                          UserDetailsService userDetailsService, // Injected for remember-me
                                                          PersistentTokenRepository persistentTokenRepository // Injected for remember-me
    ) throws Exception {

        // Theoretically Spring Security's WebAuthnConfigurer automatically makes its necessary endpoints (e.g., /webauthn/register/options, /webauthn/register, /webauthn/authenticate/options, /webauthn/authenticate) accessible.

        // One way to potentially mitigate the "unchecked" warning is to assign the configurer to a typed variable first.
//        WebAuthnConfigurer<HttpSecurity> webAuthnConfigurer = new WebAuthnConfigurer<>();

        http.authorizeHttpRequests(
                authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll()
                        .requestMatchers("/login/**", "/webauthn/**", "/error").permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/profile/passkeys/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/profile/**")).authenticated() // Secure profile endpoints
                        .requestMatchers(new AntPathRequestMatcher("/api/register/**")).permitAll() // Allow registration
                        .anyRequest().authenticated()
        );

        http
                .webAuthn(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login") // Specify your custom login page if you have one
                        .permitAll() // Allow access to the login page
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
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)
                                .userService(oauth2UserService)
                        )
                );

        http.csrf(cfg -> cfg.ignoringRequestMatchers(
                new AntPathRequestMatcher("/webauthn/**"),
                new AntPathRequestMatcher("/login/webauthn"),
                new AntPathRequestMatcher("/api/**") // If your API clients handle CSRF differently or are stateless
        ));
        // If your API is stateful and uses cookies, CSRF protection is important.
        // For stateless APIs (token-based), CSRF might be less relevant for those specific endpoints.
        // Consider CSRF for /api/profile if it's used by a browser-based frontend with sessions.

        // No explicit matcher needed here as it's the fallback chain
        return http.build();
    }

    // The JpaRegisteredClientRepositoryAdapter bean (defined as @Service)
    // will be automatically picked up by Spring Authorization Server.
    // No explicit InMemoryRegisteredClientRepository bean for client definitions is needed here.

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
        if (authorityRepository.findByAuthority("ROLE_ROOR").isEmpty()) {
            authorityRepository.save(new Authority("ROLE_ROOT"));
            log.info("Seeded default ROLE_ROOT authority.");
        }

    }

    // @Bean (if not already implicitly configured by Spring Boot)
    // public UserDetailsService userDetailsService(UserRepository userRepository) {
    //     return new JpaUserDetailsService(userRepository);
    // }
    // Spring Boot typically auto-configures UserDetailsService if there's one PasswordEncoder and one UserDetailsService bean.
    // If you have multiple, you might need to specify it in HttpSecurity:
    // http.userDetailsService(myUserDetailsService)

    /**
     * @param userRepository {@link UserRepository}
     * @return {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new JpaUserDetailsService(userRepository);
    }


    // TODO: change to ECPrivateKey
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

    // --- CORS ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("http://127.0.0.1:8080"); // Consider making this more configurable
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // --- OAuth2TokenCustomizer ---
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserRepository userRepository) {
        return (context) -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claims((claims) -> {
                    // String principalName = context.getPrincipal().getName();
                    if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                        Set<String> roles = context.getRegisteredClient().getScopes();
                        claims.put("roles", roles);
                    } else { // For user-based grants
                        //  When you are creating JWT claims,
                        //  you are explicitly removing the ROLE_ prefix.
                        //  This means if a user has the authority "ROLE_ADMIN",
                        //  the JWT roles claim will contain "ADMIN".
                        //  This is a common practice to make the claims cleaner.
                        Set<String> roles = AuthorityUtils.authorityListToSet(context.getPrincipal().getAuthorities())
                                .stream()
                                .map(r -> r.replaceFirst("^ROLE_", ""))
                                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                        claims.put("roles", roles);

                        // Add user_id and full_name if the principal is a UserDetails representing your User
                        Object principal = context.getPrincipal().getPrincipal(); // Get the actual principal object
                        if (principal instanceof User appUser) { // Check if it's your User class
                            putClaims(claims, appUser);
                        } else if (context.getPrincipal().getName() != null) {
                            // Fallback for other UserDetails implementations or if principal is just username string
                            userRepository.findByEmail_Value(context.getPrincipal().getName()).ifPresent(user -> {
                                putClaims(claims, user);
                            });
                        }
                    }
                });
            }
        };
    }

    /**
     * Put claims into the JWT claims.
     *
     * @param claims  {@link Map}
     * @param appUser {@link User}
     */
    private void putClaims(Map<String, Object> claims, User appUser) {
        claims.put("user_id_numeric", appUser.getId());
        claims.put("given_name", appUser.getFirstName());
        claims.put("family_name", appUser.getLastName());
        claims.put("full_name", appUser.getDisplayableFullName());
        claims.put("nickname", appUser.getNickname());
        claims.put("email", appUser.getEmail().getValue());
    }

    // --- Passkey/WebAuthn Beans (Spring Security Native) ---

    /**
     * In Spring Security, the “relaying party parameter” is defined by defining a bean for WebAuthnRelyingPartyOperations.
     * You’ll need an ID (which is essentially your domain), name, and allowed origin (URL).
     *
     * @param userEntities    {@link PublicKeyCredentialUserEntityRepository}
     * @param userCredentials {@link UserCredentialRepository}
     * @param webauthnId      is the domain
     * @param webauthnName    is the application name
     * @param webauthnOrigin  is the origin URL
     * @return {@link WebAuthnRelyingPartyOperations}
     */
    @Bean
    public WebAuthnRelyingPartyOperations relyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntities,
            UserCredentialRepository userCredentials,
            @Value("${spring.security.webauthn.rpId}") String webauthnId,
            @Value("${spring.security.webauthn.rpName}") String webauthnName,
            @Value("${spring.security.webauthn.allowedOrigins}") String webauthnOrigin) {
        return new Webauthn4JRelyingPartyOperations(userEntities, userCredentials,
                PublicKeyCredentialRpEntity.builder()
                        .id(webauthnId)
                        .name(webauthnName).build(),
                Set.of(webauthnOrigin));
    }

    @Bean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(
            UserRepository userRepository, AuthorityRepository authorityRepository) {
        return new DbPublicKeyCredentialUserEntityRepository(userRepository, authorityRepository);
    }

    @Bean
    public UserCredentialRepository userCredentialRepository(
            PasskeyCredentialRepository passkeyCredentialRepository, UserRepository userRepository) {
        return new DbUserCredentialRepository(passkeyCredentialRepository, userRepository);
    }

    /**
     * Remember me feature
     * <p>
     * Step 1 and 2:
     * Key Changes in LocalSecurityConfig.java:
     * <p>
     * 1. DataSource Injection:
     * - DataSource is injected into the constructor and stored as a field.
     * <p>
     * 2. defaultSecurityFilterChain Parameters:
     * - Added UserDetailsService userDetailsService and PersistentTokenRepository persistentTokenRepository
     * as parameters to be injected by Spring .
     * <p>
     * 3. .rememberMe() Configuration:
     * - tokenRepository(persistentTokenRepository):
     * - Tells Spring Security to use our database-backed token repository.
     * - userDetailsService(userDetailsService):
     * - Specifies the service to load user details when a remember-me token is validated.
     * - This is your existing JpaUserDetailsService.
     * - key("yourSuperSecretAndUniqueRememberMeKey"):
     * - A private key used to hash the contents of the remember-me cookie.
     * - Change this to a strong, unique, random string.
     * - It's good practice to externalize this to your application.properties or a secrets manager.
     * - I've used this.webAuthNProperties.getRpId() + "_RememberMeKey2024" as a placeholder;
     * generate a proper secret.
     * - tokenValiditySeconds((int) Duration.ofDays(30).toSeconds()):
     * - Sets how long the remember-me token is valid (e.g., 30 days).
     * <p>
     * 4. PersistentTokenRepository Bean:
     * - A bean named persistentTokenRepository of type PersistentTokenRepository is defined.
     * - It instantiates JdbcTokenRepositoryImpl and sets the dataSource.
     * - The line tokenRepository.setCreateTableOnStartup(true); is commented out.
     * - It's useful for the very first run in a development environment to create the persistent_logins table automatically.
     * - For subsequent runs or in production, this table should be managed by your database migration scripts
     * (e.g., Flyway, Liquibase).
     * <p>
     * 5. CSRF for /api/**:
     * - I've added /api/** to ignoringRequestMatchers for CSRF.
     * - This is a common setup if your API is primarily consumed by non-browser clients or SPAs that handle tokens differently.
     * - If your /api/profile endpoints are called from traditional web forms within your authserver's UI
     * and rely on session cookies, you might need more granular CSRF configuration.
     * - For now, this simplifies things.
     * <p>
     * 6. Login Page:
     * - Added .loginPage("/login").permitAll() to formLogin to explicitly state the login page URL,
     * assuming you have one or will create one.
     * - If you rely on Spring Security's default login page, this can be omitted, but a custom page is usually preferred.
     * <p>
     * Step 3: Database Schema for persistent_logins
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

    @ConfigurationProperties(prefix = "spring.security.webauthn")
    public static class WebAuthNProperties {
        private String rpId = "localhost"; // Default value
        private String rpName = "Amlume Passkeys"; // Default value - domain name of the application
        private Set<String> allowedOrigins = Collections.singleton("http://localhost:8080"); // Default value

        public WebAuthNProperties() {
        }

        public String getRpId() {
            return this.rpId;
        }

        public String getRpName() {
            return this.rpName;
        }

        public Set<String> getAllowedOrigins() {
            return this.allowedOrigins;
        }

        public void setRpId(String rpId) {
            this.rpId = rpId;
        }

        public void setRpName(String rpName) {
            this.rpName = rpName;
        }

        public void setAllowedOrigins(Set<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof WebAuthNProperties other)) return false;
            if (!other.canEqual(this)) return false;
            final Object this$rpId = this.getRpId();
            final Object other$rpId = other.getRpId();
            if (!Objects.equals(this$rpId, other$rpId)) return false;
            final Object this$rpName = this.getRpName();
            final Object other$rpName = other.getRpName();
            if (!Objects.equals(this$rpName, other$rpName)) return false;
            final Object this$allowedOrigins = this.getAllowedOrigins();
            final Object other$allowedOrigins = other.getAllowedOrigins();
            return Objects.equals(this$allowedOrigins, other$allowedOrigins);
        }

        protected boolean canEqual(final Object other) {
            return other instanceof WebAuthNProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $rpId = this.getRpId();
            result = result * PRIME + ($rpId == null ? 43 : $rpId.hashCode());
            final Object $rpName = this.getRpName();
            result = result * PRIME + ($rpName == null ? 43 : $rpName.hashCode());
            final Object $allowedOrigins = this.getAllowedOrigins();
            result = result * PRIME + ($allowedOrigins == null ? 43 : $allowedOrigins.hashCode());
            return result;
        }

        public String toString() {
            return "LocalSecurityConfig.WebAuthNProperties(rpId=" + this.getRpId() + ", rpName=" + this.getRpName() + ", allowedOrigins=" + this.getAllowedOrigins() + ")";
        }
    }

    @PostConstruct
    public void registerWebAuthnJacksonModules() {
        ObjectConverter objectConverter = new ObjectConverter();
        // Register the WebAuthnJSONModule with the Spring-managed ObjectMapper
        this.objectMapper.registerModule(new WebAuthnJSONModule(objectConverter));
        log.info("Registered WebAuthnJSONModule with Spring's ObjectMapper.");
    }

    // --- Social Login Beans (OAuth2 Client) ---
//    There are a @Service class that defines this bean. Therefore, this bean can be removed.
//    @Bean
//    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(
//            UserRepository userRepository, AuthorityRepository authorityRepository, WebClient.Builder webClient) {
//        return new CustomOAuth2UserService(userRepository, authorityRepository, webClient);
//    }

//    There are a @Service class that defines this bean. Therefore, this bean can be removed.
//    @Bean
//    public OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, OidcUser> customOidcUserService(
//            UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
//        return new CustomOidcUserService(userRepository, authorityRepository);
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

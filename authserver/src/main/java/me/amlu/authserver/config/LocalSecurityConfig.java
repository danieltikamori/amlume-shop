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
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.oauth2.service.CustomOAuth2UserService;
import me.amlu.authserver.oauth2.service.CustomOidcUserService;
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
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
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

    private final ObjectMapper objectMapper;
    private final JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter;
    private final WebAuthNProperties webAuthNProperties;

    public LocalSecurityConfig(ObjectMapper objectMapper,
                               JpaRegisteredClientRepositoryAdapter jpaRegisteredClientRepositoryAdapter,
                               WebAuthNProperties webAuthNProperties) {
        this.objectMapper = objectMapper;
        this.jpaRegisteredClientRepositoryAdapter = jpaRegisteredClientRepositoryAdapter;
        this.webAuthNProperties = webAuthNProperties;
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
                                                          WebAuthNProperties webAuthNProperties,
                                                          UserCredentialRepository userCredentialRepository,
                                                          PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository,
                                                          OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, OidcUser> oidcUserService,
                                                          OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService
    ) throws Exception {

        // Theoretically Spring Security's WebAuthnConfigurer automatically makes its necessary endpoints (e.g., /webauthn/register/options, /webauthn/register, /webauthn/authenticate/options, /webauthn/authenticate) accessible.

        // One way to potentially mitigate the "unchecked" warning is to assign the configurer to a typed variable first.
//        WebAuthnConfigurer<HttpSecurity> webAuthnConfigurer = new WebAuthnConfigurer<>();

        // Allow png images to be served without authentication
        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll());

        // TODO: implement password reset / account recovery mechanism

        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login/**", "/webauthn/**", "/error").permitAll() // Permit error page
                        .requestMatchers(new AntPathRequestMatcher("/api/profile/passkeys/**")).authenticated() // Passkeys
                        .anyRequest().authenticated()
                )
                .webAuthn(Customizer.withDefaults()) // This would use the bean by default

//                .webAuthn(webauth ->
//                        webauth.allowedOrigins(webAuthNProperties.getAllowedOrigins())
//                                .rpId(webAuthNProperties.getRpId())
//                                .rpName(webAuthNProperties.getRpName())
//                )
                .formLogin(Customizer.withDefaults())
                // Add OAuth2 Login for social providers
                .oauth2Login(oauth2Login -> oauth2Login
                                .userInfoEndpoint(userInfo -> userInfo
                                        .oidcUserService(oidcUserService) // For OIDC providers (e.g., Google)
                                        .userService(oauth2UserService)   // For generic OAuth2 providers (e.g., GitHub)
                                )
                        // .loginPage("/login") // If you have a custom login page listing social options
                        // .defaultSuccessUrl("/user-profile", true) // Redirect after successful social login
                );

        // CSRF
        http.csrf(cfg -> cfg.ignoringRequestMatchers(
                new AntPathRequestMatcher("/webauthn/**"), new AntPathRequestMatcher("/login/webauthn")));

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

            String defaultRedirectBase = this.webAuthNProperties.getAllowedOrigins().stream().findFirst().orElse("http://localhost:8080");

            RegisteredClient clientCredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeapi")
                    .clientSecret("VxubZgAXyyTq9lGjj3qGvWNsHtE4SqTq") // Raw secret, adapter's save will hash it
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scopes(scopeConfig -> scopeConfig.addAll(List.of(OidcScopes.OPENID, "ADMIN", "USER")))
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            RegisteredClient introspectClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeintrospect")
                    .clientSecret("c1BK9Bg2REeydBbvUoUeKCbD2bvJzXGj") // Raw secret
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scopes(scopeConfig -> scopeConfig.addAll(List.of(OidcScopes.OPENID)))
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .accessTokenFormat(OAuth2TokenFormat.REFERENCE).build()).build();

            RegisteredClient authCodeClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeclient")
                    .clientSecret("Qw3rTy6UjMnB9zXcV2pL0sKjHn5TxQqB") // Raw secret
                    .clientAuthenticationMethods(methods -> methods.addAll(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_POST, ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://oauth.pstmn.io/v1/callback")
                    .redirectUri(defaultRedirectBase + "/login/oauth2/code/custom")
                    .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .refreshTokenTimeToLive(Duration.ofHours(8)).reuseRefreshTokens(false)
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            RegisteredClient pkceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("amlumeapipublicclient")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("https://oauth.pstmn.io/v1/callback")
                    .redirectUri(defaultRedirectBase + "/login/oauth2/code/custom-public")
                    .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
                    .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                    .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(10))
                            .refreshTokenTimeToLive(Duration.ofHours(8)).reuseRefreshTokens(false)
                            .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build()).build();

            this.jpaRegisteredClientRepositoryAdapter.save(clientCredClient);
            this.jpaRegisteredClientRepositoryAdapter.save(introspectClient);
            this.jpaRegisteredClientRepositoryAdapter.save(authCodeClient);
            this.jpaRegisteredClientRepositoryAdapter.save(pkceClient);
            log.info("Finished seeding OAuth2 clients.");
        } else {
            log.info("OAuth2 registered clients already seem to exist. Skipping seeding.");
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
        config.addAllowedOrigin("http://127.0.0.1:8080");
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
                        Set<String> roles = AuthorityUtils.authorityListToSet(context.getPrincipal().getAuthorities())
                                .stream()
                                .map(r -> r.replaceFirst("^ROLE_", ""))
                                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
                        claims.put("roles", roles);

                        // Add user_id and full_name if the principal is a UserDetails representing your User
                        Object principal = context.getPrincipal().getPrincipal(); // Get the actual principal object
                        if (principal instanceof User appUser) { // Check if it's your User class
                            claims.put("user_id_numeric", appUser.getId());
                            claims.put("full_name", appUser.getDisplayableFullName());
                            claims.put("email", appUser.getEmail().getValue());
                        } else if (context.getPrincipal().getName() != null) {
                            // Fallback for other UserDetails implementations or if principal is just username string
                            userRepository.findByEmail_Value(context.getPrincipal().getName()).ifPresent(user -> {
                                claims.put("user_id_numeric", user.getId());
                                claims.put("full_name", user.getDisplayableFullName());
                                claims.put("email", user.getEmail().getValue());
                            });
                        }
                    }
                });
            }
        };
    }

    // --- PasswordEncoder, CompromisedPasswordChecker ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Checks for compromised passwords.
     *
     * @return {@link CompromisedPasswordChecker}
     */
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
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

    @ConfigurationProperties(prefix = "spring.security.webauthn")
    static class WebAuthNProperties {
        private String rpId = "localhost"; // Default value
        private String rpName = "Amlume Passkeys"; // Default value
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
            if (!(o instanceof WebAuthNProperties)) return false;
            final WebAuthNProperties other = (WebAuthNProperties) o;
            if (!other.canEqual((Object) this)) return false;
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
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(
            UserRepository userRepository, AuthorityRepository authorityRepository) {
        return new CustomOAuth2UserService(userRepository, authorityRepository);
    }

    @Bean
    public OAuth2UserService<org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest, OidcUser> customOidcUserService(
            UserRepository userRepository, AuthorityRepository authorityRepository, PasswordEncoder passwordEncoder) {
        return new CustomOidcUserService(userRepository, authorityRepository, passwordEncoder);
    }
}
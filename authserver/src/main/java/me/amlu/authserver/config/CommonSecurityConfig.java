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

import me.amlu.authserver.oauth2.service.JpaUserDetailsService;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.passkey.repository.DbUserCredentialRepository;
import me.amlu.authserver.passkey.service.PasskeyServiceImpl;
import me.amlu.authserver.security.CustomWebAuthnRelyingPartyOperations;
import me.amlu.authserver.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

import java.util.HashMap;
import java.util.Map;

import static me.amlu.authserver.common.SecurityConstants.*;

@Profile("!test")
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize, @PostAuthorize, etc.
public class CommonSecurityConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommonSecurityConfig.class);

    private final javax.sql.DataSource dataSource;

    public CommonSecurityConfig(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        String hierarchy = "ROLE_ROOT > ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_USER";
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    @Bean
    public DefaultWebSecurityExpressionHandler webSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

    // @Bean (if not already implicitly configured by Spring Boot)
    // public UserDetailsService userDetailsService(UserRepository userRepository) {
    //     return new JpaUserDetailsService(userRepository);
    // }
    // Spring Boot typically auto-configures UserDetailsService if there's one PasswordEncoder and one UserDetailsService bean.
    // If you have multiple, you might need to specify it in HttpSecurity:
    // http.userDetailsService(myUserDetailsService)

    /**
     * Returns the primary {@link UserDetailsService} for the application.
     * This service is responsible for loading user-specific data.
     *
     * @param userRepository {@link UserRepository}
     * @return {@link UserDetailsService}
     */
    @Bean
    @Primary
    @Qualifier("mainUserDetailsService")
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new JpaUserDetailsService(userRepository);
    }

    /**
     * Implement Remember me feature
     * <p>
     * Users do not need to authenticate in trusted browsers and devices.
     * <p>
     * Needs a persistent storage for remember me tokens.
     *
     * @return {@link PersistentTokenRepository}
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        // For initial development, you can let it create the table.
        // In production, you should manage schema via Flyway/Liquibase.
        // tokenRepository.setCreateTableOnStartup(true);
        // In production, manage schema via Flyway/Liquibase and keep setCreateTableOnStartup(false) (default).
        return tokenRepository;
    }

    // --- Passkey/WebAuthn Beans (Spring Security Native) ---

    /**
     * In Spring Security, the “relaying party parameter” is defined by defining a bean for WebAuthnRelyingPartyOperations.
     * You’ll need an ID (which is essentially your domain), name, and allowed origin (URL).
     *
     * @param userRepository                            {@link UserRepository}
     * @param dbPublicKeyCredentialUserEntityRepository {@link PublicKeyCredentialUserEntityRepository}
     * @param dbUserCredentialRepository                {@link UserCredentialRepository}
     * @param webAuthNProperties                        {@link WebAuthNProperties}
     * @return {@link WebAuthnRelyingPartyOperations}
     */
    @Bean
    public WebAuthnRelyingPartyOperations relyingPartyOperations(
            UserRepository userRepository,
            DbPublicKeyCredentialUserEntityRepository dbPublicKeyCredentialUserEntityRepository,
            DbUserCredentialRepository dbUserCredentialRepository, // Use the concrete type for injection
            WebAuthNProperties webAuthNProperties
    ) {

        if (webAuthNProperties.getAllowedOrigins() == null || webAuthNProperties.getAllowedOrigins().isEmpty()) {
            log.error("WebAuthn allowedOrigins is not configured or empty via WebAuthNProperties. Please check 'spring.security.webauthn.allowedOrigins'.");
            throw new IllegalStateException("WebAuthn allowedOrigins must be configured.");
        }
        log.info("Configuring WebAuthnRelyingPartyOperations with rpId: '{}', rpName: '{}', allowedOrigins: {}",
                webAuthNProperties.getRpId(), webAuthNProperties.getRpName(), webAuthNProperties.getAllowedOrigins());

        CustomWebAuthnRelyingPartyOperations operations = new CustomWebAuthnRelyingPartyOperations(
                userRepository,
                dbPublicKeyCredentialUserEntityRepository,
                dbUserCredentialRepository,
                webAuthNProperties
        );

        // --- Explicitly set the defaults for registration options ---

        // --- Use setCustomizeCreationOptions (More aligned with Spring Security's API) ---
        // This customizer is applied *after* Webauthn4JRelyingPartyOperations has already
        // populated the builder using its WebAuthnManager and the input request.
        // This is good for overriding or ensuring specific values.
        operations.setCustomizeCreationOptions(builder -> {
            log.debug("Customizing PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder...");

            // Example: Ensure timeout is always your application's default if not set by request
            // The builder here is org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder
            // It doesn't have a direct getTimeout() to check current value before setting.
            // So, this will effectively override.
            builder.timeout(PasskeyServiceImpl.PASSKEY_RELYING_PARTY_TIMEOUT);

            // Ensure attestation is INDIRECT OR NONE as some devices (like Apple devices) may not provide the required data if DIRECT is set
            builder.attestation(org.springframework.security.web.webauthn.api.AttestationConveyancePreference.INDIRECT); // NONE is default, less secure

            // Disabled as we implemented role-based conditional selection in PasskeyServiceImpl,
            // the builder below would overwrite/override the implementation.
            // Ensure authenticator selection is as desired
//            builder.authenticatorSelection(
//                    org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria.builder()
//                            .userVerification(org.springframework.security.web.webauthn.api.UserVerificationRequirement.PREFERRED)
//                            .residentKey(org.springframework.security.web.webauthn.api.ResidentKeyRequirement.PREFERRED)
////                            .authenticatorAttachment(org.springframework.security.web.webauthn.api.AuthenticatorAttachment.CROSS_PLATFORM) // CROSS_PLATFORM - USB or other roaming devices Or PLATFORM device bound (Windows Hello, Touch ID, Face ID), more secure, but less flexible
//                            .build()
//            );

            // Example: Ensure extensions are empty
            // Inside the setCustomizeCreationOptions lambda in CommonSecurityConfig.java

// Corrected and recommended way for empty extensions:
            builder.extensions(new org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs());
//            builder.extensions(new org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs((Collections.emptyMap()));

            log.info("Applied customizations to PublicKeyCredentialCreationOptions builder: timeout={}, attestation={}, authNSelection, extensions.",
                    PasskeyServiceImpl.PASSKEY_RELYING_PARTY_TIMEOUT,
                    org.springframework.security.web.webauthn.api.AttestationConveyancePreference.INDIRECT
//                    org.springframework.security.web.webauthn.api.AttestationConveyancePreference.NONE // Default, less secure but more flexible
            );
        });
        log.info("CustomWebAuthnRelyingPartyOperations configured with setCustomizeCreationOptions.");


        log.info("CustomWebAuthnRelyingPartyOperations bean fully configured.");
        return operations;
    }


    @Bean
    @DependsOn({"dbUserCredentialRepository", "dbPublicKeyCredentialUserEntityRepository", "relyingPartyOperations", "amlumeUserDetailsService"})
    public WebAuthnAuthenticationProvider webAuthnAuthenticationProvider(
            WebAuthnRelyingPartyOperations relyingPartyOperations, // Injected by Spring
            @Qualifier("mainUserDetailsService") UserDetailsService userDetailsService // Injected by Spring (your AmlumeUserDetailsService)
            // Note: UserCredentialRepository and PublicKeyCredentialUserEntityRepository are NOT passed to this constructor
            // but are needed by the provider's internal logic, likely accessed via RelyingPartyOperations or UserDetailsService.
    ) {
        log.info("Creating WebAuthnAuthenticationProvider bean.");
        return new WebAuthnAuthenticationProvider(
                relyingPartyOperations,
                userDetailsService // Pass your AmlumeUserDetailsService
        );
    }

    // --- PasswordEncoder, CompromisedPasswordChecker ---
    @Bean
    public PasswordEncoder passwordEncoder() {
//        Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(
//                16,    // saltLength
//                32,    // hashLength
//                2,     // parallelism (adjust based on CPU cores)
//                1 << 14, // memory cost (16MB - adjust based on available RAM)
//                3       // iterations (increase for more security, impacts performance)
//        );
        String encodingId = "argon2";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, new Argon2PasswordEncoder(ARGON2_SALT_LENGTH, ARGON2_HASH_LENGTH, ARGON2_PARALLELISM, ARGON2_MEMORY, ARGON2_ITERATIONS));
//        encoders.put(encodingId, new Argon2PasswordEncoder(16, 32, 2, 1 << 14, 3));
        return new DelegatingPasswordEncoder(encodingId, encoders);
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
}

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
import me.amlu.authserver.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Profile("!test")
@Configuration
public class CommonSecurityConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommonSecurityConfig.class);

    private final javax.sql.DataSource dataSource;

    public CommonSecurityConfig(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
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
     * @param userEntities       {@link PublicKeyCredentialUserEntityRepository}
     * @param userCredentials    {@link UserCredentialRepository}
     * @param webAuthNProperties {@link LocalSecurityConfig.WebAuthNProperties}
     * @return {@link WebAuthnRelyingPartyOperations}
     */
    @Bean
    public WebAuthnRelyingPartyOperations relyingPartyOperations(
            PublicKeyCredentialUserEntityRepository userEntities,
            UserCredentialRepository userCredentials,
            LocalSecurityConfig.WebAuthNProperties webAuthNProperties) { // Inject the properties bean

        if (webAuthNProperties.getAllowedOrigins() == null || webAuthNProperties.getAllowedOrigins().isEmpty()) {
            log.error("WebAuthn allowedOrigins is not configured or empty via WebAuthNProperties. Please check 'spring.security.webauthn.allowedOrigins'.");
            throw new IllegalStateException("WebAuthn allowedOrigins must be configured.");
        }
        log.info("Configuring WebAuthnRelyingPartyOperations with rpId: '{}', rpName: '{}', allowedOrigins: {}",
                webAuthNProperties.getRpId(), webAuthNProperties.getRpName(), webAuthNProperties.getAllowedOrigins());

        return new Webauthn4JRelyingPartyOperations(userEntities, userCredentials,
                PublicKeyCredentialRpEntity.builder()
                        .id(webAuthNProperties.getRpId())
                        .name(webAuthNProperties.getRpName()).build(),
                webAuthNProperties.getAllowedOrigins());
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
}

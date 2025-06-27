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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.amlu.authserver.config.jackson.mixin.DurationMixin;
import me.amlu.authserver.config.jackson.module.CustomJavaTimeModule;
import me.amlu.authserver.config.jackson.module.WebAuthnCustomModule;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.UserMixin;
import me.amlu.authserver.user.model.vo.AccountStatus;
import me.amlu.authserver.user.model.vo.AccountStatusMixin;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.EmailAddressMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.jspecify.annotations.NonNull;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.web.jackson2.WebJackson2Module;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;

import java.time.Duration;
import java.util.List;

/**
 * Configures the primary Jackson {@link ObjectMapper} for the application.
 */
@Configuration
public class JacksonConfig implements BeanClassLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(JacksonConfig.class);
    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates the primary ObjectMapper bean used throughout the application.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register modules
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new CustomJavaTimeModule());
        objectMapper.registerModule(new CoreJackson2Module());
        objectMapper.registerModule(new WebJackson2Module());
        objectMapper.registerModule(new WebauthnJackson2Module());
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        objectMapper.registerModule(new WebAuthnCustomModule()); // Add our custom WebAuthn module

        // Register Spring Security modules
        List<Module> securityModules = SecurityJackson2Modules.getModules(this.classLoader);
        objectMapper.registerModules(securityModules);

        // Configure features
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        
        // Add mixins
        objectMapper.addMixIn(Duration.class, DurationMixin.class);
        objectMapper.addMixIn(User.class, UserMixin.class);
        objectMapper.addMixIn(EmailAddress.class, EmailAddressMixin.class);
        objectMapper.addMixIn(AccountStatus.class, AccountStatusMixin.class);

        log.info("Configured primary ObjectMapper with WebAuthnCustomModule");
        return objectMapper;
    }
}

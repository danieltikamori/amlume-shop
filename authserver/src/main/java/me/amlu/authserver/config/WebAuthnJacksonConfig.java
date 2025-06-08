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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import me.amlu.authserver.config.jackson.mixin.DurationMixin;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.amlu.authserver.config.jackson.module.CustomJavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;

import java.time.Duration;

/**
 * Configuration for WebAuthn-specific Jackson serialization.
 * This class provides a specialized ObjectMapper and RedisSerializer for WebAuthn objects
 * to ensure proper serialization/deserialization when stored in Redis sessions.
 */
@Configuration
public class WebAuthnJacksonConfig implements BeanClassLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnJacksonConfig.class);

    private ClassLoader classLoader; // Field to store the ClassLoader

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) { // Use @NonNull
        this.classLoader = classLoader;
    }

    /**
     * Creates a specialized ObjectMapper for WebAuthn objects with proper type handling.
     * This mapper is configured with the WebauthnJackson2Module and appropriate type information
     * to ensure WebAuthn objects can be properly serialized and deserialized in Redis sessions.
     *
     * @param primaryObjectMapper             The application's primary ObjectMapper (from JacksonConfig)
     * @param sessionPolymorphicTypeValidator The validator for polymorphic types
     * @return A configured ObjectMapper for WebAuthn objects
     */
    @Bean
    @Qualifier("webAuthnSessionObjectMapper")
    public ObjectMapper webAuthnSessionObjectMapper(
            ObjectMapper primaryObjectMapper,
            @Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) {

        ObjectMapper webAuthnMapper = primaryObjectMapper.copy(); // Start with a copy of the primary

        // 1. Activate Default Typing - This is crucial for this mapper.
        // It should be done early as it can influence serializer selection.
        webAuthnMapper.activateDefaultTyping(
                sessionPolymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        log.info("webAuthnSessionObjectMapper: Activated default typing.");

        // 2. Register Spring Security modules.
        // The copy from primaryObjectMapper likely already has these.
        // Re-registering ensures they are present if the copy logic changes or if primaryObjectMapper is minimal.
        // WebauthnJackson2Module's DurationSerializer (outputs long) will be overridden for Duration.
        // Use this.classLoader which is set by Spring via BeanClassLoaderAware
        SecurityJackson2Modules.getModules(this.classLoader).forEach(webAuthnMapper::registerModule);
        // If WebauthnJackson2Module is not picked up by getModules, register it explicitly:
        if (SecurityJackson2Modules.getModules(this.classLoader).stream().noneMatch(m -> m instanceof WebauthnJackson2Module)) {
            log.info("webAuthnSessionObjectMapper: Explicitly registering WebauthnJackson2Module as it was not found in SecurityJackson2Modules.getModules()");
            webAuthnMapper.registerModule(new WebauthnJackson2Module());
        }
        log.info("webAuthnSessionObjectMapper: Ensured Spring Security modules are registered.");

        // 3. Register CustomJavaTimeModule.
        // If this module provides a DurationSerializer that outputs a string,
        // it should take precedence over WebauthnJackson2Module's due to later registration.
        webAuthnMapper.registerModule(new CustomJavaTimeModule());
        log.info("webAuthnSessionObjectMapper: Registered CustomJavaTimeModule.");

        // 4. CRITICAL: Ensure Duration is serialized as a STRING (ISO-8601).
        // This makes it compatible with Jackson's default typing mechanism.
        // This configuration explicitly tells the active JavaTimeModule (or CustomJavaTimeModule)
        // to serialize Duration as a string.
        webAuthnMapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        log.info("webAuthnSessionObjectMapper: Configured WRITE_DURATIONS_AS_TIMESTAMPS=false to ensure Duration serializes as String.");

        // 5. The DurationMixin might add @JsonTypeInfo to Duration for this mapper,
        // or provide custom serializer/deserializer that are type-aware.
        // This is important if the string representation alone isn't enough for default typing.
        webAuthnMapper.addMixIn(Duration.class, DurationMixin.class);
        log.info("webAuthnSessionObjectMapper: Added DurationMixin for Duration.class.");

        // The second call to activateDefaultTyping was redundant and has been removed.

        log.info("Configured specialized WebAuthn ObjectMapper for session serialization (Hash: {})",
                System.identityHashCode(webAuthnMapper));
        return webAuthnMapper;
    }

    /**
     * Creates a Redis serializer using the WebAuthn-specific ObjectMapper.
     * This serializer is optimized for handling WebAuthn objects in Redis sessions.
     *
     * @param webAuthnSessionObjectMapper The WebAuthn-specific ObjectMapper
     * @return A RedisSerializer for WebAuthn objects
     */
    @Bean
    @Qualifier("webAuthnRedisSerializer")
    public RedisSerializer<Object> webAuthnRedisSerializer(
            @Qualifier("webAuthnSessionObjectMapper") ObjectMapper webAuthnSessionObjectMapper) {

        log.info("Creating WebAuthn-specific Redis serializer using ObjectMapper (Hash: {})", System.identityHashCode(webAuthnSessionObjectMapper));
        return new GenericJackson2JsonRedisSerializer(webAuthnSessionObjectMapper);
    }
}

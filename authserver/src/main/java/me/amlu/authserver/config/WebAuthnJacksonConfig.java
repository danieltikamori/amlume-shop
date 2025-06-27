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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.amlu.authserver.config.jackson.mixin.DurationMixin;
import me.amlu.authserver.config.jackson.module.CustomJavaTimeModule;
import me.amlu.authserver.config.jackson.module.WebAuthnCustomModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.jspecify.annotations.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration for WebAuthn-specific Jackson serialization for session objects.
 * This class provides a specialized ObjectMapper and RedisSerializer for WebAuthn objects
 * to ensure proper serialization/deserialization when stored in sessions with default typing.
 */
@Configuration
public class WebAuthnJacksonConfig {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnJacksonConfig.class);

//    private ClassLoader classLoader;
//
//    @Override
//    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
//        this.classLoader = classLoader;
//    }

    @Bean(name = "webAuthnApiMapper")
    public ObjectMapper webAuthnApiMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register custom modules
        mapper.registerModule(new CustomJavaTimeModule());

        // Standard Spring Security module for WebAuthn types
        mapper.registerModule(new WebauthnJackson2Module());

        // The custom module for any other specific WebAuthn needs
        // (Ensure WebAuthnCustomModule is simplified as per step 2)
        mapper.registerModule(new WebAuthnCustomModule());

        //        mapper.registerModule(new JavaTimeModule());

//        // Optional: Disable writing dates as timestamps if you prefer ISO strings for other date types
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        // Optional: Disable writing durations as timestamps if CustomJavaTimeModule handles it as string
//        mapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

        // Enable default typing for polymorphic types
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        log.info("Configured 'webAuthnApiMapper' with CustomJavaTimeModule, WebauthnJackson2Module, and WebAuthnCustomModule.");

        return mapper;
    }

    /**
     * Creates a specialized ObjectMapper for WebAuthn objects to be stored in the session.
     * This mapper copies the configuration from the main 'sessionObjectMapper' (which should
     * already be correctly configured for Duration serialization via mixins and custom modules)
     * and then activates default typing for polymorphic deserialization from the session.
     *
     * @param sessionObjectMapper             The application's primary session ObjectMapper (from JacksonConfig),
     *                                        expected to be fully configured with all necessary modules and mixins.
     * @param sessionPolymorphicTypeValidator The validator for polymorphic types used in session.
     * @return A configured ObjectMapper for WebAuthn objects in the session.
     */
    @Bean
    @Qualifier("webAuthnSessionObjectMapper") // This mapper is for WebAuthn objects *in session*
    public ObjectMapper webAuthnSessionObjectMapper(
            @Qualifier("sessionObjectMapper") ObjectMapper sessionObjectMapper,
            @Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) {

        // Start with a copy of the sessionObjectMapper. This copy inherits all modules,
        // mixins (including CustomWebauthnMixins for PublicKeyCredentialCreationOptions.timeout),
        // and feature configurations from sessionObjectMapper.
        ObjectMapper webAuthnMapper = sessionObjectMapper.copy();
        log.info("webAuthnSessionObjectMapper: Copied 'sessionObjectMapper' (Hash: {}). This copy includes all its pre-configured modules and mixins.", System.identityHashCode(sessionObjectMapper));

        // Activate default typing using the provided PolymorphicTypeValidator.
        // The serializers for types like Duration (especially within PublicKeyCredentialCreationOptions.timeout)
        // must correctly implement serializeWithType. This is expected to be handled by the
        // configuration of the original sessionObjectMapper (e.g., via CustomWebauthnMixins
        // specifying the string-based DurationSerializer).
        webAuthnMapper.activateDefaultTyping(
                sessionPolymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        log.info("webAuthnSessionObjectMapper: Activated default typing with PTV.");

        // DO NOT re-register modules like SecurityJackson2Modules or JavaTimeModule here,
        // and do not add custom Duration serializers or mixins here.
        // Doing so can override the carefully configured serializer precedence inherited from sessionObjectMapper,
        // which is the likely cause of the Duration serialization issue.
        // The 'sessionObjectMapper' should be the single source of truth for serialization logic.

        log.info("Configured 'webAuthnSessionObjectMapper' (Hash: {}) for WebAuthn objects in session. " +
                        "It relies on the copied 'sessionObjectMapper' for detailed serialization logic (including Duration handling) " +
                        "and has default typing enabled.",
                System.identityHashCode(webAuthnMapper));

        return webAuthnMapper;
    }

    /**
     * Creates a Redis serializer using the WebAuthn-specific ObjectMapper.
     * This serializer is optimized for handling WebAuthn objects in Redis sessions.
     *
     * @param webAuthnSessionObjectMapperForRedis The WebAuthn-specific session ObjectMapper.
     * @return A RedisSerializer for WebAuthn objects.
     */
    @Bean
    @Qualifier("webAuthnRedisSerializer")
    public RedisSerializer<Object> webAuthnRedisSerializer(
            @Qualifier("webAuthnSessionObjectMapper") ObjectMapper webAuthnSessionObjectMapperForRedis) { // Use the mapper qualified for WebAuthn in session
        log.info("Creating WebAuthn-specific Redis serializer using 'webAuthnSessionObjectMapper' (Hash: {})",
                System.identityHashCode(webAuthnSessionObjectMapperForRedis));
        return new GenericJackson2JsonRedisSerializer(webAuthnSessionObjectMapperForRedis);
    }

    // --- Additional module specifically for Duration serialization ---
    // The explicitDurationModule and javaTimeModule() beans previously in this class are removed
    // as their functionality should now be correctly and centrally handled by the
    // 'sessionObjectMapper' configuration in JacksonConfig.java.
//    @Bean
//    public SimpleModule javaTimeModule() {
//        SimpleModule module = new SimpleModule("ExtraJavaTimeModule");
//        module.addSerializer(Duration.class, new JsonSerializer<Duration>() {
//            @Override
//            public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//                if (value == null) {
//                    gen.writeNull();
//                } else {
//                    gen.writeString(value.toString());
//                }
//            }
//
//            @Override
//            public void serializeWithType(Duration value, JsonGenerator gen, SerializerProvider serializers,
//                                          com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
//                if (value == null) {
//                    gen.writeNull();
//                    return;
//                }
//                WritableTypeId typeId = typeSer.typeId(value, Duration.class, JsonToken.VALUE_STRING);
//                typeSer.writeTypePrefix(gen, typeId);
//                serialize(value, gen, serializers);
//                typeSer.writeTypeSuffix(gen, typeId);
//            }
//        });
//        module.addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
//            @Override
//            public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                if (p.currentToken() == JsonToken.VALUE_NULL) {
//                    return null;
//                }
//                return Duration.parse(p.getValueAsString());
//            }
//        });
//        return module;
//    }
}

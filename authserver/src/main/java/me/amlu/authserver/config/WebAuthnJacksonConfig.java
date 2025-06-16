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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.amlu.authserver.config.jackson.mixin.DurationMixin;
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

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration for WebAuthn-specific Jackson serialization.
 * This class provides a specialized ObjectMapper and RedisSerializer for WebAuthn objects
 * to ensure proper serialization/deserialization when stored in Redis sessions.
 */
@Configuration
public class WebAuthnJacksonConfig implements BeanClassLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnJacksonConfig.class);

    private ClassLoader classLoader;

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates a specialized ObjectMapper for WebAuthn objects with proper type handling.
     * This mapper is configured with the WebauthnJackson2Module and appropriate type information
     * to ensure WebAuthn objects can be properly serialized and deserialized in Redis sessions.
     *
     * @param sessionObjectMapper             The application's primary ObjectMapper (from JacksonConfig)
     * @param sessionPolymorphicTypeValidator The validator for polymorphic types
     * @return A configured ObjectMapper for WebAuthn objects
     */
    @Bean
    @Qualifier("webAuthnSessionObjectMapper") // This mapper is for WebAuthn objects *in session*
    public ObjectMapper webAuthnSessionObjectMapper(
            @Qualifier("sessionObjectMapper") ObjectMapper sessionObjectMapper,
            @Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) {

        ObjectMapper webAuthnMapper = sessionObjectMapper.copy(); // Start with a copy of the primary
        log.info("webAuthnSessionObjectMapper: Copied 'sessionObjectMapper' (Hash: {}).", System.identityHashCode(sessionObjectMapper));

        // 1. Activate Default Typing - This is crucial for this mapper.
        // It should be done early as it can influence serializer selection.
        // The underlying serializers (especially for Duration within PKCCOptions)
        // must correctly implement serializeWithType. This should be ensured by sessionObjectMapper's configuration.
        webAuthnMapper.activateDefaultTyping(
                sessionPolymorphicTypeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY // Or the preferred mechanism
        );
        log.info("webAuthnSessionObjectMapper: Activated default typing.");

        // 2. Register Spring Security modules (including WebauthnJackson2Module)
        // Their serializers might be overridden by subsequent modules for specific types.
        SecurityJackson2Modules.getModules(this.classLoader).forEach(webAuthnMapper::registerModule);
        // Explicitly register WebauthnJackson2Module if not found by the above
        if (SecurityJackson2Modules.getModules(this.classLoader).stream().noneMatch(m -> m instanceof WebauthnJackson2Module)) {
            log.info("webAuthnSessionObjectMapper: Explicitly registering WebauthnJackson2Module as it was not found by SecurityJackson2Modules.getModules().");
            webAuthnMapper.registerModule(new WebauthnJackson2Module());
        }
        log.info("webAuthnSessionObjectMapper: Ensured Spring Security modules (including WebauthnJackson2Module) are registered.");

        // 3. Register standard JavaTimeModule AFTER WebauthnJackson2Module.
        // Its DurationSerializer respects WRITE_DURATIONS_AS_TIMESTAMPS and should override
        // the one from WebauthnJackson2Module for Duration.class.
        webAuthnMapper.registerModule(new JavaTimeModule());
        log.info("webAuthnSessionObjectMapper: Registered standard JavaTimeModule.");

        // 4. Configure Duration serialization to STRING (ISO-8601) for JavaTimeModule.
        // This is a general instruction for JavaTimeModule.
        webAuthnMapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        log.info("webAuthnSessionObjectMapper: Configured WRITE_DURATIONS_AS_TIMESTAMPS=false (intended for JavaTimeModule).");

        // 5. Force String serialization for Duration with a custom SimpleModule registered LAST,
        //    AND ensure it correctly handles serializeWithType.
        SimpleModule explicitDurationModule = new SimpleModule("ExplicitDurationWithTypeModule");
        explicitDurationModule.addSerializer(Duration.class, new JsonSerializer<Duration>() {
            @Override
            public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.toString()); // e.g., "PT120S"
                }
            }

            @Override
            public void serializeWithType(Duration value, JsonGenerator gen, SerializerProvider serializers,
                                          com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
                // This method is called when default typing is active for Duration.
                // We write the type information then the value as a string.
                WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, Duration.class, JsonToken.VALUE_STRING));
                serialize(value, gen, serializers); // Write the duration as a string using the method above
                typeSer.writeTypeSuffix(gen, typeId);
            }
        });
        webAuthnMapper.registerModule(explicitDurationModule);

        // Add mixin for Duration to ensure proper type handling
        webAuthnMapper.addMixIn(Duration.class, DurationMixin.class);

        log.info("webAuthnSessionObjectMapper: Registered custom Duration serialization with type handling");

        log.info("Configured specialized WebAuthn ObjectMapper for session serialization (Hash: {})",
                System.identityHashCode(webAuthnMapper));
        return webAuthnMapper;
    }

    /**
     * Creates a Redis serializer using the WebAuthn-specific ObjectMapper.
     * This serializer is optimized for handling WebAuthn objects in Redis sessions.
     *
     * @param webAuthnSessionObjectMapperForRedis The WebAuthn-specific ObjectMapper
     * @return A RedisSerializer for WebAuthn objects
     */
    @Bean
    @Qualifier("webAuthnRedisSerializer")
    public RedisSerializer<Object> webAuthnRedisSerializer(
            @Qualifier("webAuthnSessionObjectMapper") ObjectMapper webAuthnSessionObjectMapperForRedis) { // Use the mapper qualified for WebAuthn in session
        log.info("Creating WebAuthn-specific Redis serializer using 'webAuthnSessionObjectMapper' (Hash: {})",
                System.identityHashCode(webAuthnSessionObjectMapperForRedis));
        return new GenericJackson2JsonRedisSerializer(webAuthnSessionObjectMapperForRedis);
    }

    // Additional module specifically for Duration serialization
    @Bean
    public SimpleModule javaTimeModule() {
        SimpleModule module = new SimpleModule("ExtraJavaTimeModule");
        module.addSerializer(Duration.class, new JsonSerializer<Duration>() {
            @Override
            public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.toString());
                }
            }

            @Override
            public void serializeWithType(Duration value, JsonGenerator gen, SerializerProvider serializers,
                                          com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                WritableTypeId typeId = typeSer.typeId(value, Duration.class, JsonToken.VALUE_STRING);
                typeSer.writeTypePrefix(gen, typeId);
                serialize(value, gen, serializers);
                typeSer.writeTypeSuffix(gen, typeId);
            }
        });
        module.addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
            @Override
            public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    return null;
                }
                return Duration.parse(p.getValueAsString());
            }
        });
        return module;
    }
}

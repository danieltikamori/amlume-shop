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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.session.data.mongo.AbstractMongoSessionConverter;
import org.springframework.session.data.mongo.JacksonMongoSessionConverter;
import org.springframework.session.data.mongo.JdkMongoSessionConverter;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

import java.time.Duration;
import java.util.Collections;

/**
 * Configuration for MongoDB-based session storage.
 * This replaces the Redis session configuration to avoid serialization issues.
 */
@Configuration
// EnableMongoHttpSession should be present to activate MongoDB session handling.
// The maxInactiveIntervalInSeconds and collectionName can also be set in application.properties:
// spring.session.mongodb.collection-name=auth_sessions
// server.servlet.session.timeout=2h (e.g., 7200s)
@EnableMongoHttpSession(maxInactiveIntervalInSeconds = 7200, collectionName = "auth_sessions")
public class MongoSessionConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoSessionConfig.class);
    private final ObjectMapper sessionObjectMapper; // Renamed for clarity

    public MongoSessionConfig(@Qualifier("sessionObjectMapper") ObjectMapper sessionObjectMapper) { // Use @Qualifier
        this.sessionObjectMapper = sessionObjectMapper;
        log.info("MongoSessionConfig initialized with 'sessionObjectMapper' (Hash: {})",
                System.identityHashCode(this.sessionObjectMapper));
    }

    // Diagnostic
    @PostConstruct
    public void init() {
        log.info("MongoSessionConfig initialized with collection name: auth_sessions");
    }

    /**
     * Provides a custom MongoSessionConverter that uses the application's
     * primary Jackson ObjectMapper. This ensures that session attributes,
     * especially the Spring SecurityContext, are serialized and deserialized
     * correctly with all necessary modules and mixins, including the
     * PolymorphicTypeValidator.
     *
     * @return A configured AbstractMongoSessionConverter.
     */
    @Bean
    @Primary // Important if other converters might exist (like the JdkMongoSessionConverter below)
    public AbstractMongoSessionConverter mongoSessionConverter() {
        // JacksonMongoSessionConverter is used by Spring Session when Jackson is on the classpath.
        // We provide our fully configured primaryObjectMapper to it.
        // The primaryObjectMapper already has the PolymorphicTypeValidator and necessary modules.
        JacksonMongoSessionConverter converter = new JacksonMongoSessionConverter(this.sessionObjectMapper); // Use the qualified mapper
        log.info("Custom JacksonMongoSessionConverter configured to use 'sessionObjectMapper' (Hash: {}).",
                System.identityHashCode(this.sessionObjectMapper));
        return converter;

        // --- Use the LoggingJacksonMongoSessionConverter (diagnostic tool)
//        LoggingJacksonMongoSessionConverter converter = new LoggingJacksonMongoSessionConverter(this.primaryObjectMapper);
//        log.info("Custom LoggingJacksonMongoSessionConverter configured to use primary ObjectMapper (Hash: {}).",
//                System.identityHashCode(this.primaryObjectMapper));
//        return converter;
        // --- END diagnostic tool ---
    }

    /**
     * Provides custom MongoDB type conversions.
     * For session serialization, Jackson is the primary mechanism, so this is often empty
     * unless you have specific domain types that need custom BSON conversion
     * outside of what Jackson handles for session attributes.
     *
     * @return A MongoCustomConversions object.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        // Typically, for session attributes serialized by Jackson, this list can be empty.
        // Add custom converters here if you have types stored directly in MongoDB
        // (not as session attributes) that need special BSON mapping.
        return new MongoCustomConversions(Collections.emptyList());
    }

    // The @Bean for "jacksonMongoSessionConverter" with @Qualifier is redundant
    // if the @Primary mongoSessionConverter() bean above is already providing
    // the JacksonMongoSessionConverter with the primary ObjectMapper.
    // Spring Session will pick up the @Primary AbstractMongoSessionConverter.
    // Keeping it might not cause harm but isn't strictly necessary unless
    // explicitly injected by @Qualifier elsewhere for a specific reason.
    // For clarity and to avoid potential confusion, it can be removed if mongoSessionConverter() is @Primary.

    /**
     * Creates a MongoDB session converter using the application's primary ObjectMapper.
     * This ensures consistent serialization/deserialization of session attributes.
     * This bean is redundant if mongoSessionConverter() is @Primary and provides the same type.
     *
     * @param objectMapper The application's primary ObjectMapper
     * @return A configured JacksonMongoSessionConverter
     */
    /*
    @Bean
    @Qualifier("jacksonMongoSessionConverter") // Qualified name if needed elsewhere
    public JacksonMongoSessionConverter jacksonMongoSessionConverter(@Qualifier("objectMapper") ObjectMapper objectMapper) {
        log.info("Configuring qualified 'jacksonMongoSessionConverter' with primary ObjectMapper (Hash: {})", System.identityHashCode(objectMapper));
        return new JacksonMongoSessionConverter(objectMapper);
    }
    */

    // ---- Diagnostic tool ---
//    @Bean
//    @Qualifier("jacksonMongoSessionConverter")
//    public LoggingJacksonMongoSessionConverter jacksonMongoSessionConverter(@Qualifier("objectMapper") ObjectMapper objectMapper) {
//        log.info("Configuring qualified 'jacksonMongoSessionConverter' (Logging version) with primary ObjectMapper (Hash: {})", System.identityHashCode(objectMapper));
//        return new LoggingJacksonMongoSessionConverter(objectMapper);
//    }

    /**
     * Alternative MongoDB session converter using JDK serialization.
     * This can be used as a fallback or for specific needs but is generally less flexible
     * and can be less performant than Jackson-based serialization for complex objects.
     * It's good to have as an option but ensure the @Primary bean is the Jackson-based one
     * if that's your intended default.
     *
     * @return A configured JdkMongoSessionConverter
     */
    @Bean
    @Qualifier("jdkMongoSessionConverter") // Qualify it so it doesn't conflict with the primary
    public JdkMongoSessionConverter jdkMongoSessionConverter() {
        log.info("Configuring JDK-based MongoDB session converter as a non-primary option.");
        // Ensure this timeout matches your desired session timeout (e.g., server.servlet.session.timeout)
        return new JdkMongoSessionConverter(Duration.ofSeconds(7200));
    }
}

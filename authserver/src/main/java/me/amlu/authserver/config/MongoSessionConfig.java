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
// Ensure maxInactiveIntervalInSeconds matches your desired session timeout
// and collectionName matches your MongoDB collection for sessions.
// Removed @EnableMongoHttpSession as we're manually configuring the session repository in SessionRepositoryConfig
public class MongoSessionConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoSessionConfig.class);
    private final ObjectMapper primaryObjectMapper;

    // Inject the @Primary ObjectMapper configured in JacksonConfig
    public MongoSessionConfig(@Qualifier("objectMapper") ObjectMapper primaryObjectMapper) {
        this.primaryObjectMapper = primaryObjectMapper;
        log.info("MongoSessionConfig initialized with primary ObjectMapper (Hash: {})",
                System.identityHashCode(this.primaryObjectMapper));
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
    @Primary // Important if other converters might exist
    public AbstractMongoSessionConverter mongoSessionConverter() {
        // JacksonMongoSessionConverter is used by Spring Session when Jackson is on the classpath.
        // We provide our fully configured primaryObjectMapper to it.
        // The primaryObjectMapper already has the PolymorphicTypeValidator and necessary modules.
        JacksonMongoSessionConverter converter = new JacksonMongoSessionConverter(this.primaryObjectMapper);
        log.info("Custom JacksonMongoSessionConverter configured to use primary ObjectMapper (Hash: {}).",
                System.identityHashCode(this.primaryObjectMapper));
        return converter;
    }

    /**
     * Provides custom MongoDB type conversions.
     * For session serialization, Jackson is the primary mechanism, so this is often empty.
     *
     * @return A MongoCustomConversions object.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Collections.emptyList());
    }


    /**
     * Creates a MongoDB session converter using the application's primary ObjectMapper.
     * This ensures consistent serialization/deserialization of session attributes.
     *
     * @param objectMapper The application's primary ObjectMapper
     * @return A configured JacksonMongoSessionConverter
     */
    @Bean
    @Qualifier("jacksonMongoSessionConverter") // Qualified name if needed elsewhere
    public JacksonMongoSessionConverter jacksonMongoSessionConverter(@Qualifier("objectMapper") ObjectMapper objectMapper) {
        log.info("Configuring qualified 'jacksonMongoSessionConverter' with primary ObjectMapper (Hash: {})", System.identityHashCode(objectMapper));
        return new JacksonMongoSessionConverter(objectMapper);
    }

    /**
     * Alternative MongoDB session converter using JDK serialization.
     * This can be used as a fallback if Jackson serialization encounters issues.
     *
     * @return A configured JdkMongoSessionConverter
     */
    @Bean
    public JdkMongoSessionConverter jdkMongoSessionConverter() {
        log.info("Configuring JDK-based MongoDB session converter");
        return new JdkMongoSessionConverter(Duration.ofSeconds(7200));
    }
}

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
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Configures session behavior for the application.
 * This configuration includes custom settings for cookie behavior.
 *
 * <p>Sessions are now stored in MongoDB instead of Redis to avoid serialization issues.
 * Redis is still used for caching purposes.
 * </p>
 *
 * @see JacksonConfig
// * @see MongoSessionConfig
 */
//@Configuration
//@EnableHazelcastHttpSession // Enable Hazelcast-backed HTTP sessions
// maxInactiveIntervalInSeconds can be set via spring.session.timeout in application.properties
// mapName can be set via spring.session.hazelcast.map-name
// This enables non-indexed Redis sessions. If you need indexed sessions, use @EnableRedisIndexedHttpSession
// The redisNamespace isolates session keys, e.g., "authserver:session:amlume:sessions:<session_id>"
// Removed Redis session configuration in favor of MongoDB
@Configuration
@EnableJdbcHttpSession // Enable JDBC-backed HTTP sessions
public class SessionConfig {

    private static final Logger log = LoggerFactory.getLogger(SessionConfig.class);

    // Inject the primary ObjectMapper configured in JacksonConfig
    private final ObjectMapper objectMapperForSession;
    private final PolymorphicTypeValidator sessionPolymorphicTypeValidator;
//    private final ClassLoader classLoader;

    /**
     * Injects the {@code spring.session.cookie.secure} property value.
     * Defaults to {@code true} if the property is not set.
     * This allows conditional configuration of the 'Secure' cookie attribute,
     * which is useful for local development over HTTP.
     */
    @Value("${spring.session.cookie.secure:true}")
    private boolean useSecureCookie;

    /**
     * Constructs the SessionConfig with the primary Jackson ObjectMapper.
     * The ObjectMapper is typically provided by {@link JacksonConfig} and is used for
     * serializing session attributes to JSON.
     *
     * @param sessionObjectMapper             The primary {@link ObjectMapper} instance.
     * @param sessionPolymorphicTypeValidator The polymorphic type validator for the session.
     *                                        //     * @param classLoader The class loader used for loading classes.
     */
    public SessionConfig(
            @Qualifier("sessionObjectMapper") ObjectMapper sessionObjectMapper,
            @Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) { // Inject primary ObjectMapper
        this.objectMapperForSession = sessionObjectMapper;
        this.sessionPolymorphicTypeValidator = sessionPolymorphicTypeValidator;
//        this.classLoader = getClass().getClassLoader(); // Possibly not used
        log.info("SessionConfig initialized with 'sessionObjectMapper' (Hash: {}) and PTV. 'spring.session.cookie.secure' is set to: {}",
                System.identityHashCode(this.objectMapperForSession), this.useSecureCookie);
    }

    // Redis serializer bean removed as we're now using MongoDB for sessions

    /**
     * Customizes the session cookie (e.g., JSESSIONID).
     * This configuration enhances security and aligns with application requirements.
     *
     * <p><b>Cookie Attributes:</b></p>
     * <ul>
     *   <li><b>Name:</b> {@code AMLUAUTHJSESSIONID} (custom name).</li>
     *   <li><b>Path:</b> {@code /} (cookie accessible from all paths).</li>
     *   <li><b>HttpOnly:</b> {@code true} (prevents client-side script access, mitigating XSS).</li>
     *   <li><b>Secure:</b> Configured by {@code spring.session.cookie.secure} property (defaults to {@code true}).
     *       When true, the cookie is only sent over HTTPS. For local development over HTTP,
     *       set {@code spring.session.cookie.secure=false} in the local application properties.</li>
     *   <li><b>SameSite:</b> {@code Lax} (provides a balance between security and usability for cross-site requests).
     *       Consider "Strict" for higher security if "Lax" behavior is not needed.</li>
     *   <li><b>DomainNamePattern:</b> {@code ^.+?\\.(\\w+\\.[a-z]+)$}. This pattern attempts to set the cookie
     *       on the parent domain (e.g., for {@code auth.example.com}, sets cookie for {@code .example.com}),
     *       allowing session sharing across subdomains of the same parent.
     *       For {@code localhost} or IP addresses, this pattern will not match, and the cookie domain
     *       will correctly default to the host, which is the desired behavior.</li>
     *   <li><b>MaxAge:</b> {@code 7200} seconds (2 hours). This sets the cookie's expiration time in the browser.
     *       Server-side session inactivity timeout is configured separately (e.g., {@code server.servlet.session.timeout}).
     *       The max-age is set due to requirements/recommendations for some browsers (e.g., iOS Safari)
     *       to persist cookies across sessions. See:
     *       <a href="https://github.com/spring-projects/spring-session/issues/879">Spring Session Issue #879</a>.
     * </ul>
     *
     * @return A configured {@link CookieSerializer}.
     * @see <a href="https://docs.spring.io/spring-session/reference/guides/java-custom-cookie.html">Spring Session Custom Cookie Guide</a>
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("AMLUAUTHJSESSIONID"); // Matches the application.yml
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true); // Security: Mitigate XSS

        // Conditionally set Secure attribute based on configuration property
        // For production, this should always be true (HTTPS).
        // For local HTTP development, set spring.session.cookie.secure=false in application-local.properties
        serializer.setUseSecureCookie(this.useSecureCookie);
//        serializer.setUseSecureCookie(true);

        serializer.setSameSite("Lax"); // Recommended for most cases. Alternatives: "Strict", "None" (requires Secure).

        // Ensure consistent cookie naming
        // Note: setSessionIdAlias is not available in this version of Spring Session

        // Sets cookie for parent domain (e.g., .example.com for app.example.com)
        // This pattern correctly handles localhost by not setting a domain.
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");

        // Max age in seconds (e.g., 7200s = 2 hours)
        // Important for some browsers (iOS) to persist session cookies.
        serializer.setCookieMaxAge(7200);

        // Disable the JSessionID URL rewriting to prevent session ID leakage
        serializer.setUseBase64Encoding(true);
        serializer.setRememberMeRequestAttribute("remember-me");

        log.info("Custom CookieSerializer configured: Name=AMLUAUTHJSESSIONID, Secure={}, SameSite=Lax, MaxAge=7200s.", this.useSecureCookie);
        return serializer;
    }

    // If you need to customize HazelcastInstance configuration (e.g., for embedded server or specific client settings not covered by properties):
    /*
    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("dev"); // Or your cluster name
        clientConfig.getNetworkConfig().addAddress("localhost:5701"); // Or hazelcast:5701 if app is also in Docker
        // Add other client configurations if needed
        return HazelcastClient.newHazelcastClient(clientConfig);
    }
    */
    // However, Spring Boot auto-configuration for Hazelcast client is usually sufficient
    // when `spring.hazelcast.client.*` properties are set.
}

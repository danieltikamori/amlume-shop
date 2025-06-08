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
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.session.MapSession;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.*;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Configures Spring Session to use Redis as the session store.
 * This configuration includes custom settings for session serialization, cookie behavior,
 * and robust session mapping.
 *
 * <p>Sessions are stored in Redis under the namespace "{@code authserver:session:amlume}".
 * This setup uses non-indexed Redis sessions. If session indexing features (e.g., finding sessions
 * by principal name) are required, consider using {@code @EnableRedisIndexedHttpSession}.
 * </p>
 *
 * @see EnableRedisHttpSession
 * @see JacksonConfig
 * @see RedisSessionConfig
 */
@Configuration
// This enables non-indexed Redis sessions. If you need indexed sessions, use @EnableRedisIndexedHttpSession
// The redisNamespace isolates session keys, e.g., "authserver:session:amlume:sessions:<session_id>"
@EnableRedisHttpSession(redisNamespace = "authserver:session:amlume")
public class SessionConfig {

    private static final Logger log = LoggerFactory.getLogger(SessionConfig.class);

    // Inject the primary ObjectMapper configured in JacksonConfig
    private final ObjectMapper primaryObjectMapper;
    private final PolymorphicTypeValidator sessionPolymorphicTypeValidator;
    private final ClassLoader classLoader;

    /**
     * Injects the {@code spring.session.secure-cookie} property value.
     * Defaults to {@code true} if the property is not set.
     * This allows conditional configuration of the 'Secure' cookie attribute,
     * which is useful for local development over HTTP.
     */
    @Value("${spring.session.secure-cookie:true}")
    private boolean useSecureCookie;

    /**
     * Constructs the SessionConfig with the primary Jackson ObjectMapper.
     * The ObjectMapper is typically provided by {@link JacksonConfig} and is used for
     * serializing session attributes to JSON.
     *
     * @param primaryObjectMapper The primary {@link ObjectMapper} instance.
     */
    public SessionConfig(ObjectMapper primaryObjectMapper,
                         @Qualifier("customSessionPolymorphicTypeValidator") PolymorphicTypeValidator sessionPolymorphicTypeValidator) { // Inject primary ObjectMapper
        this.primaryObjectMapper = primaryObjectMapper;
        this.sessionPolymorphicTypeValidator = sessionPolymorphicTypeValidator;
        this.classLoader = getClass().getClassLoader();
        log.info("SessionConfig initialized with primary ObjectMapper (Hash: {}) and PTV. 'spring.session.secure-cookie' is set to: {}",
                System.identityHashCode(this.primaryObjectMapper), this.useSecureCookie);
    }

    /**
     * Defines the default Redis serializer for Spring Session.
     * <p><b>Important:</b> The bean name must be exactly {@code springSessionDefaultRedisSerializer}
     * to override the default serializer used by Spring Session for session attributes.
     * </p>
     * This bean uses a {@link GenericJackson2JsonRedisSerializer} configured with the
     * application's primary {@link ObjectMapper}. This ensures consistent JSON serialization
     * for session data, leveraging any custom modules or configurations (e.g., Java 8 time,
     * Spring Security types) registered with the primary ObjectMapper.
     *
     * @return A {@link RedisSerializer} that serializes objects to JSON using Jackson.
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper sessionObjectMapper = primaryObjectMapper.copy();

        // Use the injected PTV
        sessionObjectMapper.activateDefaultTyping(this.sessionPolymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // Explicitly register WebauthnJackson2Module to ensure WebAuthn objects are properly handled
        sessionObjectMapper.registerModule(new WebauthnJackson2Module());

        // Register all Spring Security modules to ensure comprehensive security object handling
        SecurityJackson2Modules.getModules(this.classLoader)
                .forEach(sessionObjectMapper::registerModule);

        log.info("Creating 'springSessionDefaultRedisSerializer' bean using a dedicated ObjectMapper (Hash: {}) with default typing enabled for session attributes.",
                System.identityHashCode(sessionObjectMapper));
        return new GenericJackson2JsonRedisSerializer(sessionObjectMapper);
    }

    /**
     * Customizes the session cookie (e.g., JSESSIONID).
     * This configuration enhances security and aligns with application requirements.
     *
     * <p><b>Cookie Attributes:</b></p>
     * <ul>
     *   <li><b>Name:</b> {@code AMLUAUTHJSESSIONID} (custom name).</li>
     *   <li><b>Path:</b> {@code /} (cookie accessible from all paths).</li>
     *   <li><b>HttpOnly:</b> {@code true} (prevents client-side script access, mitigating XSS).</li>
     *   <li><b>Secure:</b> Configured by {@code spring.session.secure-cookie} property (defaults to {@code true}).
     *       When true, the cookie is only sent over HTTPS. For local development over HTTP,
     *       set {@code spring.session.secure-cookie=false} in the local application properties.</li>
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
        // For local HTTP development, set spring.session.secure-cookie=false in application-local.properties
        serializer.setUseSecureCookie(this.useSecureCookie);
//        serializer.setUseSecureCookie(true);

        serializer.setSameSite("Lax"); // Recommended for most cases. Alternatives: "Strict", "None" (requires Secure).

        // Sets cookie for parent domain (e.g., .example.com for app.example.com)
        // This pattern correctly handles localhost by not setting a domain.
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");

        // Max age in seconds (e.g., 7200s = 2 hours)
        // Important for some browsers (iOS) to persist session cookies.
        serializer.setCookieMaxAge(7200);

        log.info("Custom CookieSerializer configured: Name=AMLUAUTHJSESSIONID, Secure={}, SameSite=Lax, MaxAge=7200s.", this.useSecureCookie);
        return serializer;
    }

    /**
     * Customizes the {@link RedisSessionRepository} to use a {@link SafeRedisSessionMapper}.
     * This enhances resilience by handling potential deserialization errors for session data
     * retrieved from Redis.
     *
     * @return A {@link SessionRepositoryCustomizer} for {@link RedisSessionRepository}.
     */
    @Bean
    SessionRepositoryCustomizer<RedisSessionRepository> redisSessionRepositoryCustomizer() {
        return (redisSessionRepository) -> {
            log.info("Customizing RedisSessionRepository with SafeRedisSessionMapper.");
            redisSessionRepository.setRedisSessionMapper(new SafeRedisSessionMapper(redisSessionRepository));
        };
    }

    /**
     * A custom {@link BiFunction} that wraps the default {@link RedisSessionMapper} to provide
     * robust handling of session deserialization.
     * If an {@link IllegalStateException} (commonly thrown during Jackson deserialization issues,
     * e.g., due to class changes or corrupted data) occurs while mapping Redis data to a
     * {@link MapSession}, this mapper will log the error and delete the problematic session
     * from Redis to prevent further issues.
     */
    static class SafeRedisSessionMapper implements BiFunction<String, Map<String, Object>, MapSession> {
        private static final Logger log = LoggerFactory.getLogger(SafeRedisSessionMapper.class);
        private final RedisSessionMapper delegate = new RedisSessionMapper();
        private final RedisSessionRepository sessionRepository;

        /**
         * Constructs a {@code SafeRedisSessionMapper}.
         *
         * @param sessionRepository The {@link RedisSessionRepository} used to delete sessions
         *                          in case of deserialization errors.
         */
        SafeRedisSessionMapper(RedisSessionRepository sessionRepository) {
            this.sessionRepository = sessionRepository;
        }

        /**
         * Applies the mapping from a Redis hash (represented as a {@code Map<String, Object>})
         * to a {@link MapSession}.
         * Catches {@link IllegalStateException} during mapping, logs it, deletes the session,
         * and returns {@code null}.
         *
         * @param sessionId The ID of the session being mapped.
         * @param map       The map of session attributes retrieved from Redis.
         * @return A {@link MapSession} instance, or {@code null} if a deserialization error occurred
         * and the session was deleted.
         */
        @Override
        public MapSession apply(String sessionId, Map<String, Object> map) {
            // TEMPORARY debugging
            log.debug("SafeRedisSessionMapper - Session ID: '{}', Map: '{}'", sessionId, map);
            try {
                return this.delegate.apply(sessionId, map);
            } catch (IllegalStateException ex) {
                // Log the error with session ID for diagnostics
                log.warn("Failed to deserialize session data for sessionId '{}'. " +
                        "The session will be deleted from Redis. Error: {}", sessionId, ex.getMessage(), ex);
                try {
                    this.sessionRepository.deleteById(sessionId);
                    log.info("Successfully deleted problematic session '{}' from Redis.", sessionId);
                } catch (Exception deleteEx) {
                    log.error("Failed to delete problematic session '{}' from Redis after deserialization error. " +
                            "Manual cleanup might be required. Delete Error: {}", sessionId, deleteEx.getMessage(), deleteEx);
                }
                return null; // Indicate that the session could not be loaded
            }
        }
    }

    /**
     * Configures the {@link RedisSessionExpirationStore} used by Spring Session to manage
     * session expiration events in Redis.
     * <p>
     * Redis expiration events can be unreliable if keys are not accessed. This store helps
     * ensure more deterministic firing of expiration events by tracking session expirations
     * (typically to the nearest minute) and allowing a background task to access potentially
     * expired sessions.
     * </p>
     * <p>
     * This bean creates a dedicated {@link RedisTemplate} for the expiration store.
     * The keys for expiration tracking will be stored under the namespace defined by
     * {@link RedisIndexedSessionRepository#DEFAULT_NAMESPACE} (which is "spring:session"),
     * e.g., "{@code spring:session:expirations:<timestamp>}". This is separate from the main
     * session data namespace ("{@code authserver:session:amlume}").
     * </p>
     *
     * @param redisConnectionFactory The {@link RedisConnectionFactory} for connecting to Redis.
     * @return A configured {@link RedisSessionExpirationStore}.
     * @see SortedSetRedisSessionExpirationStore
     */
    @Bean
    public RedisSessionExpirationStore redisSessionExpirationStore(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // Use StringRedisSerializer for keys, as expiration keys are strings.
        redisTemplate.setKeySerializer(RedisSerializer.string());
        // Values stored by SortedSetRedisSessionExpirationStore are session IDs (strings).
        // Explicitly set StringRedisSerializer for values for clarity and efficiency.
        redisTemplate.setValueSerializer(RedisSerializer.string());
        // Hash key/value serializers are not strictly necessary for SortedSet operations
        // but setting them to StringRedisSerializer is a safe default if the template were used for hashes.
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.string());

        // afterPropertiesSet() initializes the template.
        redisTemplate.afterPropertiesSet();

        // The namespace here ("spring:session") is for the expiration tracking keys,
        // distinct from the main session data namespace.
        String expirationNamespace = RedisIndexedSessionRepository.DEFAULT_NAMESPACE;
        log.info("Configuring RedisSessionExpirationStore with namespace: '{}'", expirationNamespace);
        return new SortedSetRedisSessionExpirationStore(redisTemplate, expirationNamespace);
    }
}

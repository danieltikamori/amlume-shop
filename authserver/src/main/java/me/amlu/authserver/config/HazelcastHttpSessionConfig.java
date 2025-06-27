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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.client.config.ClientUserCodeDeploymentConfig;
import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import me.amlu.authserver.config.hazelcast.AuthoritySerializer;
import me.amlu.authserver.config.hazelcast.CsrfTokenSerializer;
import me.amlu.authserver.config.hazelcast.SecurityContextSerializer;
import me.amlu.authserver.oauth2.model.Authority;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.session.FlushMode;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.Session;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionSerializer;
import org.springframework.session.hazelcast.PrincipalNameExtractor;
import org.springframework.session.hazelcast.SessionUpdateEntryProcessor;
import org.springframework.session.hazelcast.config.annotation.SpringSessionHazelcastInstance;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Configures Hazelcast for Spring Session.
 * This setup uses Hazelcast in a client-server mode, connecting to an external Hazelcast cluster
 * (as defined in Docker Compose). It enables JSON serialization for session attributes.
 *
 */
import static me.amlu.authserver.common.SecurityConstants.REMEMBER_ME_PARAMETER;

/**
 * Configures Hazelcast for Spring Session.
 * This setup uses Hazelcast in a client-server mode, connecting to an external Hazelcast cluster
 * (as defined in Docker Compose). It enables JSON serialization for session attributes.
 */
@Configuration
@EnableHazelcastHttpSession
public class HazelcastHttpSessionConfig {

    private static final Logger log = LoggerFactory.getLogger(HazelcastHttpSessionConfig.class);

    @Value("${spring.session.cookie.secure:true}")
    private boolean useSecureCookie;

    @Value("${spring.session.hazelcast.map-name:spring:session:authserver:sessions}")
    private String sessionMapName;

    @Value("${server.servlet.session.timeout:2h}")
    private Duration sessionTimeout;

    @Value("${spring.hazelcast.client.cluster-name:dev}")
    private String hazelcastClusterName;

    @Value("${spring.hazelcast.client.network.cluster-members:localhost:5701}")
    private List<String> hazelcastClusterMembers;

    /**
     * Configures the HazelcastInstance bean to act as a client connecting to the
     * external Hazelcast server/cluster defined in Docker Compose.
     * This instance will be used by Spring Session.
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *     <li>The {@code @SpringSessionHazelcastInstance} annotation is crucial for Spring Session to
     *     identify which HazelcastInstance to use.</li>
     *     <li>Custom serializers are registered for Spring Security objects (CsrfToken, SecurityContext, Authority)
     *     to ensure proper serialization when stored in Hazelcast.</li>
     *     <li>User code deployment is enabled to push necessary Spring Session classes to the Hazelcast members,
     *     which is required for entry processors to function correctly if these classes are not already on the classpath.</li>
     * </ul>
     *
     * @return Configured HazelcastInstance client.
     */
    @Bean
    @SpringSessionHazelcastInstance
    public HazelcastInstance hazelcastInstance() {

        // Use ClientConfig for client-side configuration
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(hazelcastClusterName);

        AttributeConfig attributeConfig = new AttributeConfig()
                .setName(HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE)
                .setExtractorClassName(PrincipalNameExtractor.class.getName());

        // Map configuration for the session map
        MapConfig sessionMapConfig = new MapConfig(sessionMapName)
                .setTimeToLiveSeconds((int) sessionTimeout.toSeconds()) // Set TTL for sessions
                .setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.LRU)
                        .setSize(100000) // Max 100,000 sessions in memory
                        .setMaxSizePolicy(MaxSizePolicy.PER_NODE))
                .addAttributeConfig(attributeConfig) // Add principal name attribute for indexing
                .addIndexConfig( // Add index for efficient principal name lookups
                        new IndexConfig(IndexType.HASH, HazelcastIndexedSessionRepository.PRINCIPAL_NAME_ATTRIBUTE));

        if (CollectionUtils.isEmpty(hazelcastClusterMembers)) {
            log.error("Hazelcast cluster members not configured (spring.hazelcast.client.network.cluster-members). Cannot create client.");
            throw new IllegalStateException("Hazelcast cluster members must be configured.");
        }
        clientConfig.getNetworkConfig().addAddress(hazelcastClusterMembers.toArray(new String[0]));
        clientConfig.addMapConfig(sessionMapConfig); // Add the session map configuration to the client config


        // Configure custom serializers for Spring Security objects
        clientConfig.getSerializationConfig()
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(new CsrfTokenSerializer())
                        .setTypeClass(DefaultCsrfToken.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(new SecurityContextSerializer())
                        .setTypeClass(SecurityContextImpl.class))
                .addSerializerConfig(new SerializerConfig()
                        .setImplementation(new AuthoritySerializer())
                        .setTypeClass(Authority.class));


        // Configure a custom serializer for the session map itself.
        // This is crucial for performance and proper serialization of MapSession objects.
        // All members in a cluster and connected clients must use the same serializer for sessions.
        // If the server-side Hazelcast cluster does not have this serializer configured, issues will arise.
        SerializerConfig sessionSerializerConfig = new SerializerConfig();
        sessionSerializerConfig.setImplementation(new HazelcastSessionSerializer()).setTypeClass(MapSession.class);
        clientConfig.getSerializationConfig().addSerializerConfig(sessionSerializerConfig);


        // Configure connection strategy
        clientConfig.getConnectionStrategyConfig()
                .setAsyncStart(false)
                .setReconnectMode(ClientConnectionStrategyConfig.ReconnectMode.ON)
                .getConnectionRetryConfig()
                .setInitialBackoffMillis(1000)
                .setMaxBackoffMillis(30000)
                .setMultiplier(2.0)
                .setClusterConnectTimeoutMillis(5000)
                .setJitter(0.2);

        // --- Use the deprecated but available ClientUserCodeDeploymentConfig ---
        // This is the correct API on the ClientConfig to send classes to the server.
        // If spring-session packages do not present in Hazelcast member's classpath,
        // these classes need to be deployed over the client. This is required since
        // Hazelcast updates sessions via entry processors.
        ClientUserCodeDeploymentConfig userCodeDeploymentConfig = new ClientUserCodeDeploymentConfig();
        userCodeDeploymentConfig.setEnabled(true)
                // Add your custom serializers
                .addClass(CsrfTokenSerializer.class)
                .addClass(SecurityContextSerializer.class)
                .addClass(AuthoritySerializer.class)
                // Add Spring Session classes required by the server for entry processing
                .addClass(Session.class)
                .addClass(MapSession.class)
                .addClass(SessionUpdateEntryProcessor.class);

        clientConfig.setUserCodeDeploymentConfig(userCodeDeploymentConfig);

        log.info("Configuring Hazelcast Client: clusterName='{}', members='{}'", hazelcastClusterName, hazelcastClusterMembers);
        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Provides a custom HazelcastSessionSerializer that uses the application's
     * configured ObjectMapper for serializing session attributes to JSON.
     *
     * <p><b>Usage:</b></p>
     * This bean is automatically picked up by Spring Session's Hazelcast integration
     * to handle the serialization and deserialization of {@link MapSession} objects.
     *
     * @return HazelcastSessionSerializer for JSON.
     */
    @Bean
    public HazelcastSessionSerializer hazelcastSessionSerializer() {
        log.info("Creating HazelcastSessionSerializer with default configuration.");
        return new HazelcastSessionSerializer();
    }

    /**
     * Customizes the HazelcastIndexedSessionRepository settings.
     *
     * <p><b>Important Notes:</b></p>
     * <ul>
     *     <li>{@code FlushMode.IMMEDIATE}: Ensures session changes are written to Hazelcast immediately.</li>
     *     <li>{@code SaveMode.ALWAYS}: Ensures the session is always saved to Hazelcast, even if no attributes change.</li>
     *     <li>The session map name and default max inactive interval are configured from application properties.</li>
     * </ul>
     * @return SessionRepositoryCustomizer for HazelcastIndexedSessionRepository.
     */
    @Bean
    public SessionRepositoryCustomizer<HazelcastIndexedSessionRepository> customizeHazelcastSessionRepository() {
        return (sessionRepository) -> {
            sessionRepository.setFlushMode(FlushMode.IMMEDIATE);
            sessionRepository.setSaveMode(SaveMode.ALWAYS);
            sessionRepository.setSessionMapName(sessionMapName);
            sessionRepository.setDefaultMaxInactiveInterval(sessionTimeout);

            log.info("Customized HazelcastIndexedSessionRepository: mapName='{}', timeout={}, flushMode={}, saveMode={}",
                    sessionMapName, sessionTimeout, FlushMode.IMMEDIATE, SaveMode.ALWAYS);
        };
    }

    /**
     * Customizes the session cookie configuration.
     *
     * <p><b>Security and Performance Considerations:</b></p>
     * <ul>
     *     <li>{@code useSecureCookie}: Set to true in production to ensure cookies are only sent over HTTPS.</li>
     *     <li>{@code useHttpOnlyCookie}: Set to true to prevent client-side scripts from accessing the cookie, mitigating XSS attacks.</li>
     *     <li>{@code SameSite=Lax}: Provides a good balance between security (CSRF protection) and usability.</li>
     *     <li>{@code cookieMaxAge}: Aligns with the session timeout to ensure cookie expiration matches session expiration.</li>
     *     <li>{@code useBase64Encoding}: Encodes the session ID in Base64, which can be useful for certain environments or proxies.</li>
     * </ul>
     * @return A configured CookieSerializer.
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("AMLUAUTHJSESSIONID");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(this.useSecureCookie);
        serializer.setSameSite("Lax");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        serializer.setCookieMaxAge((int) sessionTimeout.toSeconds());
        serializer.setUseBase64Encoding(true);
        serializer.setRememberMeRequestAttribute(REMEMBER_ME_PARAMETER);

        log.info("Custom CookieSerializer configured: Name=AMLUAUTHJSESSIONID, Secure={}, SameSite=Lax, MaxAge={}s.",
                this.useSecureCookie, sessionTimeout.toSeconds());
        return serializer;
    }
}

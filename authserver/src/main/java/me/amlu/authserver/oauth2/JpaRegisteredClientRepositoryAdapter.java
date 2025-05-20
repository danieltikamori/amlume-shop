/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.amlu.authserver.oauth2.model.RegisteredClient;
import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.oauth2.repository.RegisteredClientRepository;
import me.amlu.authserver.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient.Builder;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class JpaRegisteredClientRepositoryAdapter implements org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository {

    private final RegisteredClientRepository jpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;

    public JpaRegisteredClientRepositoryAdapter(RegisteredClientRepository jpaRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, UserRepository userRepository) {
        Assert.notNull(jpaRepository, "jpaRepository cannot be null");
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        Assert.notNull(authorityRepository, "authorityRepository cannot be null");
        Assert.notNull(userRepository, "userRepository cannot be null");
        this.jpaRepository = jpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.userRepository = userRepository;

        // Configure ObjectMapper with necessary modules for ClientSettings and TokenSettings
        ClassLoader classLoader = JpaRegisteredClientRepositoryAdapter.class.getClassLoader();
        List<Module> securityModules = org.springframework.security.jackson2.SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    @Transactional
    public void save(org.springframework.security.oauth2.server.authorization.client.RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        this.jpaRepository.save(toEntity(registeredClient));
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.security.oauth2.server.authorization.client.RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return this.jpaRepository.findById(id).map(this::toRegisteredClient).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.security.oauth2.server.authorization.client.RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return this.jpaRepository.findByClientId(clientId).map(this::toRegisteredClient).orElse(null);
    }

    private org.springframework.security.oauth2.server.authorization.client.RegisteredClient toRegisteredClient(RegisteredClient entity) {
        Set<ClientAuthenticationMethod> clientAuthenticationMethods = entity.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::new)
                .collect(Collectors.toSet());
        Set<AuthorizationGrantType> authorizationGrantTypes = entity.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::new)
                .collect(Collectors.toSet());

        Builder builder = org.springframework.security.oauth2.server.authorization.client.RegisteredClient.withId(entity.getId())
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientSecret(entity.getClientSecret()) // Secret is already hashed if stored hashed
                .clientSecretExpiresAt(entity.getClientSecretExpiresAt())
                .clientName(entity.getClientName())
                .clientAuthenticationMethods(methods -> methods.addAll(clientAuthenticationMethods))
                .authorizationGrantTypes(grantTypes -> grantTypes.addAll(authorizationGrantTypes))
                .redirectUris(uris -> uris.addAll(entity.getRedirectUris()))
                .postLogoutRedirectUris(uris -> {
                    if (entity.getPostLogoutRedirectUris() != null) {
                        uris.addAll(entity.getPostLogoutRedirectUris());
                    }
                })
                .scopes(scopes -> scopes.addAll(entity.getScopes()));

        Map<String, Object> clientSettingsMap = parseMap(entity.getClientSettings());
        builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

        Map<String, Object> tokenSettingsMap = parseMap(entity.getTokenSettings());
        builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

        return builder.build();
    }

    private RegisteredClient toEntity(org.springframework.security.oauth2.server.authorization.client.RegisteredClient registeredClient) {
        RegisteredClient entity = new RegisteredClient(); // Your JPA entity
        entity.setId(registeredClient.getId());
        entity.setClientId(registeredClient.getClientId());
        entity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());

        if (StringUtils.hasText(registeredClient.getClientSecret())) {
            String secret = registeredClient.getClientSecret();
            // Only encode if it's not already in Spring Security's encoded format (e.g. "{bcrypt}...")
            if (!secret.startsWith("{") || !this.passwordEncoder.matches("", secret)) {
                entity.setClientSecret(this.passwordEncoder.encode(secret));
            } else {
                entity.setClientSecret(secret); // Assume already encoded
            }
        } else {
            entity.setClientSecret(null);
        }

        entity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
        entity.setClientName(registeredClient.getClientName());

        entity.setClientAuthenticationMethods(registeredClient.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.toSet()));
        entity.setAuthorizationGrantTypes(registeredClient.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.toSet()));
        entity.setRedirectUris(registeredClient.getRedirectUris());
        entity.setPostLogoutRedirectUris(registeredClient.getPostLogoutRedirectUris());
        entity.setScopes(registeredClient.getScopes());
        entity.setClientSettings(writeMap(registeredClient.getClientSettings().getSettings()));
        entity.setTokenSettings(writeMap(registeredClient.getTokenSettings().getSettings()));

        return entity;
    }

    private Map<String, Object> parseMap(String data) {
        try {
            if (!StringUtils.hasText(data)) {
                return Collections.emptyMap();
            }
            return this.objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Error parsing JSON string: " + data, ex);
        }
    }

    private String writeMap(Map<String, Object> data) {
        try {
            return this.objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Error writing map to JSON string", ex);
        }
    }

    public AuthorityRepository getAuthorityRepository() {
        return this.authorityRepository;
    }

    public UserRepository getUserRepository() {
        return this.userRepository;
    }
}

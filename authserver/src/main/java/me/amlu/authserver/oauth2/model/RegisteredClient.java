/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.oauth2.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "oauth2_registered_client", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id", "client_name"})
}, indexes = {
        @Index(name = "idx_client_id", columnList = "client_id"),
        @Index(name = "idx_client_name", columnList = "client_name"),
        @Index(name = "idx_client_secret_expires_at", columnList = "client_secret_expires_at"),

})
public class RegisteredClient {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "client_id", length = 100, unique = true, nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at", nullable = false)
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret", length = 200) // Store hashed secrets
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name", length = 200, unique = true, nullable = false)
    private String clientName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_authentication_methods", joinColumns = @JoinColumn(name = "registered_client_id"))
    @Column(name = "client_authentication_method", nullable = false, length = 100)
    private Set<String> clientAuthenticationMethods; // Store as String (e.g., "client_secret_basic")

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_authorization_grant_types", joinColumns = @JoinColumn(name = "registered_client_id"))
    @Column(name = "authorization_grant_type", nullable = false, length = 100)
    private Set<String> authorizationGrantTypes; // Store as String (e.g., "authorization_code")

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_redirect_uris", joinColumns = @JoinColumn(name = "registered_client_id"))
    // Do not set too high length, as 3072 bytes is the limit for MySQL InnoDB primary keys
    @Column(name = "redirect_uri", nullable = false, length = 255)
    private Set<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_post_logout_redirect_uris", joinColumns = @JoinColumn(name = "registered_client_id"))
    // Do not set too high length, as 3072 bytes is the limit for MySQL InnoDB primary keys
    @Column(name = "post_logout_redirect_uri", length = 255)
    private Set<String> postLogoutRedirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_client_scopes", joinColumns = @JoinColumn(name = "registered_client_id"))
    @Column(name = "scope", nullable = false, length = 100)
    private Set<String> scopes;

    @Lob
    @Column(columnDefinition = "TEXT") // Or VARCHAR(4000) depending on DB
    private String clientSettings; // JSON string for ClientSettings

    @Lob
    @Column(columnDefinition = "TEXT") // Or VARCHAR(4000) depending on DB
    private String tokenSettings; // JSON string for TokenSettings

    public RegisteredClient() {
    }

    public String getId() {
        return this.id;
    }

    public String getClientId() {
        return this.clientId;
    }

    public Instant getClientIdIssuedAt() {
        return this.clientIdIssuedAt;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public Instant getClientSecretExpiresAt() {
        return this.clientSecretExpiresAt;
    }

    public String getClientName() {
        return this.clientName;
    }

    public Set<String> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods;
    }

    public Set<String> getAuthorizationGrantTypes() {
        return this.authorizationGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return this.redirectUris;
    }

    public Set<String> getPostLogoutRedirectUris() {
        return this.postLogoutRedirectUris;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    public String getClientSettings() {
        return this.clientSettings;
    }

    public String getTokenSettings() {
        return this.tokenSettings;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientIdIssuedAt(Instant clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setClientSecretExpiresAt(Instant clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setClientAuthenticationMethods(Set<String> clientAuthenticationMethods) {
        this.clientAuthenticationMethods = clientAuthenticationMethods;
    }

    public void setAuthorizationGrantTypes(Set<String> authorizationGrantTypes) {
        this.authorizationGrantTypes = authorizationGrantTypes;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void setClientSettings(String clientSettings) {
        this.clientSettings = clientSettings;
    }

    public void setTokenSettings(String tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    // Other fields from your stub if they don't fit into ClientSettings/TokenSettings
    // private String clientUri; (Part of ClientSettings)
    // private String clientLogoUri; (Could be custom or part of clientName/description)
    // private String clientDescription; (Could be custom)
    // private String clientPolicyUri; (Part of ClientSettings)
}

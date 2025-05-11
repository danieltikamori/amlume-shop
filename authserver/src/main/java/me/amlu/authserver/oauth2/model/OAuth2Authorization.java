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
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "oauth2_authorization", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"registered_client_id", "principal_name"}),
}, indexes = {
        @Index(name = "idx_registered_client_id", columnList = "registered_client_id"),
        @Index(name = "idx_principal_name", columnList = "principal_name"),
})// Table name used by Spring Authorization Server JDBC schema
public class OAuth2Authorization {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "registered_client_id", length = 100, nullable = false)
    private String registeredClientId;

    @Column(name = "principal_name", length = 200, nullable = false)
    private String principalName;

    @Column(length = 100, nullable = false)
    private String authorizationGrantType; // e.g., "authorization_code", "client_credentials"

    @Lob
    @Column(columnDefinition = "TEXT", name = "authorized_scopes", length = 1000) // VARCHAR(1000) in default schema
    private String authorizedScopes; // Space-delimited set of scopes

    @Lob
    @Column(columnDefinition = "TEXT")
    private String attributes; // JSON representation of Map<String, Object>

    @Column(length = 500)
    private String state;

    @Lob
    @Column(columnDefinition = "TEXT", name = "authorization_code_value")
    private String authorizationCodeValue;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "authorization_code_metadata")
    private String authorizationCodeMetadata; // JSON

    @Lob
    @Column(columnDefinition = "TEXT", name = "access_token_value")
    private String accessTokenValue;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "access_token_metadata")
    private String accessTokenMetadata; // JSON
    @Column(length = 100, name = "access_token_type")
    private String accessTokenType; // e.g., "Bearer"
    @Lob
    @Column(columnDefinition = "TEXT", name = "access_token_scopes")
    private String accessTokenScopes; // Space-delimited set of scopes

    @Lob
    @Column(columnDefinition = "TEXT", name = "oidc_id_token_value")
    private String oidcIdTokenValue;
    private Instant oidcIdTokenIssuedAt;
    private Instant oidcIdTokenExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "oidc_id_token_metadata")
    private String oidcIdTokenMetadata; // JSON
    @Lob
    @Column(columnDefinition = "TEXT", name = "oidc_id_token_claims") // Default schema uses user_claims
    private String oidcIdTokenClaims; // JSON

    @Lob
    @Column(columnDefinition = "TEXT", name = "refresh_token_value")
    private String refreshTokenValue;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "refresh_token_metadata")
    private String refreshTokenMetadata; // JSON

    @Lob
    @Column(columnDefinition = "TEXT", name = "user_code_value")
    private String userCodeValue;
    private Instant userCodeIssuedAt;
    private Instant userCodeExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "user_code_metadata")
    private String userCodeMetadata; // JSON

    @Lob
    @Column(columnDefinition = "TEXT", name = "device_code_value")
    private String deviceCodeValue;
    private Instant deviceCodeIssuedAt;
    private Instant deviceCodeExpiresAt;
    @Lob
    @Column(columnDefinition = "TEXT", name = "device_code_metadata")
    private String deviceCodeMetadata; // JSON

    public OAuth2Authorization() {
    }

    public OAuth2Authorization(String id, String registeredClientId, String principalName, String authorizationGrantType, String authorizedScopes, String attributes, String state, String authorizationCodeValue, Instant authorizationCodeIssuedAt, Instant authorizationCodeExpiresAt, String authorizationCodeMetadata, String accessTokenValue, Instant accessTokenIssuedAt, Instant accessTokenExpiresAt, String accessTokenMetadata, String accessTokenType, String accessTokenScopes, String oidcIdTokenValue, Instant oidcIdTokenIssuedAt, Instant oidcIdTokenExpiresAt, String oidcIdTokenMetadata, String oidcIdTokenClaims, String refreshTokenValue, Instant refreshTokenIssuedAt, Instant refreshTokenExpiresAt, String refreshTokenMetadata, String userCodeValue, Instant userCodeIssuedAt, Instant userCodeExpiresAt, String userCodeMetadata, String deviceCodeValue, Instant deviceCodeIssuedAt, Instant deviceCodeExpiresAt, String deviceCodeMetadata) {
        this.id = id;
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
        this.authorizationGrantType = authorizationGrantType;
        this.authorizedScopes = authorizedScopes;
        this.attributes = attributes;
        this.state = state;
        this.authorizationCodeValue = authorizationCodeValue;
        this.authorizationCodeIssuedAt = authorizationCodeIssuedAt;
        this.authorizationCodeExpiresAt = authorizationCodeExpiresAt;
        this.authorizationCodeMetadata = authorizationCodeMetadata;
        this.accessTokenValue = accessTokenValue;
        this.accessTokenIssuedAt = accessTokenIssuedAt;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.accessTokenMetadata = accessTokenMetadata;
        this.accessTokenType = accessTokenType;
        this.accessTokenScopes = accessTokenScopes;
        this.oidcIdTokenValue = oidcIdTokenValue;
        this.oidcIdTokenIssuedAt = oidcIdTokenIssuedAt;
        this.oidcIdTokenExpiresAt = oidcIdTokenExpiresAt;
        this.oidcIdTokenMetadata = oidcIdTokenMetadata;
        this.oidcIdTokenClaims = oidcIdTokenClaims;
        this.refreshTokenValue = refreshTokenValue;
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.refreshTokenMetadata = refreshTokenMetadata;
        this.userCodeValue = userCodeValue;
        this.userCodeIssuedAt = userCodeIssuedAt;
        this.userCodeExpiresAt = userCodeExpiresAt;
        this.userCodeMetadata = userCodeMetadata;
        this.deviceCodeValue = deviceCodeValue;
        this.deviceCodeIssuedAt = deviceCodeIssuedAt;
        this.deviceCodeExpiresAt = deviceCodeExpiresAt;
        this.deviceCodeMetadata = deviceCodeMetadata;
    }

    public static OAuth2AuthorizationBuilder builder() {
        return new OAuth2AuthorizationBuilder();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OAuth2Authorization that = (OAuth2Authorization) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegisteredClientId() {
        return this.registeredClientId;
    }

    public void setRegisteredClientId(String registeredClientId) {
        this.registeredClientId = registeredClientId;
    }

    public String getPrincipalName() {
        return this.principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getAuthorizationGrantType() {
        return this.authorizationGrantType;
    }

    public void setAuthorizationGrantType(String authorizationGrantType) {
        this.authorizationGrantType = authorizationGrantType;
    }

    public String getAuthorizedScopes() {
        return this.authorizedScopes;
    }

    public void setAuthorizedScopes(String authorizedScopes) {
        this.authorizedScopes = authorizedScopes;
    }

    public String getAttributes() {
        return this.attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAuthorizationCodeValue() {
        return this.authorizationCodeValue;
    }

    public void setAuthorizationCodeValue(String authorizationCodeValue) {
        this.authorizationCodeValue = authorizationCodeValue;
    }

    public Instant getAuthorizationCodeIssuedAt() {
        return this.authorizationCodeIssuedAt;
    }

    public void setAuthorizationCodeIssuedAt(Instant authorizationCodeIssuedAt) {
        this.authorizationCodeIssuedAt = authorizationCodeIssuedAt;
    }

    public Instant getAuthorizationCodeExpiresAt() {
        return this.authorizationCodeExpiresAt;
    }

    public void setAuthorizationCodeExpiresAt(Instant authorizationCodeExpiresAt) {
        this.authorizationCodeExpiresAt = authorizationCodeExpiresAt;
    }

    public String getAuthorizationCodeMetadata() {
        return this.authorizationCodeMetadata;
    }

    public void setAuthorizationCodeMetadata(String authorizationCodeMetadata) {
        this.authorizationCodeMetadata = authorizationCodeMetadata;
    }

    public String getAccessTokenValue() {
        return this.accessTokenValue;
    }

    public void setAccessTokenValue(String accessTokenValue) {
        this.accessTokenValue = accessTokenValue;
    }

    public Instant getAccessTokenIssuedAt() {
        return this.accessTokenIssuedAt;
    }

    public void setAccessTokenIssuedAt(Instant accessTokenIssuedAt) {
        this.accessTokenIssuedAt = accessTokenIssuedAt;
    }

    public Instant getAccessTokenExpiresAt() {
        return this.accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(Instant accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public String getAccessTokenMetadata() {
        return this.accessTokenMetadata;
    }

    public void setAccessTokenMetadata(String accessTokenMetadata) {
        this.accessTokenMetadata = accessTokenMetadata;
    }

    public String getAccessTokenType() {
        return this.accessTokenType;
    }

    public void setAccessTokenType(String accessTokenType) {
        this.accessTokenType = accessTokenType;
    }

    public String getAccessTokenScopes() {
        return this.accessTokenScopes;
    }

    public void setAccessTokenScopes(String accessTokenScopes) {
        this.accessTokenScopes = accessTokenScopes;
    }

    public String getOidcIdTokenValue() {
        return this.oidcIdTokenValue;
    }

    public void setOidcIdTokenValue(String oidcIdTokenValue) {
        this.oidcIdTokenValue = oidcIdTokenValue;
    }

    public Instant getOidcIdTokenIssuedAt() {
        return this.oidcIdTokenIssuedAt;
    }

    public void setOidcIdTokenIssuedAt(Instant oidcIdTokenIssuedAt) {
        this.oidcIdTokenIssuedAt = oidcIdTokenIssuedAt;
    }

    public Instant getOidcIdTokenExpiresAt() {
        return this.oidcIdTokenExpiresAt;
    }

    public void setOidcIdTokenExpiresAt(Instant oidcIdTokenExpiresAt) {
        this.oidcIdTokenExpiresAt = oidcIdTokenExpiresAt;
    }

    public String getOidcIdTokenMetadata() {
        return this.oidcIdTokenMetadata;
    }

    public void setOidcIdTokenMetadata(String oidcIdTokenMetadata) {
        this.oidcIdTokenMetadata = oidcIdTokenMetadata;
    }

    public String getOidcIdTokenClaims() {
        return this.oidcIdTokenClaims;
    }

    public void setOidcIdTokenClaims(String oidcIdTokenClaims) {
        this.oidcIdTokenClaims = oidcIdTokenClaims;
    }

    public String getRefreshTokenValue() {
        return this.refreshTokenValue;
    }

    public void setRefreshTokenValue(String refreshTokenValue) {
        this.refreshTokenValue = refreshTokenValue;
    }

    public Instant getRefreshTokenIssuedAt() {
        return this.refreshTokenIssuedAt;
    }

    public void setRefreshTokenIssuedAt(Instant refreshTokenIssuedAt) {
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
    }

    public Instant getRefreshTokenExpiresAt() {
        return this.refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(Instant refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public String getRefreshTokenMetadata() {
        return this.refreshTokenMetadata;
    }

    public void setRefreshTokenMetadata(String refreshTokenMetadata) {
        this.refreshTokenMetadata = refreshTokenMetadata;
    }

    public String getUserCodeValue() {
        return this.userCodeValue;
    }

    public void setUserCodeValue(String userCodeValue) {
        this.userCodeValue = userCodeValue;
    }

    public Instant getUserCodeIssuedAt() {
        return this.userCodeIssuedAt;
    }

    public void setUserCodeIssuedAt(Instant userCodeIssuedAt) {
        this.userCodeIssuedAt = userCodeIssuedAt;
    }

    public Instant getUserCodeExpiresAt() {
        return this.userCodeExpiresAt;
    }

    public void setUserCodeExpiresAt(Instant userCodeExpiresAt) {
        this.userCodeExpiresAt = userCodeExpiresAt;
    }

    public String getUserCodeMetadata() {
        return this.userCodeMetadata;
    }

    public void setUserCodeMetadata(String userCodeMetadata) {
        this.userCodeMetadata = userCodeMetadata;
    }

    public String getDeviceCodeValue() {
        return this.deviceCodeValue;
    }

    public void setDeviceCodeValue(String deviceCodeValue) {
        this.deviceCodeValue = deviceCodeValue;
    }

    public Instant getDeviceCodeIssuedAt() {
        return this.deviceCodeIssuedAt;
    }

    public void setDeviceCodeIssuedAt(Instant deviceCodeIssuedAt) {
        this.deviceCodeIssuedAt = deviceCodeIssuedAt;
    }

    public Instant getDeviceCodeExpiresAt() {
        return this.deviceCodeExpiresAt;
    }

    public void setDeviceCodeExpiresAt(Instant deviceCodeExpiresAt) {
        this.deviceCodeExpiresAt = deviceCodeExpiresAt;
    }

    public String getDeviceCodeMetadata() {
        return this.deviceCodeMetadata;
    }

    public void setDeviceCodeMetadata(String deviceCodeMetadata) {
        this.deviceCodeMetadata = deviceCodeMetadata;
    }

    public String toString() {
        return "OAuth2Authorization(id=" + this.getId() + ", registeredClientId=" + this.getRegisteredClientId() + ", principalName=" + this.getPrincipalName() + ", authorizationGrantType=" + this.getAuthorizationGrantType() + ", authorizedScopes=" + this.getAuthorizedScopes() + ", attributes=" + this.getAttributes() + ", state=" + this.getState() + ", authorizationCodeValue=" + this.getAuthorizationCodeValue() + ", authorizationCodeIssuedAt=" + this.getAuthorizationCodeIssuedAt() + ", authorizationCodeExpiresAt=" + this.getAuthorizationCodeExpiresAt() + ", authorizationCodeMetadata=" + this.getAuthorizationCodeMetadata() + ", accessTokenValue=" + this.getAccessTokenValue() + ", accessTokenIssuedAt=" + this.getAccessTokenIssuedAt() + ", accessTokenExpiresAt=" + this.getAccessTokenExpiresAt() + ", accessTokenMetadata=" + this.getAccessTokenMetadata() + ", accessTokenType=" + this.getAccessTokenType() + ", accessTokenScopes=" + this.getAccessTokenScopes() + ", oidcIdTokenValue=" + this.getOidcIdTokenValue() + ", oidcIdTokenIssuedAt=" + this.getOidcIdTokenIssuedAt() + ", oidcIdTokenExpiresAt=" + this.getOidcIdTokenExpiresAt() + ", oidcIdTokenMetadata=" + this.getOidcIdTokenMetadata() + ", oidcIdTokenClaims=" + this.getOidcIdTokenClaims() + ", refreshTokenValue=" + this.getRefreshTokenValue() + ", refreshTokenIssuedAt=" + this.getRefreshTokenIssuedAt() + ", refreshTokenExpiresAt=" + this.getRefreshTokenExpiresAt() + ", refreshTokenMetadata=" + this.getRefreshTokenMetadata() + ", userCodeValue=" + this.getUserCodeValue() + ", userCodeIssuedAt=" + this.getUserCodeIssuedAt() + ", userCodeExpiresAt=" + this.getUserCodeExpiresAt() + ", userCodeMetadata=" + this.getUserCodeMetadata() + ", deviceCodeValue=" + this.getDeviceCodeValue() + ", deviceCodeIssuedAt=" + this.getDeviceCodeIssuedAt() + ", deviceCodeExpiresAt=" + this.getDeviceCodeExpiresAt() + ", deviceCodeMetadata=" + this.getDeviceCodeMetadata() + ")";
    }

    public static class OAuth2AuthorizationBuilder {
        private String id;
        private String registeredClientId;
        private String principalName;
        private String authorizationGrantType;
        private String authorizedScopes;
        private String attributes;
        private String state;
        private String authorizationCodeValue;
        private Instant authorizationCodeIssuedAt;
        private Instant authorizationCodeExpiresAt;
        private String authorizationCodeMetadata;
        private String accessTokenValue;
        private Instant accessTokenIssuedAt;
        private Instant accessTokenExpiresAt;
        private String accessTokenMetadata;
        private String accessTokenType;
        private String accessTokenScopes;
        private String oidcIdTokenValue;
        private Instant oidcIdTokenIssuedAt;
        private Instant oidcIdTokenExpiresAt;
        private String oidcIdTokenMetadata;
        private String oidcIdTokenClaims;
        private String refreshTokenValue;
        private Instant refreshTokenIssuedAt;
        private Instant refreshTokenExpiresAt;
        private String refreshTokenMetadata;
        private String userCodeValue;
        private Instant userCodeIssuedAt;
        private Instant userCodeExpiresAt;
        private String userCodeMetadata;
        private String deviceCodeValue;
        private Instant deviceCodeIssuedAt;
        private Instant deviceCodeExpiresAt;
        private String deviceCodeMetadata;

        OAuth2AuthorizationBuilder() {
        }

        public OAuth2AuthorizationBuilder id(String id) {
            this.id = id;
            return this;
        }

        public OAuth2AuthorizationBuilder registeredClientId(String registeredClientId) {
            this.registeredClientId = registeredClientId;
            return this;
        }

        public OAuth2AuthorizationBuilder principalName(String principalName) {
            this.principalName = principalName;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizationGrantType(String authorizationGrantType) {
            this.authorizationGrantType = authorizationGrantType;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizedScopes(String authorizedScopes) {
            this.authorizedScopes = authorizedScopes;
            return this;
        }

        public OAuth2AuthorizationBuilder attributes(String attributes) {
            this.attributes = attributes;
            return this;
        }

        public OAuth2AuthorizationBuilder state(String state) {
            this.state = state;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizationCodeValue(String authorizationCodeValue) {
            this.authorizationCodeValue = authorizationCodeValue;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizationCodeIssuedAt(Instant authorizationCodeIssuedAt) {
            this.authorizationCodeIssuedAt = authorizationCodeIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizationCodeExpiresAt(Instant authorizationCodeExpiresAt) {
            this.authorizationCodeExpiresAt = authorizationCodeExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder authorizationCodeMetadata(String authorizationCodeMetadata) {
            this.authorizationCodeMetadata = authorizationCodeMetadata;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenValue(String accessTokenValue) {
            this.accessTokenValue = accessTokenValue;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenIssuedAt(Instant accessTokenIssuedAt) {
            this.accessTokenIssuedAt = accessTokenIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenExpiresAt(Instant accessTokenExpiresAt) {
            this.accessTokenExpiresAt = accessTokenExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenMetadata(String accessTokenMetadata) {
            this.accessTokenMetadata = accessTokenMetadata;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenType(String accessTokenType) {
            this.accessTokenType = accessTokenType;
            return this;
        }

        public OAuth2AuthorizationBuilder accessTokenScopes(String accessTokenScopes) {
            this.accessTokenScopes = accessTokenScopes;
            return this;
        }

        public OAuth2AuthorizationBuilder oidcIdTokenValue(String oidcIdTokenValue) {
            this.oidcIdTokenValue = oidcIdTokenValue;
            return this;
        }

        public OAuth2AuthorizationBuilder oidcIdTokenIssuedAt(Instant oidcIdTokenIssuedAt) {
            this.oidcIdTokenIssuedAt = oidcIdTokenIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder oidcIdTokenExpiresAt(Instant oidcIdTokenExpiresAt) {
            this.oidcIdTokenExpiresAt = oidcIdTokenExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder oidcIdTokenMetadata(String oidcIdTokenMetadata) {
            this.oidcIdTokenMetadata = oidcIdTokenMetadata;
            return this;
        }

        public OAuth2AuthorizationBuilder oidcIdTokenClaims(String oidcIdTokenClaims) {
            this.oidcIdTokenClaims = oidcIdTokenClaims;
            return this;
        }

        public OAuth2AuthorizationBuilder refreshTokenValue(String refreshTokenValue) {
            this.refreshTokenValue = refreshTokenValue;
            return this;
        }

        public OAuth2AuthorizationBuilder refreshTokenIssuedAt(Instant refreshTokenIssuedAt) {
            this.refreshTokenIssuedAt = refreshTokenIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder refreshTokenExpiresAt(Instant refreshTokenExpiresAt) {
            this.refreshTokenExpiresAt = refreshTokenExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder refreshTokenMetadata(String refreshTokenMetadata) {
            this.refreshTokenMetadata = refreshTokenMetadata;
            return this;
        }

        public OAuth2AuthorizationBuilder userCodeValue(String userCodeValue) {
            this.userCodeValue = userCodeValue;
            return this;
        }

        public OAuth2AuthorizationBuilder userCodeIssuedAt(Instant userCodeIssuedAt) {
            this.userCodeIssuedAt = userCodeIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder userCodeExpiresAt(Instant userCodeExpiresAt) {
            this.userCodeExpiresAt = userCodeExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder userCodeMetadata(String userCodeMetadata) {
            this.userCodeMetadata = userCodeMetadata;
            return this;
        }

        public OAuth2AuthorizationBuilder deviceCodeValue(String deviceCodeValue) {
            this.deviceCodeValue = deviceCodeValue;
            return this;
        }

        public OAuth2AuthorizationBuilder deviceCodeIssuedAt(Instant deviceCodeIssuedAt) {
            this.deviceCodeIssuedAt = deviceCodeIssuedAt;
            return this;
        }

        public OAuth2AuthorizationBuilder deviceCodeExpiresAt(Instant deviceCodeExpiresAt) {
            this.deviceCodeExpiresAt = deviceCodeExpiresAt;
            return this;
        }

        public OAuth2AuthorizationBuilder deviceCodeMetadata(String deviceCodeMetadata) {
            this.deviceCodeMetadata = deviceCodeMetadata;
            return this;
        }

        public OAuth2Authorization build() {
            return new OAuth2Authorization(this.id, this.registeredClientId, this.principalName, this.authorizationGrantType, this.authorizedScopes, this.attributes, this.state, this.authorizationCodeValue, this.authorizationCodeIssuedAt, this.authorizationCodeExpiresAt, this.authorizationCodeMetadata, this.accessTokenValue, this.accessTokenIssuedAt, this.accessTokenExpiresAt, this.accessTokenMetadata, this.accessTokenType, this.accessTokenScopes, this.oidcIdTokenValue, this.oidcIdTokenIssuedAt, this.oidcIdTokenExpiresAt, this.oidcIdTokenMetadata, this.oidcIdTokenClaims, this.refreshTokenValue, this.refreshTokenIssuedAt, this.refreshTokenExpiresAt, this.refreshTokenMetadata, this.userCodeValue, this.userCodeIssuedAt, this.userCodeExpiresAt, this.userCodeMetadata, this.deviceCodeValue, this.deviceCodeIssuedAt, this.deviceCodeExpiresAt, this.deviceCodeMetadata);
        }

        public String toString() {
            return "OAuth2Authorization.OAuth2AuthorizationBuilder(id=" + this.id + ", registeredClientId=" + this.registeredClientId + ", principalName=" + this.principalName + ", authorizationGrantType=" + this.authorizationGrantType + ", authorizedScopes=" + this.authorizedScopes + ", attributes=" + this.attributes + ", state=" + this.state + ", authorizationCodeValue=" + this.authorizationCodeValue + ", authorizationCodeIssuedAt=" + this.authorizationCodeIssuedAt + ", authorizationCodeExpiresAt=" + this.authorizationCodeExpiresAt + ", authorizationCodeMetadata=" + this.authorizationCodeMetadata + ", accessTokenValue=" + this.accessTokenValue + ", accessTokenIssuedAt=" + this.accessTokenIssuedAt + ", accessTokenExpiresAt=" + this.accessTokenExpiresAt + ", accessTokenMetadata=" + this.accessTokenMetadata + ", accessTokenType=" + this.accessTokenType + ", accessTokenScopes=" + this.accessTokenScopes + ", oidcIdTokenValue=" + this.oidcIdTokenValue + ", oidcIdTokenIssuedAt=" + this.oidcIdTokenIssuedAt + ", oidcIdTokenExpiresAt=" + this.oidcIdTokenExpiresAt + ", oidcIdTokenMetadata=" + this.oidcIdTokenMetadata + ", oidcIdTokenClaims=" + this.oidcIdTokenClaims + ", refreshTokenValue=" + this.refreshTokenValue + ", refreshTokenIssuedAt=" + this.refreshTokenIssuedAt + ", refreshTokenExpiresAt=" + this.refreshTokenExpiresAt + ", refreshTokenMetadata=" + this.refreshTokenMetadata + ", userCodeValue=" + this.userCodeValue + ", userCodeIssuedAt=" + this.userCodeIssuedAt + ", userCodeExpiresAt=" + this.userCodeExpiresAt + ", userCodeMetadata=" + this.userCodeMetadata + ", deviceCodeValue=" + this.deviceCodeValue + ", deviceCodeIssuedAt=" + this.deviceCodeIssuedAt + ", deviceCodeExpiresAt=" + this.deviceCodeExpiresAt + ", deviceCodeMetadata=" + this.deviceCodeMetadata + ")";
        }
    }
}
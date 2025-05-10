/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.model.oauth2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "oauth2_authorization") // Table name used by Spring Authorization Server JDBC schema
@Getter
@Setter
@NoArgsConstructor
public class OAuth2Authorization {

    @Id
    @Column(length = 100)
    private String id;

    @Column(length = 100, nullable = false)
    private String registeredClientId;

    @Column(length = 200, nullable = false)
    private String principalName;

    @Column(length = 100, nullable = false)
    private String authorizationGrantType; // e.g., "authorization_code", "client_credentials"

    @Lob
    @Column(columnDefinition = "TEXT", name = "authorized_scopes") // VARCHAR(1000) in default schema
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
}
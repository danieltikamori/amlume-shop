/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- V1: Create the complete initial database schema for the Amlume Authserver.

-- 1. Enable Extensions
CREATE EXTENSION IF NOT EXISTS ltree;

-- 2. Create 'users' table
CREATE TABLE users
(
    user_id                    BIGSERIAL PRIMARY KEY,
    external_id                VARCHAR(255)             NOT NULL UNIQUE,
    given_name                 VARCHAR(127)             NOT NULL,
    middle_name                VARCHAR(127),
    surname                    BYTEA,                                    -- Encrypted
    nickname                   VARCHAR(127) UNIQUE,
    email                      VARCHAR(255)             NOT NULL UNIQUE, -- Or BYTEA if encrypted + blind index
    -- email_blind_index    VARCHAR(64) UNIQUE, -- If primary email is encrypted
    recovery_email_encrypted   BYTEA UNIQUE,
    recovery_email_blind_index VARCHAR(64) UNIQUE,
    profile_picture_url        VARCHAR(2046),
    mobile_number_encrypted    BYTEA UNIQUE,
    password                   VARCHAR(127),                             -- Hashed
    email_verified             BOOLEAN                  NOT NULL,
    enabled                    BOOLEAN                  NOT NULL,
    failed_login_attempts      INTEGER                           DEFAULT 0,
    lockout_expiration_time    TIMESTAMP WITH TIME ZONE,
    account_non_expired        BOOLEAN                  NOT NULL,
    credentials_non_expired    BOOLEAN                  NOT NULL,
    last_login_at              TIMESTAMP WITH TIME ZONE,
    last_password_change_date  TIMESTAMP WITH TIME ZONE,
    deleted_at                 TIMESTAMP WITH TIME ZONE,
    version                    INTEGER                           DEFAULT 0 NOT NULL,
    -- Audit columns
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT,
    last_modified_by           BIGINT
);

-- Indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_external_id ON users (external_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email); -- Or on email_blind_index
CREATE INDEX IF NOT EXISTS idx_recovery_email ON users (recovery_email_encrypted);
CREATE INDEX IF NOT EXISTS idx_users_recovery_email_blind_index ON users (recovery_email_blind_index);
-- CREATE INDEX IF NOT EXISTS idx_given_name ON users (given_name);
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users (nickname);

-- Optional indexes
CREATE INDEX IF NOT EXISTS idx_mobile_number ON users (mobile_number_encrypted);
CREATE INDEX IF NOT EXISTS idx_created_at ON users (created_at);
CREATE INDEX IF NOT EXISTS idx_updated_at ON users (updated_at);
CREATE INDEX IF NOT EXISTS idx_last_login_at ON users (last_login_at);
-- CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);

-- -- Optional: Add foreign key constraints if 'createdBy' and 'lastModifiedBy' reference user_id
-- ALTER TABLE users
--     ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);
-- ALTER TABLE users
--     ADD CONSTRAINT fk_users_modified_by FOREIGN KEY (last_modified_by) REFERENCES users (user_id);

-- 3. Create 'roles' table with hierarchy support
CREATE TABLE roles
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    description      VARCHAR(255),
    path             LTREE        NOT NULL,
    parent_id        BIGINT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    last_modified_by BIGINT,
    FOREIGN KEY (parent_id) REFERENCES roles (id) ON DELETE SET NULL
);

CREATE INDEX roles_path_idx ON roles USING GIST (path);
CREATE INDEX roles_parent_id_idx ON roles (parent_id);

-- UPDATE: Authority replaced by Role
-- -- 2. Create roles table (for roles like ROLE_USER, ROLE_ADMIN)
-- CREATE TABLE authorities
-- (
--     id               BIGSERIAL PRIMARY KEY,
--     authority        VARCHAR(255)             NOT NULL UNIQUE,
--     -- Audit columns
--     created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     created_by       BIGINT,
--     last_modified_by BIGINT
-- );

-- 4. Create 'permissions' table
CREATE TABLE permissions
(
    id         CHAR(26) PRIMARY KEY, -- Changed from UUID to CHAR(13) for TSID.
    -- Related to role_permissions.permission_id
    name             VARCHAR(255)             NOT NULL UNIQUE,
    description      TEXT,
    -- Audit columns (assuming PermissionsEntity extends AbstractAuditableEntity or similar)
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,               -- Assuming created_by/last_modified_by are Long in AbstractAuditableEntity
    last_modified_by BIGINT
);

-- 5. Create join tables for Users, Roles, and Permissions
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);
CREATE INDEX user_roles_role_id_idx ON user_roles (role_id);

-- -- 4. Create user_authorities join table (User <-> Authority)
-- CREATE TABLE user_authorities
-- (
--     user_id      BIGINT NOT NULL,
--     authority_id BIGINT NOT NULL,
--     PRIMARY KEY (user_id, authority_id),
--     CONSTRAINT fk_user_authorities_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
--     CONSTRAINT fk_user_authorities_authority FOREIGN KEY (authority_id) REFERENCES authorities (id) ON DELETE CASCADE
-- );
-- CREATE INDEX idx_user_authorities_authority_id ON user_authorities (authority_id);
-- -- For finding users by role

-- 5. Create role_permissions join table (Authority <-> PermissionsEntity)
CREATE TABLE role_permissions
(
    role_id       BIGINT   NOT NULL, -- Corresponds to Authority.id
    permission_id CHAR(26) NOT NULL, -- Corresponds to PermissionsEntity.id. ALWAYS check correspondence
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);
-- For finding roles by permission

-- 6. Create persistent_logins table (for Remember-Me)
CREATE TABLE persistent_logins
(
    username  VARCHAR(255)             NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64)              NOT NULL,
    last_used TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_persistent_logins_username ON persistent_logins (username);

-- 7. Create passkey_credentials table (from your PasskeyCredential entity)
CREATE TABLE passkey_credentials
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT                   NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    user_handle        VARCHAR(255)             NOT NULL,        -- Typically User.externalId
    friendly_name      VARCHAR(255),
    credential_type    VARCHAR(50),                              -- e.g., "public-key"
    credential_id      VARCHAR(512)             NOT NULL UNIQUE, -- Base64URL encoded credential ID from authenticator
    public_key_cose    BYTEA                    NOT NULL,        -- COSE-encoded public key (encrypted)
    signature_count    BIGINT                   NOT NULL,
    uv_initialized     BOOLEAN,                                  -- User Verification initialized
    transports         VARCHAR(100),                             -- Comma-separated list of transports (e.g., "internal,usb")
    backup_eligible    BOOLEAN,
    backup_state       BOOLEAN,
    attestation_object BYTEA,                                    -- Attestation object (encrypted), may not be stored long-term
    last_used_at       TIMESTAMP WITH TIME ZONE,
    -- Audit columns from AbstractAuditableEntity
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         BIGINT,
    last_modified_by   BIGINT,

    CONSTRAINT fk_passkey_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_passkey_user_id ON passkey_credentials (user_id);
CREATE INDEX idx_passkey_credential_id ON passkey_credentials (credential_id);
CREATE INDEX idx_passkey_user_handle ON passkey_credentials (user_handle);

-- 8. Create oauth2_registered_client table (from the RegisteredClient entity)
CREATE TABLE oauth2_registered_client
(
    id                       VARCHAR(100) PRIMARY KEY,
    client_id                VARCHAR(100)             NOT NULL UNIQUE,
    client_id_issued_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    client_secret            VARCHAR(200),
    client_secret_expires_at TIMESTAMP WITH TIME ZONE,
    client_name              VARCHAR(200)             NOT NULL,
    client_settings          TEXT,
    token_settings           TEXT,
    -- Audit columns
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT,
    last_modified_by         BIGINT
);
CREATE INDEX idx_oauth2_reg_client_client_id ON oauth2_registered_client (client_id);

-- Element collections for oauth2_registered_client
CREATE TABLE oauth2_client_authentication_methods
(
    registered_client_id         VARCHAR(100) NOT NULL,
    client_authentication_method VARCHAR(100) NOT NULL,
    PRIMARY KEY (registered_client_id, client_authentication_method),
    CONSTRAINT fk_oauth2_cam_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);

CREATE TABLE oauth2_authorization_grant_types
(
    registered_client_id     VARCHAR(100) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    PRIMARY KEY (registered_client_id, authorization_grant_type),
    CONSTRAINT fk_oauth2_agt_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);

CREATE TABLE oauth2_redirect_uris
(
    registered_client_id VARCHAR(100)  NOT NULL,
    redirect_uri         VARCHAR(1000) NOT NULL, -- Spring default is 1000, your entity has 255
    PRIMARY KEY (registered_client_id, redirect_uri),
    CONSTRAINT fk_oauth2_ru_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);

CREATE TABLE oauth2_post_logout_redirect_uris
(
    registered_client_id     VARCHAR(100)  NOT NULL,
    post_logout_redirect_uri VARCHAR(1000) NOT NULL, -- Spring default is 1000, your entity has 255
    PRIMARY KEY (registered_client_id, post_logout_redirect_uri),
    CONSTRAINT fk_oauth2_plru_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);

CREATE TABLE oauth2_client_scopes
(
    registered_client_id VARCHAR(100) NOT NULL,
    scope                VARCHAR(100) NOT NULL,
    PRIMARY KEY (registered_client_id, scope),
    CONSTRAINT fk_oauth2_cs_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);


-- 9. Create oauth2_authorization table (from your OAuth2Authorization entity)
CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100) PRIMARY KEY,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             TEXT,
    attributes                    TEXT,
    state                         VARCHAR(500),
    authorization_code_value      TEXT,
    authorization_code_issued_at  TIMESTAMP WITH TIME ZONE,
    authorization_code_expires_at TIMESTAMP WITH TIME ZONE,
    authorization_code_metadata   TEXT,
    access_token_value            TEXT,
    access_token_issued_at        TIMESTAMP WITH TIME ZONE,
    access_token_expires_at       TIMESTAMP WITH TIME ZONE,
    access_token_metadata         TEXT,
    access_token_type             VARCHAR(100),
    access_token_scopes           TEXT,
    oidc_id_token_value           TEXT,
    oidc_id_token_issued_at       TIMESTAMP WITH TIME ZONE,
    oidc_id_token_expires_at      TIMESTAMP WITH TIME ZONE,
    oidc_id_token_metadata        TEXT,
    oidc_id_token_claims          TEXT,
    refresh_token_value           TEXT,
    refresh_token_issued_at       TIMESTAMP WITH TIME ZONE,
    refresh_token_expires_at      TIMESTAMP WITH TIME ZONE,
    refresh_token_metadata        TEXT,
    user_code_value               TEXT,
    user_code_issued_at           TIMESTAMP WITH TIME ZONE,
    user_code_expires_at          TIMESTAMP WITH TIME ZONE,
    user_code_metadata            TEXT,
    device_code_value             TEXT,
    device_code_issued_at         TIMESTAMP WITH TIME ZONE,
    device_code_expires_at        TIMESTAMP WITH TIME ZONE,
    device_code_metadata          TEXT,
    CONSTRAINT fk_oauth2_auth_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
    -- Consider index on principal_name if you query by it often for cleanups
);
-- Indexes for oauth2_authorization (based on Spring Authorization Server's default JDBC schema)
CREATE INDEX idx_oauth2_auth_reg_client_id ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_auth_principal_name ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_auth_auth_grant_type ON oauth2_authorization (authorization_grant_type);
CREATE INDEX idx_oauth2_auth_auth_code_val ON oauth2_authorization (authorization_code_value);
CREATE INDEX idx_oauth2_auth_access_token_val ON oauth2_authorization (access_token_value);
CREATE INDEX idx_oauth2_auth_refresh_token_val ON oauth2_authorization (refresh_token_value);
CREATE INDEX idx_oauth2_auth_user_code_val ON oauth2_authorization (user_code_value);
CREATE INDEX idx_oauth2_auth_device_code_val ON oauth2_authorization (device_code_value);
CREATE INDEX idx_oauth2_auth_state ON oauth2_authorization (state);


/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */


-- 10. Create oauth2_authorization_consent table (from your OAuth2AuthorizationConsent entity)
CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name       VARCHAR(200) NOT NULL,
    authorities          TEXT         NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name),
    CONSTRAINT fk_oauth2_consent_client FOREIGN KEY (registered_client_id) REFERENCES oauth2_registered_client (id) ON DELETE CASCADE
);
-- The PRIMARY KEY (registered_client_id, principal_name) automatically creates a unique index
-- that covers lookups by (registered_client_id, principal_name) and by (registered_client_id).
-- An additional index on principal_name might be beneficial if you query frequently by principal_name only.
CREATE INDEX IF NOT EXISTS idx_oauth2_auth_consent_principal_name ON oauth2_authorization_consent (principal_name);

-- 11. Create user_device_fingerprint table
CREATE TABLE user_device_fingerprint
(
    user_device_fingerprint_id BIGSERIAL PRIMARY KEY,
    user_id                    BIGINT                   NOT NULL,
    device_fingerprint         VARCHAR(255)             NOT NULL UNIQUE, -- Hashed fingerprint
    last_used_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at             TIMESTAMP WITH TIME ZONE,
    failed_attempts            INTEGER                  NOT NULL DEFAULT 0,
    trusted                    BOOLEAN,
    active                     BOOLEAN                  NOT NULL DEFAULT TRUE,
    device_name                VARCHAR(255),
    last_known_ip              VARCHAR(255),
    update_count               INTEGER                  NOT NULL DEFAULT 0,
    location                   VARCHAR(255),
    last_known_country         VARCHAR(255),
    browser_info               VARCHAR(512)             NOT NULL,        -- User-Agent or derived info
    source                     VARCHAR(255),                             -- e.g., "Login Event", "Manual Registration"
    -- Audit columns
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT,
    last_modified_by           BIGINT,

    CONSTRAINT fk_user_device_fingerprint_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_device_fingerprint_user_id ON user_device_fingerprint (user_id);
CREATE INDEX IF NOT EXISTS idx_user_device_fingerprint_fingerprint ON user_device_fingerprint (device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_user_device_fingerprint_active ON user_device_fingerprint (active);
CREATE INDEX IF NOT EXISTS idx_user_device_fingerprint_trusted ON user_device_fingerprint (trusted);

-- 12. Create ASN_entries table
CREATE TABLE asn_entries
(
    ip               VARCHAR(45) PRIMARY KEY,
    asn              VARCHAR(255)             NOT NULL,
    -- Audit columns to match AbstractAuditableEntity
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    last_modified_by BIGINT
);
CREATE TABLE ip_blocklist
(
    id               BIGSERIAL,
    ip_address       VARCHAR(45) PRIMARY KEY,
    reason           TEXT,
    blocked_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blocked_until    TIMESTAMP WITH TIME ZONE,
    active           BOOLEAN                  NOT NULL,
    -- Audit columns to match AbstractAuditableEntity
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    last_modified_by BIGINT

);
CREATE TABLE ip_whitelist
(
    ip_address  VARCHAR(45) PRIMARY KEY,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ip_metadata
(
    id               BIGSERIAL PRIMARY KEY,
    ip_address       VARCHAR(45)              NOT NULL UNIQUE,
    suspicious_count INTEGER                  NOT NULL DEFAULT 0,
    last_geolocation VARCHAR(255)             NOT NULL,
    first_seen_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ttl         INTEGER,
    -- Audit columns to match AbstractAuditableEntity
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    last_modified_by BIGINT
);

CREATE TABLE revoked_tokens
(
    jti         VARCHAR(255) PRIMARY KEY,
    expiry_time TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE security_events
(
    id         BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50)              NOT NULL,
    user_id    BIGINT,
    username   VARCHAR(255),
    ip_address VARCHAR(45),
    timestamp  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details    TEXT
);

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

-- ... (after the CREATE TABLE security_events block)

-- 13. Create Element Collection tables for IpMetadataEntity
-- Table for storing historical geolocation strings for an IP
CREATE TABLE ip_previous_geolocations
(
    ip_metadata_id        BIGINT NOT NULL,
    previous_geolocations VARCHAR(255),
    CONSTRAINT fk_ip_prev_geo_ip_metadata FOREIGN KEY (ip_metadata_id) REFERENCES ip_metadata (id) ON DELETE CASCADE
);

-- Table for storing historical geolocation entries (location + timestamp) for an IP
CREATE TABLE ip_geo_history
(
    ip_metadata_id BIGINT NOT NULL,
    location       VARCHAR(255),
    timestamp      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_ip_geo_hist_ip_metadata FOREIGN KEY (ip_metadata_id) REFERENCES ip_metadata (id) ON DELETE CASCADE
);

-- Table for storing historical TTL values for an IP
CREATE TABLE ip_ttl_history
(
    ip_metadata_id BIGINT NOT NULL,
    ttl_history    INTEGER,
    CONSTRAINT fk_ip_ttl_hist_ip_metadata FOREIGN KEY (ip_metadata_id) REFERENCES ip_metadata (id) ON DELETE CASCADE
);

-- Final FK constraints that depend on 'users' table
-- ... (rest of the file)

-- Final FK constraints that depend on 'users' table
ALTER TABLE users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;
ALTER TABLE users
    ADD CONSTRAINT fk_users_modified_by FOREIGN KEY (last_modified_by) REFERENCES users (user_id) ON DELETE SET NULL;

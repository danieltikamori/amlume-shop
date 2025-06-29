/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

/**
 * Constants for configuration property keys used throughout the application.
 * This class centralizes configuration key definitions to ensure consistency.
 */
public final class ConfigKeys {
    // Application configuration
    public static final String APP_NAME = "app.name";
    public static final String APP_VERSION = "app.version";
    public static final String APP_ENVIRONMENT = "app.environment";
    public static final String APP_BASE_URL = "app.base-url";

    // Security configuration
    public static final String SECURITY_JWT_SECRET = "security.jwt.secret";
    public static final String SECURITY_JWT_ISSUER = "security.jwt.issuer";
    public static final String SECURITY_JWT_ACCESS_TOKEN_EXPIRATION = "security.jwt.access-token-expiration";
    public static final String SECURITY_JWT_REFRESH_TOKEN_EXPIRATION = "security.jwt.refresh-token-expiration";
    public static final String SECURITY_PASSWORD_ENCODER_STRENGTH = "security.password-encoder.strength";
    public static final String SECURITY_CORS_ALLOWED_ORIGINS = "security.cors.allowed-origins";
    public static final String SECURITY_CORS_ALLOWED_METHODS = "security.cors.allowed-methods";
    public static final String SECURITY_CORS_ALLOWED_HEADERS = "security.cors.allowed-headers";
    public static final String SECURITY_CORS_MAX_AGE = "security.cors.max-age";

    // Account lockout configuration
    public static final String SECURITY_ACCOUNT_LOCKOUT_ENABLED = "security.account-lockout.enabled";
    public static final String SECURITY_ACCOUNT_LOCKOUT_MAX_ATTEMPTS = "security.account-lockout.max-attempts";
    public static final String SECURITY_ACCOUNT_LOCKOUT_DURATION = "security.account-lockout.duration";

    // Rate limiting configuration
    public static final String SECURITY_RATE_LIMIT_ENABLED = "security.rate-limit.enabled";
    public static final String SECURITY_RATE_LIMIT_MAX_REQUESTS = "security.rate-limit.max-requests";
    public static final String SECURITY_RATE_LIMIT_WINDOW = "security.rate-limit.window";

    // WebAuthn configuration
    public static final String WEBAUTHN_RP_ID = "webauthn.rp.id";
    public static final String WEBAUTHN_RP_NAME = "webauthn.rp.name";
    public static final String WEBAUTHN_RP_ICON = "webauthn.rp.icon";
    public static final String WEBAUTHN_ATTESTATION = "webauthn.attestation";
    public static final String WEBAUTHN_AUTHENTICATOR_ATTACHMENT = "webauthn.authenticator-attachment";
    public static final String WEBAUTHN_REQUIRE_RESIDENT_KEY = "webauthn.require-resident-key";
    public static final String WEBAUTHN_USER_VERIFICATION = "webauthn.user-verification";

    // Database configuration
    public static final String DB_URL = "spring.datasource.url";
    public static final String DB_USERNAME = "spring.datasource.username";
    public static final String DB_PASSWORD = "spring.datasource.password";
    public static final String DB_DRIVER = "spring.datasource.driver-class-name";

    // Redis configuration
    public static final String REDIS_HOST = "spring.redis.host";
    public static final String REDIS_PORT = "spring.redis.port";
    public static final String REDIS_PASSWORD = "spring.redis.password";

    // Email configuration
    public static final String EMAIL_HOST = "spring.mail.host";
    public static final String EMAIL_PORT = "spring.mail.port";
    public static final String EMAIL_USERNAME = "spring.mail.username";
    public static final String EMAIL_PASSWORD = "spring.mail.password";
    public static final String EMAIL_FROM = "app.email.from";
    public static final String EMAIL_ENABLED = "app.email.enabled";

    // Logging configuration
    public static final String LOG_LEVEL_ROOT = "logging.level.root";
    public static final String LOG_LEVEL_APP = "logging.level.me.amlu";
    public static final String LOG_FILE = "logging.file.name";

    private ConfigKeys() {
    } // Private constructor to prevent instantiation
}

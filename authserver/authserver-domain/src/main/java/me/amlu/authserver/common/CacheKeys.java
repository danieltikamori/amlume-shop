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
 * Constants for cache keys used throughout the application.
 * This class centralizes cache key definitions to ensure consistency
 * and prevent key collisions.
 */
public final class CacheKeys {

    private static final String CACHE_VERSION = "v1";

    // Authentication cache keys
    public static final String AUTH_CACHE = "authCache";
    public static final String AUTH_CACHE_KEY_PREFIX = "auth:";
    public static final String SESSION_CACHE = "sessionCache";
    public static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    public static final String BLOCKED_IP_PREFIX = "blocked:ip:";
    public static final String USER_ROLES_PREFIX = "user:roles:";

    // --- Cache Names for IP Security & Geolocation ---
    public static final String IP_BLOCK_CACHE = "ipBlockCache";
    public static final String IP_METADATA_CACHE = "ipMetadataCache";
    public static final String GEO_LOCATION_CACHE = "geoLocationCache";
    public static final String GEO_HISTORY_CACHE = "geoHistoryCache";

    // User cache keys
    public static final String USER_CACHE = "userCache";
    public static final String USER_CACHE_KEY_PREFIX = AUTH_CACHE_KEY_PREFIX + CACHE_VERSION + ":user:";
    public static final String CURRENT_USER_CACHE = "currentUser";
    public static final String USER_BY_ID_PREFIX = "user:id:";
    public static final String USER_BY_EMAIL_PREFIX = "user:email:";
    public static final String USER_BY_EXTERNAL_ID_PREFIX = "user:externalId:";
    public static final String ROLES_CACHE = "rolesCache";

    // Regions/names
    public static final String ASN_CACHE = "asnCache";

    // Resilience cache keys
    public static final String RATE_LIMIT_CACHE = "rateLimitCache";
    public static final String CAPTCHA_RATELIMIT_KEY = "captcha:";
    public static final String AUTH_SW_RATELIMIT_KEY = "auth-sw:";

    // The Secrets cache keys
    public static final String HCP_SECRETS_CACHE = "hcpSecretsCache";

    // Token cache keys
    public static final String TOKEN_CACHE = "tokenCache";
    public static final String ACCESS_TOKEN_PREFIX = "token:access:";
    public static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    public static final String REVOKED_TOKEN_PREFIX = "token:revoked:";
    public static final String PASSWORD_RESET_TOKEN_PREFIX = "token:passwordReset:";
    public static final String EMAIL_VERIFICATION_TOKEN_PREFIX = "token:emailVerification:";

    // WebAuthn cache keys
    public static final String WEBAUTHN_CACHE = "webauthnCache";
    public static final String WEBAUTHN_CHALLENGE_PREFIX = "webauthn:challenge:";
    public static final String WEBAUTHN_REGISTRATION_PREFIX = "webauthn:registration:";
    public static final String WEBAUTHN_CREDENTIAL_PREFIX = "webauthn:credential:";

    // Temporary cache keys
    public static final String TEMPORARY_CACHE = "temporaryCache"; // Used by CacheMaintenanceService

    /**
     * Generates a user ID cache key.
     *
     * @param userId The user ID
     * @return The cache key for the user
     */
    public static String userByIdKey(Long userId) {
        return USER_BY_ID_PREFIX + userId;
    }

    /**
     * Generates a user email cache key.
     *
     * @param email The user's email
     * @return The cache key for the user
     */
    public static String userByEmailKey(String email) {
        return USER_BY_EMAIL_PREFIX + email;
    }

    /**
     * Generates a login attempts cache key.
     *
     * @param username The username or email
     * @return The cache key for login attempts
     */
    public static String loginAttemptsKey(String username) {
        return LOGIN_ATTEMPTS_PREFIX + username;
    }

    /**
     * Generates a blocked IP cache key.
     *
     * @param ipAddress The IP address
     * @return The cache key for the blocked IP
     */
    public static String blockedIpKey(String ipAddress) {
        return BLOCKED_IP_PREFIX + ipAddress;
    }

    private CacheKeys() {
    } // Private constructor to prevent instantiation
}

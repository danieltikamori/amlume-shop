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

import java.time.Duration;

/**
 * Security-related constants used throughout the application.
 */
public final class SecurityConstants {

    // Cache TTL
    public static final Duration ANNOTATION_CACHE_DURATION = Duration.ofHours(24);
    public static final Duration USER_CACHE_DURATION = Duration.ofMinutes(30);
    public static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration USERS_CACHE_TTL = Duration.ofMinutes(15); // User details - shorter TTL
    public static final Duration ROLES_CACHE_TTL = Duration.ofHours(4);
    //    public static final Duration ROLES_CACHE_TTL = Duration.ofHours(2);
    public static final Duration TOKENS_CACHE_TTL = Duration.ofHours(1); // Tokens - TTL should match token validity
    public static final Duration AUTH_CACHE_TTL = Duration.ofHours(6);
    public static final Duration PRODUCTS_CACHE_TTL = Duration.ofDays(3);
    //    public static final Duration PRODUCTS_CACHE_TTL = Duration.ofMinutes(30);
    public static final Duration CATEGORIES_CACHE_TTL = Duration.ofDays(7);
    //    public static final Duration CATEGORIES_CACHE_TTL = Duration.ofHours(1);
    public static final Duration ASN_CACHE_TTL = Duration.ofDays(15);
    //    public static final Duration ASN_CACHE_TTL = Duration.ofDays(7);
    public static final Duration HCP_SECRETS_CACHE_TTL = Duration.ofMinutes(35); // (e.g., slightly longer than secrets refresh interval)
    public static final Duration TEMPORARY_CACHE_TTL = Duration.ofMinutes(10); // Cleaned by maintenance service

    // Cryptography constants
    public static final String BCRYPT = "{bcrypt}";
    public static final String ARGON2 = "{argon2}";
    public static final int ARGON2_SALT_LENGTH = 16;
    public static final int ARGON2_HASH_LENGTH = 32;
    public static final int ARGON2_PARALLELISM = 1;
    public static final int ARGON2_MEMORY = 65536;
    //    public static final int ARGON2_MEMORY = 1 << 16;
    public static final int ARGON2_ITERATIONS = 10;
    public static final String SHA256 = "SHA-256";
    public static final String SHA512 = "SHA-512";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String PBKDF2 = "PBKDF2WithHmacSHA256";
    public static final String PBKDF2_ITERATIONS = "10000";
    public static final String PBKDF2_LENGTH = "32";
    public static final String PBKDF2_SALT_LENGTH = "16";
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final String PBKDF2_ALGORITHM_NAME = "PBKDF2";
    public static final String PBKDF2_ALGORITHM_FULL_NAME = "PBKDF2WithHmacSHA256";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int INITIAL_MAP_CAPACITY = 16;

    // JWT related constants
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long ACCESS_TOKEN_EXPIRATION = 900_000; // 15 minutes in milliseconds
    public static final long REFRESH_TOKEN_EXPIRATION = 604_800_000; // 7 days in milliseconds

    // Password policy constants
    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final boolean REQUIRE_UPPERCASE = true;
    public static final boolean REQUIRE_LOWERCASE = true;
    public static final boolean REQUIRE_DIGIT = true;
    public static final boolean REQUIRE_SPECIAL_CHAR = true;
    public static final int MAX_REPETITIVE_CHARS = 3;

    // Account lockout constants
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION = 900_000; // 15 minutes in milliseconds

    // Session constants
    public static final int SESSION_TIMEOUT = 1800; // 30 minutes in seconds
    public static final boolean SECURE_COOKIE = true;
    public static final String REMEMBER_ME_KEY = "amlume-remember-me-key";
    public static final int REMEMBER_ME_VALIDITY = 2592000; // 30 days in seconds
    public static final String REMEMBER_ME_PARAMETER = "remember-me";
    public static final String REMEMBER_ME_COOKIE_NAME = "remember-me";
    public static final String SESSIONS_MAP_NAME = "${spring:session:authserver:sessions}";

    // CSRF constants
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    // Content Security Policy (CSP) nonce attribute name
    public static final String CSP_NONCE_ATTRIBUTE = "nonce";

    // WebAuthn constants
    public static final String WEBAUTHN_RELYING_PARTY_ID = "amlume.me";
    public static final String WEBAUTHN_RELYING_PARTY_NAME = "Amlume Shop";

    private SecurityConstants() {
    } // Private constructor to prevent instantiation
}

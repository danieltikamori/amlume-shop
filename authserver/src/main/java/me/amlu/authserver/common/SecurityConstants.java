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
 * Security-related constants used throughout the application.
 */
public final class SecurityConstants {

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

    // CSRF constants
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    // WebAuthn constants
    public static final String WEBAUTHN_RELYING_PARTY_ID = "amlume.me";
    public static final String WEBAUTHN_RELYING_PARTY_NAME = "Amlume Shop";

    private SecurityConstants() {
    } // Private constructor to prevent instantiation
}

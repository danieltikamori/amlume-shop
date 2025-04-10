/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.commons;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@Getter
public final class Constants {
    // --- ASN constants ---
    public static final String ASN_LOOKUP_REQUESTS_METRIC = "asn.lookup.requests";
    public static final String ASN_LOOKUP_RATELIMIT_EXCEEDED_METRIC = "asn.lookup.ratelimit.exceeded";
    public static final String ASN_LOOKUP_RATE_LIMIT_EXCEEDED_MESSAGE = "ASN lookup rate limit exceeded";

    // --- Cache constants ---
    public static final String CACHE = "cache";
    private static final String CACHE_VERSION = "v1";

    // --- Cache Names ---
//    public static final String USER_CACHE = "users";
    public static final String USER_CACHE = "userCache";
    public static final String CURRENT_USER_CACHE = "currentUser";
    public static final String AUTH_CACHE = "authCache";
    public static final String PRODUCT_CACHE = "productCache";


    // --- Cache Keys ---
//    public static final String USER_CACHE_KEY_PREFIX = "user::";
    public static final String AUTH_CACHE_KEY_PREFIX = "auth:";
    public static final String LOCK_PREFIX = "auth:lock:";
    public static final String USER_CACHE_KEY_PREFIX = AUTH_CACHE_KEY_PREFIX + CACHE_VERSION + ":user:";
    public static final String ANNOTATION_CACHE_KEY_PREFIX = AUTH_CACHE_KEY_PREFIX + CACHE_VERSION + ":annotation:";

    // --- Cache Durations ---
    public static final Duration ANNOTATION_CACHE_DURATION = Duration.ofHours(24);
    public static final Duration USER_CACHE_DURATION = Duration.ofMinutes(30);
    public static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    // --- Compression constants ---
    public static final int COMPRESSION_THRESHOLD_BYTES = 1024;

    // --- Pagination constants ---
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "50";
    public static final String SORT_DIR = "asc";
    public static final String SORT_CATEGORIES_BY = "categoryId";
    public static final String SORT_PRODUCTS_BY = "productId";
    public static final String DEFAULT_PAGE_NUMBER = "0";

    // --- Tolerances ---
    // For time-based validation
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(10);

    // --- Authentication and Authorization constants ---
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";

    // --- Http header constants ---
    public static final String USER_AGENT = "User-Agent";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String TIME_ZONE = "X-Time-Zone";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
//    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
//    public static final String X_FORWARDED_PORT = "X-Forwarded-Port";
//    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
//    public static final String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";
//    public static final String X_FORWARDED_SERVER = "X-Forwarded-Server";
//    public static final String X_FORWARDED_URI = "X-Forwarded-Uri";
//    public static final String X_FORWARDED_SCHEME = "X-Forwarded-Scheme";
//    public static final String X_FORWARDED_METHOD = "X-Forwarded-Method";
//    public static final String X_FORWARDED_FORWARDED = "X-Forwarded-Forwarded";
//    public static final String X_FORWARDED_HOSTNAME = "X-Forwarded-Hostname";
//    public static final String X_FORWARDED_SERVER_NAME = "X-Forwarded-Server-Name";
//    public static final String X_FORWARDED_SERVER_PORT = "X-Forwarded-Server-Port";
//    public static final String X_FORWARDED_SERVER_PROTOCOL = "X-Forwarded-Server-Protocol";
//    public static final String X_FORWARDED_SERVER_SCHEME = "X-Forwarded-Server-Scheme";
//    public static final String X_FORWARDED_SERVER_URI = "X-Forwarded-Server-Uri";

    // --- Resilience constants ---
    public static final String CIRCUIT_BREAKER = "circuitBreaker";
    public static final String RATES_LIMITER = "rateLimiter";
    public static final String RETRY = "retry";
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_INTERVAL = 1000L;
    public static final Long MAX_REQUESTS_PER_MINUTE = 100L;

    // --- User constants ---
    public static final String USER_ID = "userId";
    public static final String USERNAME = "XXXXXXXX";
    public static final String PASSWORD = "XXXXXXXX";
    public static final String EMAIL = "XXXXXXXX";
    public static final String USER_NOT_FOUND = "User not found";

    // --- Security Constants ---
    public static final String ROLE_PREFIX = "ROLE_";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_DURATION_MILLIS = 1000 * 60 * (long) 60;
    public static final long LOCK_TIME_DURATION = 24 * 60 * 60 * (long) 1000; // 24 hours in milliseconds
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int INITIAL_MAP_CAPACITY = 16;
    public static final String DEVICE_FINGERPRINT_CANNOT_BE_NULL = "Device fingerprint cannot be null";

    // --- Line separator ---
    private static final String LINE_SEPARATOR = "\n";

    // --- Product constants ---

    public static final String PRODUCT = "Product";
    public static final String PRODUCT_ID = "productId";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

}
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
public class Constants {
    //ASN constants
    public static final String ASN_LOOKUP_REQUESTS_METRIC = "asn.lookup.requests";
    public static final String ASN_LOOKUP_RATELIMIT_EXCEEDED_METRIC = "asn.lookup.ratelimit.exceeded";
    public static final String ASN_LOOKUP_RATE_LIMIT_EXCEEDED_MESSAGE = "ASN lookup rate limit exceeded";

    // Pagination constants
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "50";
    public static final String SORT_DIR = "asc";
    public static final String SORT_CATEGORIES_BY = "categoryId";
    public static final String SORT_PRODUCTS_BY = "productId";
    public static final String DEFAULT_PAGE_NUMBER = "0";

    // Tolerances
    // For time-based validation
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(10);

    // Authentication and Authorization constants
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";

    // Http header constants
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

    // Resilience constants
    public static final String CIRCUIT_BREAKER = "circuitBreaker";
    public static final String RATES_LIMITER = "rateLimiter";

    // User constants
    public static final String USER_ID = "userId";
    public static final String USERNAME = "XXXXXXXX";
    public static final String PASSWORD = "XXXXXXXX";
    public static final String EMAIL = "XXXXXXXX";
    public static final String USER_NOT_FOUND = "User not found";

    // Security constants
    public static final String ROLE_PREFIX = "ROLE_";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_DURATION_MILLIS = 1000 * 60 * (long) 60;
    public static final long LOCK_TIME_DURATION = 24 * 60 * 60 * (long) 1000; // 24 hours in milliseconds
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int INITIAL_MAP_CAPACITY = 16;

    private static final String LINE_SEPARATOR = "\n";

    private Constants() {
    } // Private constructor to prevent instantiation

}
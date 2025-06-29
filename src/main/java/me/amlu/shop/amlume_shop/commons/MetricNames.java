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

/**
 * Metric names used for monitoring and observability.
 */
public final class MetricNames {
    // Authentication metrics
    public static final String AUTH_LOGIN_ATTEMPTS = "auth.login.attempts";
    public static final String AUTH_LOGIN_SUCCESS = "auth.login.success";
    public static final String AUTH_LOGIN_FAILURE = "auth.login.failure";
    public static final String AUTH_LOGOUT = "auth.logout";
    public static final String AUTH_PASSWORD_RESET = "auth.password.reset";
    public static final String AUTH_ACCOUNT_LOCKED = "auth.account.locked";

    // API metrics
    public static final String API_REQUEST_COUNT = "api.request.count";
    public static final String API_REQUEST_DURATION = "api.request.duration";
    public static final String API_ERROR_COUNT = "api.error.count";
    public static final String API_RATE_LIMIT_EXCEEDED = "api.rate.limit.exceeded";

    // User metrics
    public static final String USER_REGISTRATION = "user.registration";
    public static final String USER_DELETION = "user.deletion";
    public static final String USER_ACTIVE_COUNT = "user.active.count";

    // WebAuthn metrics
    public static final String WEBAUTHN_REGISTRATION = "webauthn.registration";
    public static final String WEBAUTHN_AUTHENTICATION = "webauthn.authentication";

    // System metrics
    public static final String SYSTEM_CPU_USAGE = "system.cpu.usage";
    public static final String SYSTEM_MEMORY_USAGE = "system.memory.usage";
    public static final String SYSTEM_THREAD_COUNT = "system.thread.count";

    // Database metrics
    public static final String DB_CONNECTION_COUNT = "db.connection.count";
    public static final String DB_QUERY_TIME = "db.query.time";
    public static final String DB_TRANSACTION_COUNT = "db.transaction.count";

    // Cache metrics
    public static final String CACHE_HIT_COUNT = "cache.hit.count";
    public static final String CACHE_MISS_COUNT = "cache.miss.count";
    public static final String CACHE_SIZE = "cache.size";

    private MetricNames() {
    } // Private constructor to prevent instantiation
}

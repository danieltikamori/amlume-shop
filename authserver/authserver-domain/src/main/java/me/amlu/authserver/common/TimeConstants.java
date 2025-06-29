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
import java.time.temporal.ChronoUnit;

/**
 * Time-related constants used throughout the application.
 */
public final class TimeConstants {
    // Time in milliseconds
    public static final long MILLIS_PER_SECOND = 1000L;
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
    public static final long MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY;

    // Time in seconds
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    public static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
    public static final long SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;
    public static final long SECONDS_PER_MONTH_30 = 30 * SECONDS_PER_DAY;
    public static final long SECONDS_PER_YEAR_365 = 365 * SECONDS_PER_DAY;

    // Common durations
    public static final Duration FIVE_MINUTES = Duration.of(5, ChronoUnit.MINUTES);
    public static final Duration FIFTEEN_MINUTES = Duration.of(15, ChronoUnit.MINUTES);
    public static final Duration THIRTY_MINUTES = Duration.of(30, ChronoUnit.MINUTES);
    public static final Duration ONE_HOUR = Duration.of(1, ChronoUnit.HOURS);
    public static final Duration ONE_DAY = Duration.of(1, ChronoUnit.DAYS);
    public static final Duration ONE_WEEK = Duration.of(7, ChronoUnit.DAYS);
    public static final Duration THIRTY_DAYS = Duration.of(30, ChronoUnit.DAYS);

    // Common timeout values
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.of(10, ChronoUnit.SECONDS);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.of(30, ChronoUnit.SECONDS);
    public static final Duration DEFAULT_SESSION_TIMEOUT = Duration.of(30, ChronoUnit.MINUTES);
    public static final Duration DEFAULT_CACHE_TIMEOUT = Duration.of(1, ChronoUnit.HOURS);
    public static final Duration DEFAULT_TOKEN_EXPIRATION = Duration.of(15, ChronoUnit.MINUTES);
    public static final Duration DEFAULT_REFRESH_TOKEN_EXPIRATION = Duration.of(7, ChronoUnit.DAYS);
    public static final Duration DEFAULT_PASSWORD_RESET_EXPIRATION = Duration.of(1, ChronoUnit.HOURS);
    public static final Duration DEFAULT_VERIFICATION_CODE_EXPIRATION = Duration.of(10, ChronoUnit.MINUTES);

    private TimeConstants() {
    } // Private constructor to prevent instantiation
}

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Utility methods for date and time operations.
 */
public final class DateUtils {

    private DateUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the current UTC instant.
     *
     * @return Current UTC instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Converts a Date to an Instant.
     *
     * @param date The date to convert
     * @return The instant
     */
    public static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    /**
     * Converts an Instant to a Date.
     *
     * @param instant The instant to convert
     * @return The date
     */
    public static Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }

    /**
     * Formats an Instant using the specified pattern.
     *
     * @param instant The instant to format
     * @param pattern The pattern to use
     * @return The formatted date string
     */
    public static String format(Instant instant, String pattern) {
        if (instant == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    /**
     * Formats an Instant using ISO_DATE_TIME format.
     *
     * @param instant The instant to format
     * @return The formatted date string
     */
    public static String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    /**
     * Parses a date string using the specified pattern.
     *
     * @param dateStr The date string to parse
     * @param pattern The pattern to use
     * @return The parsed instant
     */
    public static Instant parse(String dateStr, String pattern) {
        if (dateStr == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault());
        return ZonedDateTime.parse(dateStr, formatter).toInstant();
    }

    /**
     * Adds the specified amount of time to an instant.
     *
     * @param instant The instant
     * @param amount  The amount to add
     * @param unit    The time unit
     * @return The new instant
     */
    public static Instant add(Instant instant, long amount, ChronoUnit unit) {
        if (instant == null) {
            return null;
        }
        return instant.plus(amount, unit);
    }

    /**
     * Subtracts the specified amount of time from an instant.
     *
     * @param instant The instant
     * @param amount  The amount to subtract
     * @param unit    The time unit
     * @return The new instant
     */
    public static Instant subtract(Instant instant, long amount, ChronoUnit unit) {
        if (instant == null) {
            return null;
        }
        return instant.minus(amount, unit);
    }

    /**
     * Checks if an instant is before another instant.
     *
     * @param instant1 The first instant
     * @param instant2 The second instant
     * @return true if the first instant is before the second
     */
    public static boolean isBefore(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return false;
        }
        return instant1.isBefore(instant2);
    }

    /**
     * Checks if an instant is after another instant.
     *
     * @param instant1 The first instant
     * @param instant2 The second instant
     * @return true if the first instant is after the second
     */
    public static boolean isAfter(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return false;
        }
        return instant1.isAfter(instant2);
    }

    /**
     * Gets the duration between two instants.
     *
     * @param start The start instant
     * @param end   The end instant
     * @return The duration between the instants
     */
    public static Duration between(Instant start, Instant end) {
        if (start == null || end == null) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }

    /**
     * Checks if an instant is expired based on a duration.
     *
     * @param instant  The instant to check
     * @param duration The duration
     * @return true if the instant plus duration is before now
     */
    public static boolean isExpired(Instant instant, Duration duration) {
        if (instant == null) {
            return true;
        }
        return instant.plus(duration).isBefore(now());
    }
}

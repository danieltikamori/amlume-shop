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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility methods for string operations.
 */
public final class StringUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMERIC = "0123456789";

    private StringUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Checks if a string is null or empty.
     *
     * @param str The string to check
     * @return true if the string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * @param str The string to check
     * @return true if the string is null, empty, or contains only whitespace
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Returns a default value if the string is null or empty.
     *
     * @param str          The string to check
     * @param defaultValue The default value to return if the string is null or empty
     * @return The string if not null or empty, otherwise the default value
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length The length of the string to generate
     * @return A random alphanumeric string
     */
    public static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random numeric string of the specified length.
     *
     * @param length The length of the string to generate
     * @return A random numeric string
     */
    public static String randomNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC.charAt(SECURE_RANDOM.nextInt(NUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random UUID as a string.
     *
     * @return A random UUID string
     */
    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Masks a string by replacing characters with asterisks, keeping only the first and last characters.
     *
     * @param str          The string to mask
     * @param visibleChars Number of characters to keep visible at start and end
     * @return The masked string
     */
    public static String mask(String str, int visibleChars) {
        if (isEmpty(str) || str.length() <= visibleChars * 2) {
            return str;
        }

        int maskLength = str.length() - (visibleChars * 2);
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, visibleChars));
        for (int i = 0; i < maskLength; i++) {
            sb.append('*');
        }
        sb.append(str.substring(str.length() - visibleChars));
        return sb.toString();
    }

    /**
     * Encodes a string to Base64.
     *
     * @param str The string to encode
     * @return The Base64 encoded string
     */
    public static String encodeBase64(String str) {
        if (str == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a Base64 encoded string.
     *
     * @param base64 The Base64 encoded string
     * @return The decoded string
     */
    public static String decodeBase64(String base64) {
        if (base64 == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }
}

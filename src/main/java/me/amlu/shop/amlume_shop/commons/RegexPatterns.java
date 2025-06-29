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

import java.util.regex.Pattern;

/**
 * Common regex patterns for security-related validations.
 */
public final class RegexPatterns {
    // Security patterns
    public static final String XSS_SUSPICIOUS = "<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|style\\s*=\\s*\".*?expression\\s*\\(|<iframe|<object|<embed";
    public static final Pattern XSS_SUSPICIOUS_PATTERN = Pattern.compile(XSS_SUSPICIOUS, Pattern.CASE_INSENSITIVE);

    // SQL injection patterns
    public static final String SQL_INJECTION_SUSPICIOUS = "(?i)\\b(select|insert|update|delete|drop|alter|create|exec|union|where)\\b.*?\\b(from|into|table|database|values)\\b";
    public static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(SQL_INJECTION_SUSPICIOUS, Pattern.CASE_INSENSITIVE);

    // Path traversal patterns
    public static final String PATH_TRAVERSAL = "\\.\\./|\\.\\.\\\\|~/|~\\\\";
    public static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(PATH_TRAVERSAL);

    // JWT token pattern
    public static final String JWT_TOKEN = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$";
    public static final Pattern JWT_TOKEN_PATTERN = Pattern.compile(JWT_TOKEN);

    // IP address patterns
    public static final String IPV4_ADDRESS = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    public static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile(IPV4_ADDRESS);

    public static final String IPV6_ADDRESS = "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$";
    public static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile(IPV6_ADDRESS);

    private RegexPatterns() {
    } // Private constructor to prevent instantiation

    /**
     * Checks if input contains potential XSS attack patterns.
     *
     * @param input The input to check
     * @return true if suspicious patterns are found
     */
    public static boolean containsXssSuspiciousPattern(String input) {
        if (input == null) {
            return false;
        }
        return XSS_SUSPICIOUS_PATTERN.matcher(input).find();
    }

    /**
     * Checks if input contains potential SQL injection patterns.
     *
     * @param input The input to check
     * @return true if suspicious patterns are found
     */
    public static boolean containsSqlInjectionPattern(String input) {
        if (input == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Validates if a string is a valid JWT token format.
     *
     * @param token The token to validate
     * @return true if the token matches JWT format
     */
    public static boolean isValidJwtFormat(String token) {
        if (token == null) {
            return false;
        }
        return JWT_TOKEN_PATTERN.matcher(token).matches();
    }
}

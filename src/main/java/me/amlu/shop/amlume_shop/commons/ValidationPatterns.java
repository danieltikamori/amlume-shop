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
 * Centralized validation patterns used throughout the application.
 * Contains regex patterns and compiled Pattern objects for efficient validation.
 */
public final class ValidationPatterns {
    // Email validation
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // Password validation - at least 12 chars, with uppercase, lowercase, digit, and special char
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,128}$";
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    // Username/nickname validation - alphanumeric with underscore and hyphen, 3-30 chars
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,30}$";
    public static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    // Phone number validation - E.164 format
    public static final String PHONE_REGEX = "^\\+[1-9]\\d{1,14}$";
    public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    // URL validation
    public static final String URL_REGEX = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
    public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    // UUID validation
    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    // Name validation - letters, spaces, hyphens, apostrophes
    public static final String NAME_REGEX = "^[\\p{L} .'-]{1,100}$";
    public static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);

    // Postal/ZIP code validation (generic pattern, customize for specific countries)
    public static final String POSTAL_CODE_REGEX = "^[a-zA-Z0-9\\s-]{3,10}$";
    public static final Pattern POSTAL_CODE_PATTERN = Pattern.compile(POSTAL_CODE_REGEX);

    // ISO country code validation
    public static final String COUNTRY_CODE_REGEX = "^[A-Z]{2}$";
    public static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile(COUNTRY_CODE_REGEX);

    // ISO language code validation
    public static final String LANGUAGE_CODE_REGEX = "^[a-z]{2}(-[A-Z]{2})?$";
    public static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile(LANGUAGE_CODE_REGEX);

    private ValidationPatterns() {
    } // Private constructor to prevent instantiation

    /**
     * Validates an email address against the email pattern.
     *
     * @param email The email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates a password against the password pattern.
     *
     * @param password The password to validate
     * @return true if the password is valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validates a phone number against the E.164 format.
     *
     * @param phoneNumber The phone number to validate
     * @return true if the phone number is valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }
}

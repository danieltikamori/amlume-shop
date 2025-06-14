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
 * General constants used throughout the application that don't fit into more specific categories.
 */
public final class Constants {
    // General application constants
    public static final String APP_NAME = "Amlume Auth Server";
    public static final String APP_VERSION = "1.0.0";
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_LOCALE = "en_US";

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // Default limits
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_PASSWORD_LENGTH = 127;
    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int MAX_DEVICES_PER_USER = 10;
    public static final int MAX_SESSIONS_PER_USER = 5;

    // Feature flags
    public static final boolean ENABLE_MFA = true;
    public static final boolean ENABLE_PASSKEYS = true;
    public static final boolean ENABLE_SOCIAL_LOGIN = true;
    public static final boolean ENABLE_RATE_LIMITING = true;
    public static final boolean ENABLE_IP_FILTERING = true;

    // Default timeouts (seconds)
    public static final int SESSION_TIMEOUT = 1800; // 30 minutes
    public static final int REMEMBER_ME_TIMEOUT = 2592000; // 30 days
    public static final int PASSWORD_RESET_TIMEOUT = 3600; // 1 hour
    public static final int VERIFICATION_CODE_TIMEOUT = 600; // 10 minutes

    // Database constants
    public static final int PASSWORD_FIELD_LENGTH = 127; // During my testing, the longest passwords generated an encoded string of 94 characters. So 127 is enough
    public static final int USERNAME_FIELD_LENGTH = 127;
    public static final int GIVEN_NAME_FIELD_LENGTH = 127;
    public static final int MIDDLE_NAME_FIELD_LENGTH = 127;
    public static final int SURNAME_FIELD_LENGTH = 127;
    public static final int EMAIL_FIELD_LENGTH = 255;
    public static final int IP_FIELD_LENGTH = 45;
    public static final int USER_AGENT_FIELD_LENGTH = 255;
    public static final int SESSION_ID_FIELD_LENGTH = 255;
    public static final int DEVICE_ID_FIELD_LENGTH = 255;
    public static final int SESSION_TOKEN_FIELD_LENGTH = 255;
    public static final int VERIFICATION_CODE_LENGTH = 6;


    private Constants() {
    } // Private constructor to prevent instantiation
}

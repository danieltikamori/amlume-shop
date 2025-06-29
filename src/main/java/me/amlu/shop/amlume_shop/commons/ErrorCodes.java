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
 * Error codes used throughout the application for consistent error handling.
 * Each error code has a unique identifier that can be used for documentation,
 * logging, and client-side error handling.
 */
public final class ErrorCodes {
    // Authentication errors (1000-1099)
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH-1000";
    public static final String AUTH_ACCOUNT_LOCKED = "AUTH-1001";
    public static final String AUTH_ACCOUNT_DISABLED = "AUTH-1002";
    public static final String AUTH_ACCOUNT_EXPIRED = "AUTH-1003";
    public static final String AUTH_CREDENTIALS_EXPIRED = "AUTH-1004";
    public static final String AUTH_INVALID_TOKEN = "AUTH-1005";
    public static final String AUTH_EXPIRED_TOKEN = "AUTH-1006";
    public static final String AUTH_REVOKED_TOKEN = "AUTH-1007";
    public static final String AUTH_MISSING_TOKEN = "AUTH-1008";
    public static final String AUTH_INVALID_MFA = "AUTH-1009";
    public static final String AUTH_MFA_REQUIRED = "AUTH-1010";
    public static final String AUTH_INVALID_CAPTCHA = "AUTH-1011";
    public static final String AUTH_TOO_MANY_ATTEMPTS = "AUTH-1012";

    // Authorization errors (1100-1199)
    public static final String ACCESS_DENIED = "AUTH-1100";
    public static final String INSUFFICIENT_PERMISSIONS = "AUTH-1101";
    public static final String INVALID_SCOPE = "AUTH-1102";

    // User management errors (1200-1299)
    public static final String USER_NOT_FOUND = "USER-1200";
    public static final String USER_ALREADY_EXISTS = "USER-1201";
    public static final String USER_INVALID_DATA = "USER-1202";
    public static final String USER_EMAIL_IN_USE = "USER-1203";
    public static final String USER_NICKNAME_IN_USE = "USER-1204";
    public static final String USER_PHONE_IN_USE = "USER-1205";
    public static final String USER_WEAK_PASSWORD = "USER-1206";
    public static final String USER_EMAIL_NOT_VERIFIED = "USER-1207";

    // WebAuthn/Passkey errors (1300-1399)
    public static final String WEBAUTHN_REGISTRATION_FAILED = "WEBAUTHN-1300";
    public static final String WEBAUTHN_AUTHENTICATION_FAILED = "WEBAUTHN-1301";
    public static final String WEBAUTHN_CREDENTIAL_NOT_FOUND = "WEBAUTHN-1302";
    public static final String WEBAUTHN_CHALLENGE_MISMATCH = "WEBAUTHN-1303";
    public static final String WEBAUTHN_ORIGIN_MISMATCH = "WEBAUTHN-1304";
    public static final String WEBAUTHN_ATTESTATION_INVALID = "WEBAUTHN-1305";

    // System errors (1400-1499)
    public static final String SYSTEM_INTERNAL_ERROR = "SYS-1400";
    public static final String SYSTEM_SERVICE_UNAVAILABLE = "SYS-1401";
    public static final String SYSTEM_DATABASE_ERROR = "SYS-1402";
    public static final String SYSTEM_CACHE_ERROR = "SYS-1403";
    public static final String SYSTEM_ENCRYPTION_ERROR = "SYS-1404";
    public static final String SYSTEM_RATE_LIMIT_EXCEEDED = "SYS-1405";

    // Validation errors (1500-1599)
    public static final String VALIDATION_INVALID_INPUT = "VAL-1500";
    public static final String VALIDATION_MISSING_FIELD = "VAL-1501";
    public static final String VALIDATION_INVALID_FORMAT = "VAL-1502";

    private ErrorCodes() {
    } // Private constructor to prevent instantiation
}

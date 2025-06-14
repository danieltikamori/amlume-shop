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
 * Constants for HTTP header names used throughout the application.
 */
public final class HeaderNames {
    // Standard headers
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String ORIGIN = "Origin";
    public static final String REFERER = "Referer";
    public static final String HOST = "Host";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    // Security headers
    public static final String X_CSRF_TOKEN = "X-CSRF-Token";
    public static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_XSS_PROTECTION = "X-XSS-Protection";
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    // Custom application headers
    public static final String X_API_KEY = "X-API-Key";
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_DEVICE_ID = "X-Device-ID";
    public static final String X_APP_VERSION = "X-App-Version";
    public static final String X_CLIENT_ID = "X-Client-ID";

    // Authentication headers
    public static final String X_AUTH_TOKEN = "X-Auth-Token";
    public static final String X_REFRESH_TOKEN = "X-Refresh-Token";
    public static final String X_MFA_TOKEN = "X-MFA-Token";

    private HeaderNames() {
    } // Private constructor to prevent instantiation
}

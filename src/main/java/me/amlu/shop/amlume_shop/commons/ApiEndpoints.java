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
 * Constants for API endpoints used throughout the application.
 * This class centralizes endpoint definitions to ensure consistency
 * and make refactoring easier.
 */
public final class ApiEndpoints {
    // API version
    public static final String API_VERSION = "/api/v1";

    // Authentication endpoints
    public static final String AUTH_BASE = API_VERSION + "/auth";
    public static final String LOGIN = AUTH_BASE + "/login";
    public static final String LOGOUT = AUTH_BASE + "/logout";
    public static final String REFRESH_TOKEN = AUTH_BASE + "/refresh";
    public static final String REGISTER = AUTH_BASE + "/register";
    public static final String VERIFY_EMAIL = AUTH_BASE + "/verify-email";
    public static final String FORGOT_PASSWORD = AUTH_BASE + "/forgot-password";
    public static final String RESET_PASSWORD = AUTH_BASE + "/reset-password";

    // User management endpoints
    public static final String USERS_BASE = API_VERSION + "/users";
    public static final String USER_PROFILE = USERS_BASE + "/profile";
    public static final String CHANGE_PASSWORD = USERS_BASE + "/change-password";

    // WebAuthn/Passkey endpoints
    public static final String WEBAUTHN_BASE = API_VERSION + "/webauthn";
    public static final String WEBAUTHN_REGISTER_START = WEBAUTHN_BASE + "/register/start";
    public static final String WEBAUTHN_REGISTER_FINISH = WEBAUTHN_BASE + "/register/finish";
    public static final String WEBAUTHN_AUTHENTICATE_START = WEBAUTHN_BASE + "/authenticate/start";
    public static final String WEBAUTHN_AUTHENTICATE_FINISH = WEBAUTHN_BASE + "/authenticate/finish";

    // Admin endpoints
    public static final String ADMIN_BASE = API_VERSION + "/admin";
    public static final String ADMIN_USERS = ADMIN_BASE + "/users";
    public static final String ADMIN_ROLES = ADMIN_BASE + "/roles";
    public static final String ADMIN_PERMISSIONS = ADMIN_BASE + "/permissions";

    // OAuth2 endpoints
    public static final String OAUTH2_BASE = "/oauth2";
    public static final String OAUTH2_AUTHORIZE = OAUTH2_BASE + "/authorize";
    public static final String OAUTH2_TOKEN = OAUTH2_BASE + "/token";
    public static final String OAUTH2_JWKS = OAUTH2_BASE + "/.well-known/jwks.json";

    // Health and monitoring endpoints
    public static final String ACTUATOR_BASE = "/actuator";
    public static final String HEALTH = ACTUATOR_BASE + "/health";
    public static final String INFO = ACTUATOR_BASE + "/info";
    public static final String METRICS = ACTUATOR_BASE + "/metrics";

    private ApiEndpoints() {
    } // Private constructor to prevent instantiation
}

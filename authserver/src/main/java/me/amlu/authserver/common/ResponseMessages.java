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
 * Standard response messages used throughout the application.
 */
public final class ResponseMessages {
    // Success messages
    public static final String SUCCESS = "Operation completed successfully";
    public static final String CREATED = "Resource created successfully";
    public static final String UPDATED = "Resource updated successfully";
    public static final String DELETED = "Resource deleted successfully";

    // Authentication messages
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String LOGOUT_SUCCESS = "Logout successful";
    public static final String PASSWORD_CHANGED = "Password changed successfully";
    public static final String PASSWORD_RESET_REQUESTED = "Password reset instructions sent to your email";
    public static final String PASSWORD_RESET_SUCCESS = "Password reset successfully";
    public static final String REGISTRATION_SUCCESS = "Registration successful";
    public static final String EMAIL_VERIFICATION_SENT = "Verification email sent";
    public static final String EMAIL_VERIFIED = "Email verified successfully";

    // Error messages
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String UNAUTHORIZED = "Unauthorized access";
    public static final String FORBIDDEN = "Access forbidden";
    public static final String NOT_FOUND = "Resource not found";
    public static final String CONFLICT = "Resource conflict";
    public static final String VALIDATION_ERROR = "Validation error";
    public static final String INTERNAL_ERROR = "Internal server error";
    public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable";

    // Authentication error messages
    public static final String INVALID_CREDENTIALS = "Invalid username or password";
    public static final String ACCOUNT_LOCKED = "Account is locked";
    public static final String ACCOUNT_DISABLED = "Account is disabled";
    public static final String ACCOUNT_EXPIRED = "Account has expired";
    public static final String CREDENTIALS_EXPIRED = "Credentials have expired";
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String EXPIRED_TOKEN = "Token has expired";
    public static final String MFA_REQUIRED = "Multi-factor authentication required";
    public static final String INVALID_MFA_CODE = "Invalid multi-factor authentication code";
    public static final String TOO_MANY_ATTEMPTS = "Too many failed attempts";

    private ResponseMessages() {
    } // Private constructor to prevent instantiation
}

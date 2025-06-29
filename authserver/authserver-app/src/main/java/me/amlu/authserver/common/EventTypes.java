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
 * Constants for event types used throughout the application.
 * This class centralizes event type definitions for consistent event handling.
 */
public final class EventTypes {
    // Authentication events
    public static final String USER_LOGIN_SUCCESS = "auth.login.success";
    public static final String USER_LOGIN_FAILURE = "auth.login.failure";
    public static final String USER_LOGOUT = "auth.logout";
    public static final String PASSWORD_RESET_REQUESTED = "auth.password.reset.requested";
    public static final String PASSWORD_RESET_COMPLETED = "auth.password.reset.completed";
    public static final String PASSWORD_CHANGED = "auth.password.changed";
    public static final String ACCOUNT_LOCKED = "auth.account.locked";
    public static final String ACCOUNT_UNLOCKED = "auth.account.unlocked";
    public static final String MFA_ENABLED = "auth.mfa.enabled";
    public static final String MFA_DISABLED = "auth.mfa.disabled";
    public static final String MFA_CHALLENGE_ISSUED = "auth.mfa.challenge.issued";
    public static final String MFA_CHALLENGE_SUCCEEDED = "auth.mfa.challenge.succeeded";
    public static final String MFA_CHALLENGE_FAILED = "auth.mfa.challenge.failed";

    // User management events
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_VERIFIED = "user.verified";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_ENABLED = "user.enabled";
    public static final String USER_DISABLED = "user.disabled";
    public static final String ROLE_ASSIGNED = "user.role.assigned";
    public static final String ROLE_REVOKED = "user.role.revoked";

    // WebAuthn/Passkey events
    public static final String PASSKEY_REGISTERED = "webauthn.passkey.registered";
    public static final String PASSKEY_USED = "webauthn.passkey.used";
    public static final String PASSKEY_REMOVED = "webauthn.passkey.removed";

    // Token events
    public static final String TOKEN_CREATED = "token.created";
    public static final String TOKEN_REFRESHED = "token.refreshed";
    public static final String TOKEN_REVOKED = "token.revoked";
    public static final String TOKEN_EXPIRED = "token.expired";

    // Security events
    public static final String SUSPICIOUS_ACTIVITY = "security.suspicious.activity";
    public static final String RATE_LIMIT_EXCEEDED = "security.rate.limit.exceeded";
    public static final String IP_BLOCKED = "security.ip.blocked";
    public static final String IP_UNBLOCKED = "security.ip.unblocked";

    // System events
    public static final String SYSTEM_ERROR = "system.error";
    public static final String SYSTEM_WARNING = "system.warning";
    public static final String SYSTEM_INFO = "system.info";
    public static final String CACHE_CLEARED = "system.cache.cleared";

    private EventTypes() {
    } // Private constructor to prevent instantiation
}

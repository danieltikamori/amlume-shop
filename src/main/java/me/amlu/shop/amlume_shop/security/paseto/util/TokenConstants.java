/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto.util;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public final class TokenConstants {

    // Token related
    public static final long ACCESS_TOKEN_VALIDITY = 3600; // 1 hour in seconds
    public static final long REFRESH_TOKEN_VALIDITY = 86400; // 1 day in seconds
    public static final int MIN_TOKEN_LENGTH = 64;
    public static final int MAX_TOKEN_LENGTH = 700;
    private static final int ACCESS_TOKEN_DURATION_INT = 15;
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT);
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    public static final Duration MAX_TOKEN_LIFETIME = Duration.ofDays(7);
    public static final Duration JTI_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT + (long) 1); // Used for blocklist TTL

    // Paseto related
    public static final int PASETO_TOKEN_PARTS_LENGTH = 4;
    public static final String KEY_CONVERSION_ALGORITHM = "Ed25519";
    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB limit

    public static final int PASETO_TOKEN_VERSION_PLACE = 0;
    public static final int PASETO_TOKEN_PURPOSE_PLACE = 1;
    public static final int PASETO_TOKEN_PAYLOAD_PLACE = 2;
    public static final int PASETO_TOKEN_FOOTER_PLACE = 3;

    // Token claims
    public static final int MAX_CLAIMS_SIZE = 1024; // 1KB limit
    public static final String DEFAULT_ISSUER = "${SERVICE_NAME}";
    public static final String DEFAULT_AUDIENCE = "${SERVICE_AUDIENCE}";
    public static final String PASETO_ACCESS_KID = "${PASETO_ACCESS_KID}";
    public static final String PASETO_ACCESS_LOCAL_KID = "${PASETO_ACCESS_LOCAL_KID}";
    public static final String PASETO_REFRESH_LOCAL_KID = "${PASETO_REFRESH_LOCAL_KID}";
    public static final String PASETO_PUBLIC_ACCESS_KID = "${paseto.public.access.kid}";
//    public static final String PASETO_PUBLIC_REFRESH_KID = "${paseto.public.refresh.kid}";
    public static final String PASETO_LOCAL_ACCESS_KID = "${paseto.local.access.kid}";  
    public static final String PASETO_LOCAL_REFRESH_KID = "${paseto.local.refresh.kid}";
    public static final String CLAIM_SUBJECT = "sub";
    public static final String CLAIM_ISSUER = "iss";
    public static final String CLAIM_AUDIENCE = "aud";
    public static final String IAT_CLAIM_KEY = "iat";
    public static final String JTI_CLAIM_KEY = "jti";
    public static final String CLAIM_SCOPE = "scope";

    // Messages
    public static final String TIME_SPENT_VALIDATING_PASETO_TOKENS = "Time spent validating PASETO tokens";
    public static final String TOKEN_VALIDATION_FAILED = "Token validation failed";
    public static final String ERROR_PARSING_CLAIMS = "Error parsing token claims";
    public static final String INVALID_PASETO_TOKEN = "Invalid PASETO token";
    public static final String INVALID_TOKEN_FORMAT = "Invalid token format";
    public static final String INVALID_PASETO_SIGNATURE = "Invalid PASETO signature";
    public static final String INVALID_KEY_ID = "Invalid key ID";
    public static final String KID_IS_MISSING_IN_THE_TOKEN_FOOTER = "KID is missing in the token footer";
    public static final String INVALID_TOKEN_LENGTH = "Invalid token length";
    public static final String TOKEN_EXPIRED = "Token has expired";
    public static final String TOKEN_NOT_YET_VALID = "Token is not yet valid";
    public static final String TOKEN_ISSUED_IN_THE_FUTURE = "Token was issued in the future";
    public static final String INVALID_TOKEN_ISSUER = "Invalid token issuer";
    public static final String INVALID_TOKEN_TYPE = "Invalid token type";
    public static final String INVALID_AUDIENCE = "Invalid audience";
    public static final String INVALID_SUBJECT = "Invalid subject";
    public static final String MISSING_REQUIRED_CLAIM = "Missing required claim: %s. Possible token tampering.";
    public static final String SESSION_ID_MISMATCH = "Session ID mismatch";
    public static final String USER_ACCOUNT_DISABLED = "User account is disabled";
    public static final String NO_ROLES_FOUND_FOR_USER = "No roles found for user";
    public static final String INVALID_USER_SCOPE = "Invalid user scope. Actual: %s";
    public static final String CLAIMS_PAYLOAD_EXCEEDS_MAXIMUM_SIZE = "Claims payload exceeds maximum size";
    public static final String ERROR_SERIALIZING_CLAIMS_TO_JSON = "Error serializing claims to JSON";
    public static final String PAYLOAD_CANNOT_BE_NULL_OR_EMPTY = "Payload cannot be null or empty";
    public static final String PAYLOAD_SIZE_EXCEEDS_MAXIMUM_ALLOWED_SIZE = "Payload size exceeds maximum allowed size";
    public static final String TOKEN_TYPE_CLAIM_IS_MISSING = "Token type claim is missing";
    public static final String TOKEN_ID_IS_MISSING_CANNOT_REVOKE = "Token ID (jti) is missing, cannot revoke.";
    public static final String NULL_PAYLOAD_AFTER_PARSING = "Null payload after parsing";
    public static final String ERROR_EXTRACTING_CLAIMS_FROM_TOKEN = "Error extracting claims from token";
    public static final String INVALID_TOKEN_FORMAT_INVALID_PARTS = "Invalid token format. Invalid parts: %s";
    public static final String TOKEN_REVOCATION_FAILED = "Failed to revoke token: {}";
    public static final String TOKEN_REVOKED_SESSION_ID_MISMATCH = "Token revoked due to Session ID mismatch";
    public static final String TOKEN_REVOKED_EXPIRED = "Token revoked due to expiration or possible replay attack detected";
    public static final String TOKEN_REVOKED_NOT_YET_VALID = "Token revoked because it is not yet valid (possible replay attack)";
    public static final String TOKEN_REVOKED_ISSUED_IN_THE_FUTURE = "Token revoked because it was issued in the future (possible replay attack or another security issue)";
    public static final String TOKEN_REVOKED_INVALID_ISSUER = "Token revoked because the issuer is not valid (possible Issuer Spoofing/Impersonation, replay attack or another security issue)";
    public static final String TOKEN_REVOKED_INVALID_TOKEN_TYPE = "Token revoked because the token type is not valid (possible Token Confusion/Type Mismatch attacks, replay attack or another security issue)";
    public static final String TOKEN_REVOKED_INVALID_AUDIENCE = "Token revoked because the audience is not valid (possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue)";
    public static final String TOKEN_REVOKED_INVALID_SUBJECT = "Token revoked because the subject is not valid (possible Subject Impersonation/Authorization Bypass, replay attack or another security issue)";
    public static final String TOKEN_REVOKED_MISSING_CLAIM = "Token revoked due to missing required claim: %s. Possible token tampering.";
    public static final String TOKEN_REVOKED_GENERIC = "Token revoked due to: %s";
    public static final String TOKEN_VALIDATION_RATELIMIT_KEY = "tokenValidation:";
    public static final String CLAIMS_VALIDATION_RATELIMIT_KEY = "claimsValidation:";


    private TokenConstants() {}
}

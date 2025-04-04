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
    public static final Duration MAX_TOKEN_LIFETIME = Duration.ofDays(7);

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
    public static final String CLAIM_SUBJECT = "sub";
    public static final String CLAIM_ISSUER = "iss";
    public static final String CLAIM_AUDIENCE = "aud";
    public static final String IAT_CLAIM_KEY = "iat";
    public static final String JTI_CLAIM_KEY = "jti";
    public static final String CLAIM_SCOPE = "scope";

    private TokenConstants() {}
}
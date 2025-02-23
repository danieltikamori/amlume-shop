/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PasetoClaims {
    // Reserved claim names
    @JsonProperty("iss")
    public static final String ISSUER = "iss";

    @JsonProperty("sub")
    public static final String SUBJECT = "sub";

    @JsonProperty("aud")
    public static final String AUDIENCE = "aud";

    @JsonProperty("exp")
    public static final String EXPIRATION = "exp";

    @JsonProperty("nbf")
    public static final String NOT_BEFORE = "nbf";

    @JsonProperty("iat")
    public static final String ISSUED_AT = "iat";

    @JsonProperty("jti")
    public static final String PASETO_ID = "jti";

    @JsonProperty("sid")
    public static final String SESSION_ID = "sid";

    @JsonProperty("deviceFingerprint")
    public static final String DEVICE_FINGERPRINT = "deviceFingerprint";

    @JsonProperty("scope")
    public static final String SCOPE = "scope";

    @JsonProperty("type")
    public static final String TOKEN_TYPE = "type";

//    public static final String[] RESERVED_CLAIMS = {ISSUER, SUBJECT, AUDIENCE, EXPIRATION, NOT_BEFORE, ISSUED_AT, PASETO_ID};
//
//    public static final String[] CUSTOM_CLAIMS = {SESSION_ID, deviceFingerprint, scope, tokenType};

//    // Base claim names
//    public static final String[] BASE_CLAIMS = Stream.of(RESERVED_CLAIMS, CUSTOM_CLAIMS)
//            .flatMap(Arrays::stream)
//            .toArray(String[]::new);

    // Footer claim names
    @JsonProperty("kid")
    public static final String KEY_ID = "kid";

    @JsonProperty("wpk")
    public static final String WRAPPED_PASERK = "wpk";

//    public static final String[] FOOTER_CLAIMS = {KEY_ID, WRAPPED_PASERK};
//
//    // All claim names
//    public static final String[] ALL_CLAIMS = Stream.of(BASE_CLAIMS, FOOTER_CLAIMS)
//            .flatMap(Arrays::stream)
//            .toArray(String[]::new);

    private final Map<String, Object> claims;
    private final Map<String, Object> footer;
    private static final DateTimeFormatter RFC3339_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    public PasetoClaims(Map<String, Object> claims, Map<String, Object> footer) {
        this.claims = claims;
        this.footer = footer;
    }

    public PasetoClaims() {
        this.claims = new HashMap<>();
        this.footer = new HashMap<>();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("iss", ISSUER);
        map.put("sub", SUBJECT);
        map.put("aud", AUDIENCE);
        map.put("exp", EXPIRATION);
        map.put("nbf", NOT_BEFORE);
        map.put("iat", ISSUED_AT);
        map.put("jti", PASETO_ID);
        map.put("sid", SESSION_ID);
        map.put(DEVICE_FINGERPRINT, DEVICE_FINGERPRINT);
        map.put(SCOPE, SCOPE);
        map.put("type", TOKEN_TYPE);
        return map;
    }

    public static PasetoClaims fromMap(Map<String, Object> map) {
        PasetoClaims claims = new PasetoClaims();
        claims.setIssuer((String) map.get("iss"));
        claims.setSubject((String) map.get("sub"));
        claims.setAudience((String) map.get("aud"));
        claims.setExpiration((ZonedDateTime) map.get("exp"));
        claims.setNotBefore((ZonedDateTime) map.get("nbf"));
        claims.setIssuedAt((ZonedDateTime) map.get("iat"));
        claims.setTokenId((String) map.get("jti"));
        claims.setSessionId((String) map.get("sid"));
        claims.setDeviceFingerprint((String) map.get(DEVICE_FINGERPRINT));
        claims.setScope((String) map.get(SCOPE));
        claims.setTokenType((String) map.get("type"));
        return claims;
    }

    public static PasetoClaims fromMapForRefreshToken(Map<String, Object> map) {
        PasetoClaims claims = new PasetoClaims();
        claims.setIssuer((String) map.get("iss"));
        claims.setSubject((String) map.get("sub"));
        claims.setAudience((String) map.get("aud"));
        claims.setExpiration((ZonedDateTime) map.get("exp"));
        claims.setNotBefore((ZonedDateTime) map.get("nbf"));
        claims.setIssuedAt((ZonedDateTime) map.get("iat"));
        claims.setTokenId((String) map.get("jti"));
        claims.setTokenType((String) map.get("type"));
        return claims;
    }


    public PasetoClaims(Map<String, Object> footer) {
        this.footer = footer;
        this.claims = new HashMap<>();
    }


    public Map<String, Object> toFooterMap() {
        Map<String, Object> footerMap = new HashMap<>();
        footerMap.put("kid", KEY_ID);
        footerMap.put("wpk", WRAPPED_PASERK);
        return footerMap;
    }

    public static PasetoClaims fromFooterMap(Map<String, Object> map) {
        PasetoClaims footer = new PasetoClaims();
        footer.setKeyId((String) map.get("kid"));
        footer.setWrappedPaserk((String) map.get("wpk"));
        return footer;
    }


    // Setters for reserved claims with validation
    public PasetoClaims setIssuer(String issuer) {
        validateStringClaim(ISSUER, issuer);
        claims.put(ISSUER, issuer);
        return this;
    }

    public PasetoClaims setSubject(String subject) {
        validateStringClaim(SUBJECT, subject);
        claims.put(SUBJECT, subject);
        return this;
    }

    public PasetoClaims setAudience(String audience) {
        validateStringClaim(AUDIENCE, audience);
        claims.put(AUDIENCE, audience);
        return this;
    }

    public PasetoClaims setExpiration(ZonedDateTime expiration) {
        validateDateTimeClaim(EXPIRATION, expiration);
        claims.put(EXPIRATION, formatDateTime(expiration));
        return this;
    }

    public PasetoClaims setNotBefore(ZonedDateTime notBefore) {
        validateDateTimeClaim(NOT_BEFORE, notBefore);
        claims.put(NOT_BEFORE, formatDateTime(notBefore));
        return this;
    }

    public PasetoClaims setIssuedAt(ZonedDateTime issuedAt) {
        validateDateTimeClaim(ISSUED_AT, issuedAt);
        claims.put(ISSUED_AT, formatDateTime(issuedAt));
        return this;
    }

    public PasetoClaims setTokenId(String jti) {
        validateStringClaim(PASETO_ID, jti);
        claims.put(PASETO_ID, jti);
        return this;
    }

    public PasetoClaims setSessionId(String sessionId) {
        validateStringClaim(SESSION_ID, sessionId);
        claims.put(SESSION_ID, sessionId);
        return this;
    }

    public PasetoClaims setDeviceFingerprint(String fingerprint) {
        validateStringClaim(DEVICE_FINGERPRINT, fingerprint);
        claims.put(DEVICE_FINGERPRINT, fingerprint);
        return this;
    }

    public PasetoClaims setScope(String scope) {
        validateStringClaim(scope, scope);
        claims.put(scope, scope);
        return this;
    }

    public PasetoClaims setTokenType(String type) {
        validateStringClaim(TOKEN_TYPE, type);
        claims.put(TOKEN_TYPE, type);
        return this;
    }

    public PasetoClaims setKeyId(String keyId) {
        validateStringClaim(KEY_ID, keyId);
        claims.put(KEY_ID, keyId);
        return this;
    }

    public PasetoClaims setWrappedPaserk(String wrappedPaserk) {
        validateStringClaim(WRAPPED_PASERK, wrappedPaserk);
        claims.put(WRAPPED_PASERK, wrappedPaserk);
        return this;
    }

    // Custom claim setter
    public PasetoClaims addClaim(String name, Object value) {
        validateCustomClaim(name);
        claims.put(name, value);
        return this;
    }

    // Validation methods
    private void validateStringClaim(String claimName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid " + claimName + " claim: Cannot be null or empty");
        }
    }

    private void validateDateTimeClaim(String claimName, ZonedDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid " + claimName + " claim: Cannot be null");
        }
    }

    private void validateCustomClaim(String name) {
        if (isReservedClaim(name)) {
            throw new IllegalArgumentException("Cannot set reserved claim '" + name + "' using addClaim method");
        }
    }

    private boolean isReservedClaim(String name) {
        return Arrays.asList(ISSUER, SUBJECT, AUDIENCE, EXPIRATION,
                NOT_BEFORE, ISSUED_AT, PASETO_ID).contains(name);
    }

    private String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneOffset.UTC)
                .format(RFC3339_FORMATTER);
    }

    // Get the claims map
    public Map<String, Object> getClaims() {
        return new HashMap<>(claims);
    }
}

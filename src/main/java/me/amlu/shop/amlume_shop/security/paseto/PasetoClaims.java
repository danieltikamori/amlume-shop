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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Added for logging parse errors

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // Added for parsing errors
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Added for Objects.requireNonNullElse

public class PasetoClaims {
    private static final Logger log = LoggerFactory.getLogger(PasetoClaims.class);

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

    // Custom claim names used by this application
    @JsonProperty("sid")
    public static final String SESSION_ID = "sid";

    @JsonProperty("deviceFingerprint")
    public static final String DEVICE_FINGERPRINT = "deviceFingerprint";

    @JsonProperty("scope")
    public static final String SCOPE = "scope";

    @JsonProperty("type")
    public static final String TOKEN_TYPE = "type";

    // Footer claim names
    @JsonProperty("kid")
    public static final String KEY_ID = "kid";

    @JsonProperty("wpk")
    public static final String WRAPPED_PASERK = "wpk"; // Assuming this is still needed

    // Combined list of standard and custom claims managed by setters
    private static final String[] MANAGED_CLAIMS = {
            ISSUER, SUBJECT, AUDIENCE, EXPIRATION, NOT_BEFORE, ISSUED_AT, PASETO_ID,
            SESSION_ID, DEVICE_FINGERPRINT, SCOPE, TOKEN_TYPE
    };

    private final Map<String, Object> claims;
    private final Map<String, Object> footer;

    // Use the standard RFC3339 format (ISO 8601) which PASETO expects
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // Constructors
    public PasetoClaims(Map<String, Object> claims, Map<String, Object> footer) {
        this.claims = Objects.requireNonNullElseGet(claims, HashMap::new);
        this.footer = Objects.requireNonNullElseGet(footer, HashMap::new);
    }

    public PasetoClaims() {
        this.claims = new HashMap<>();
        this.footer = new HashMap<>();
    }

    // --- Map Conversion ---

    /**
     * Returns a mutable copy of the main claims map.
     *
     * @return A map containing the claims.
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(this.claims);
    }

    /**
     * Returns a mutable copy of the footer map.
     *
     * @return A map containing the footer claims.
     */
    public Map<String, Object> toFooterMap() {
        return new HashMap<>(this.footer);
    }

    /**
     * Creates a PasetoClaims instance from a map, typically used after parsing a token payload.
     * Handles date string parsing.
     *
     * @param map The map containing claim key-value pairs.
     * @return A new PasetoClaims instance populated from the map.
     */
    public static PasetoClaims fromMap(Map<String, Object> map) {
        PasetoClaims claims = new PasetoClaims();
        if (map == null) return claims; // Return empty if map is null

        claims.setIssuer((String) map.get(ISSUER));
        claims.setSubject((String) map.get(SUBJECT));
        claims.setAudience((String) map.get(AUDIENCE));
        claims.setExpiration(parseDateTime(map.get(EXPIRATION), EXPIRATION));
        claims.setNotBefore(parseDateTime(map.get(NOT_BEFORE), NOT_BEFORE));
        claims.setIssuedAt(parseDateTime(map.get(ISSUED_AT), ISSUED_AT));
        claims.setTokenId((String) map.get(PASETO_ID));
        claims.setSessionId((String) map.get(SESSION_ID));
        claims.setDeviceFingerprint((String) map.get(DEVICE_FINGERPRINT));
        claims.setScope((String) map.get(SCOPE));
        claims.setTokenType((String) map.get(TOKEN_TYPE));

        // Add any other custom claims from the map that aren't handled by specific setters
        map.forEach((key, value) -> {
            if (!isManagedClaim(key) && !isFooterClaim(key)) { // Avoid overwriting managed/footer claims
                claims.addClaim(key, value);
            }
        });

        return claims;
    }

    /**
     * Creates a PasetoClaims instance from a map, specifically for refresh tokens.
     * Handles date string parsing.
     *
     * @param map The map containing claim key-value pairs.
     * @return A new PasetoClaims instance populated from the map.
     */
    public static PasetoClaims fromMapForRefreshToken(Map<String, Object> map) {
        PasetoClaims claims = new PasetoClaims();
        if (map == null) return claims;

        claims.setIssuer((String) map.get(ISSUER));
        claims.setSubject((String) map.get(SUBJECT));
        claims.setAudience((String) map.get(AUDIENCE));
        claims.setExpiration(parseDateTime(map.get(EXPIRATION), EXPIRATION));
        claims.setNotBefore(parseDateTime(map.get(NOT_BEFORE), NOT_BEFORE));
        claims.setIssuedAt(parseDateTime(map.get(ISSUED_AT), ISSUED_AT));
        claims.setTokenId((String) map.get(PASETO_ID));
        claims.setTokenType((String) map.get(TOKEN_TYPE)); // Refresh tokens usually have a type

        // Add other custom claims if necessary for refresh tokens
        map.forEach((key, value) -> {
            if (!isManagedClaim(key) && !isFooterClaim(key)) {
                claims.addClaim(key, value);
            }
        });

        return claims;
    }

    /**
     * Creates a PasetoClaims instance representing only the footer from a map.
     *
     * @param map The map containing footer key-value pairs.
     * @return A new PasetoClaims instance populated with footer claims.
     */
    public static PasetoClaims fromFooterMap(Map<String, Object> map) {
        PasetoClaims footerClaims = new PasetoClaims();
        if (map == null) return footerClaims;

        footerClaims.setKeyId((String) map.get(KEY_ID));
        footerClaims.setWrappedPaserk((String) map.get(WRAPPED_PASERK));
        // Add any other footer claims if present
        map.forEach((key, value) -> {
            if (!isFooterClaim(key)) { // Avoid adding non-footer claims here
                // Maybe log a warning if unexpected keys are found?
            } else if (!key.equals(KEY_ID) && !key.equals(WRAPPED_PASERK)) {
                // Handle other potential footer claims if needed
                footerClaims.addFooterClaim(key, value); // Need an addFooterClaim method
            }
        });
        return footerClaims;
    }


    // --- Setters for Standard Claims ---
    // These add to the main 'claims' map

    public PasetoClaims setIssuer(String issuer) {
        if (issuer != null) { // Allow null to effectively remove claim if needed
            validateStringClaim(ISSUER, issuer);
            claims.put(ISSUER, issuer);
        } else {
            claims.remove(ISSUER);
        }
        return this;
    }

    public PasetoClaims setSubject(String subject) {
        if (subject != null) {
            validateStringClaim(SUBJECT, subject);
            claims.put(SUBJECT, subject);
        } else {
            claims.remove(SUBJECT);
        }
        return this;
    }

    public PasetoClaims setAudience(String audience) {
        if (audience != null) {
            validateStringClaim(AUDIENCE, audience);
            claims.put(AUDIENCE, audience);
        } else {
            claims.remove(AUDIENCE);
        }
        return this;
    }

    public PasetoClaims setExpiration(ZonedDateTime expiration) {
        if (expiration != null) {
            validateDateTimeClaim(EXPIRATION, expiration);
            claims.put(EXPIRATION, formatDateTime(expiration));
        } else {
            claims.remove(EXPIRATION);
        }
        return this;
    }

    public PasetoClaims setNotBefore(ZonedDateTime notBefore) {
        if (notBefore != null) {
            validateDateTimeClaim(NOT_BEFORE, notBefore);
            claims.put(NOT_BEFORE, formatDateTime(notBefore));
        } else {
            claims.remove(NOT_BEFORE);
        }
        return this;
    }

    public PasetoClaims setIssuedAt(ZonedDateTime issuedAt) {
        if (issuedAt != null) {
            validateDateTimeClaim(ISSUED_AT, issuedAt);
            claims.put(ISSUED_AT, formatDateTime(issuedAt));
        } else {
            claims.remove(ISSUED_AT);
        }
        return this;
    }

    public PasetoClaims setTokenId(String jti) {
        if (jti != null) {
            validateStringClaim(PASETO_ID, jti);
            claims.put(PASETO_ID, jti);
        } else {
            claims.remove(PASETO_ID);
        }
        return this;
    }

    // --- Setters for Custom Claims ---
    // These also add to the main 'claims' map

    public PasetoClaims setSessionId(String sessionId) {
        if (sessionId != null) {
            validateStringClaim(SESSION_ID, sessionId);
            claims.put(SESSION_ID, sessionId);
        } else {
            claims.remove(SESSION_ID);
        }
        return this;
    }

    public PasetoClaims setDeviceFingerprint(String fingerprint) {
        if (fingerprint != null) {
            validateStringClaim(DEVICE_FINGERPRINT, fingerprint);
            claims.put(DEVICE_FINGERPRINT, fingerprint);
        } else {
            claims.remove(DEVICE_FINGERPRINT);
        }
        return this;
    }

    public PasetoClaims setScope(String scope) {
        if (scope != null) {
            // Corrected validation and put: use SCOPE constant as key
            validateStringClaim(SCOPE, scope);
            claims.put(SCOPE, scope);
        } else {
            claims.remove(SCOPE);
        }
        return this;
    }

    public PasetoClaims setTokenType(String type) {
        if (type != null) {
            validateStringClaim(TOKEN_TYPE, type);
            claims.put(TOKEN_TYPE, type);
        } else {
            claims.remove(TOKEN_TYPE);
        }
        return this;
    }

    // --- Setters for Footer Claims ---
    // These add to the 'footer' map

    public PasetoClaims setKeyId(String keyId) {
        if (keyId != null) {
            validateStringClaim(KEY_ID, keyId);
            // Corrected: Add to footer map
            footer.put(KEY_ID, keyId);
        } else {
            footer.remove(KEY_ID);
        }
        return this;
    }

    public PasetoClaims setWrappedPaserk(String wrappedPaserk) {
        if (wrappedPaserk != null) {
            validateStringClaim(WRAPPED_PASERK, wrappedPaserk);
            // Corrected: Add to footer map
            footer.put(WRAPPED_PASERK, wrappedPaserk);
        } else {
            footer.remove(WRAPPED_PASERK);
        }
        return this;
    }

    // --- Custom Claim Setters ---

    /**
     * Adds a custom claim to the main claims map.
     * Throws an exception if the name conflicts with a managed claim or footer claim.
     *
     * @param name  The name of the custom claim.
     * @param value The value of the custom claim.
     * @return This PasetoClaims instance for chaining.
     */
    public PasetoClaims addClaim(String name, Object value) {
        validateCustomClaimName(name); // Use a more specific validation method
        if (value != null) {
            claims.put(name, value);
        } else {
            claims.remove(name); // Allow removing custom claims by setting value to null
        }
        return this;
    }

    /**
     * Adds a custom claim to the footer map.
     * Throws an exception if the name conflicts with a managed claim or footer claim.
     *
     * @param name  The name of the custom footer claim.
     * @param value The value of the custom footer claim.
     * @return This PasetoClaims instance for chaining.
     */
    public PasetoClaims addFooterClaim(String name, Object value) {
        validateCustomClaimName(name); // Use the same validation, footer claims shouldn't clash either
        if (value != null) {
            footer.put(name, value);
        } else {
            footer.remove(name);
        }
        return this;
    }

    // --- Validation Methods ---

    private void validateStringClaim(String claimName, String value) {
        // Allow null values to be set (to remove claims), but if non-null, must not be blank.
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("Invalid " + claimName + " claim: Cannot be blank (use null to remove)");
        }
    }

    private void validateDateTimeClaim(String claimName, ZonedDateTime value) {
        // Allow null values to be set (to remove claims).
        // No specific validation needed for non-null ZonedDateTime itself.
        if (value == null) {
            // This case is handled by the setters, no exception needed here.
        }
    }

    private void validateCustomClaimName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Custom claim name cannot be null or blank");
        }
        if (isManagedClaim(name)) {
            throw new IllegalArgumentException("Cannot set managed claim '" + name + "' using addClaim/addFooterClaim method. Use the specific setter (e.g., setSubject()).");
        }
        if (isFooterClaim(name)) {
            throw new IllegalArgumentException("Cannot set managed footer claim '" + name + "' using addClaim/addFooterClaim method. Use the specific setter (e.g., setKeyId()).");
        }
    }

    // --- Helper Methods ---

    private static boolean isManagedClaim(String name) {
        return Arrays.asList(MANAGED_CLAIMS).contains(name);
    }

    private static boolean isFooterClaim(String name) {
        // Define footer claims explicitly
        return KEY_ID.equals(name) || WRAPPED_PASERK.equals(name);
    }

    private String formatDateTime(ZonedDateTime dateTime) {
        // Format to ISO 8601 / RFC3339 string representation expected by PASETO
        return dateTime.withZoneSameInstant(ZoneOffset.UTC).format(RFC3339_FORMATTER);
    }

    private static ZonedDateTime parseDateTime(Object value, String claimName) {
        if (value == null) {
            return null;
        }
        if (value instanceof ZonedDateTime) {
            return (ZonedDateTime) value; // Already parsed
        }
        if (value instanceof String) {
            try {
                // Try parsing using the expected format
                return ZonedDateTime.parse((String) value, RFC3339_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("Could not parse date claim '{}' with value '{}' using format {}: {}", claimName, value, RFC3339_FORMATTER, e.getMessage());
                // Optionally, try other common formats if needed, or just return null/throw
                return null; // Or throw new IllegalArgumentException("Invalid format for claim " + claimName, e);
            }
        }
        log.warn("Unexpected type for date claim '{}': {}", claimName, value.getClass().getName());
        return null; // Or throw
    }

    // --- Getters (remain the same) ---
    public Map<String, Object> getClaims() {
        return new HashMap<>(claims);
    }

    public Map<String, Object> getFooter() {
        return new HashMap<>(footer);
    }
}
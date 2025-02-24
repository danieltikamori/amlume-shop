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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.repositories.RefreshTokenRepository;
import me.amlu.shop.amlume_shop.repositories.UserRepository;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.model.RevokedToken;
import me.amlu.shop.amlume_shop.security.model.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.security.service.KeyManagementFacade;
import org.paseto4j.commons.*;
import org.paseto4j.version4.Paseto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PasetoTokenServiceImpl implements PasetoTokenService {

    public static final String KEY_CONVERSION_ALGORITHM = "Ed25519";
    public static final String KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE = KEY_CONVERSION_ALGORITHM + " algorithm not available";
    public static final String INVALID_TOKEN_FORMAT = "Invalid token format";
    public static final String INVALID_PASETO_TOKEN = "Invalid PASETO Token";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String INVALID_KEY_ID = "Invalid key ID";
    public static final String KID_IS_MISSING_IN_THE_TOKEN_FOOTER = "kid is missing in the token footer";
    public static final String TOKEN_VALIDATION_FAILED = "Token validation failed";
    public static final String ERROR_PARSING_PASETO_CLAIMS = "Error parsing PASETO claims";
    public static final String FAILED_TO_SERIALIZE_PASETO_CLAIMS = "Failed to serialize PASETO claims";
    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DEFAULT_ISSUER = "${SERVICE_NAME}";
    private static final String DEFAULT_AUDIENCE = "${SERVICE_AUDIENCE}";

    //    private final String tokenId = String.valueOf(UUID.randomUUID());
    private static final String PASETO_ACCESS_KID = "${PASETO_ACCESS_KID}";
    private static final String PASETO_ACCESS_LOCAL_KID = "${PASETO_ACCESS_LOCAL_KID}";
    private static final String PASETO_REFRESH_LOCAL_KID = "${PASETO_REFRESH_LOCAL_KID}";

    @Value("${security.token.revoked-cache.max-size}")
    private int maxCacheSize;

//    @Value("${PASETO_WRAPPED_PASERK}")
//    private String wrappedPaserk;

    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB limit

    // Asymmetric keys for public access tokens
    private final PrivateKey accessPrivateKey;
    private final PublicKey accessPublicKey;

    // Symmetric keys for local access tokens and refresh tokens
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;

    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final RevokedTokenRepository revokedTokenRepository;

    private final KeyManagementFacade keyManagementFacade;

    private final Cache<String, Boolean> revokedTokensCache;

    private static final KeyFactory keyFactory;
    static {
        try {
            keyFactory = KeyFactory.getInstance(KEY_CONVERSION_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE, e);
            throw new KeyConversionException(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE, e);
        }
    }

    public PasetoTokenServiceImpl(
//            @Value("${PASETO_ACCESS_PRIVATE_KEY}") String accessPrivateKeyBase64,
//            @Value("${PASETO_ACCESS_PUBLIC_KEY}") String accessPublicKeyBase64,
//            @Value("${PASETO_ACCESS_SECRET_KEY}") String accessSecretKeyString,
//            @Value("${PASETO_REFRESH_SECRET_KEY}") String refreshSecretKeyString,
            HttpServletRequest httpServletRequest, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, EnhancedAuthenticationService enhancedAuthenticationService, RevokedTokenRepository revokedTokenRepository, Cache<String, Boolean> revokedTokensCache, KeyManagementFacade keyManagementFacade) {


        this.httpServletRequest = httpServletRequest;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.enhancedAuthenticationService = enhancedAuthenticationService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.keyManagementFacade = keyManagementFacade;
        // Cache as performance optimization
        this.revokedTokensCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(getConfigurableCacheSize())
                .recordStats()
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        //            KeyFactory keyFactory = KeyFactory.getInstance(KEY_CONVERSION_ALGORITHM);
//
//            this.accessPrivateKey = loadPrivateKey(keyFactory, accessPrivateKeyBase64);
//            this.accessPublicKey = loadPublicKey(keyFactory, accessPublicKeyBase64);
//
//            this.accessSecretKey = loadSecretKey(accessSecretKeyString);
//            this.refreshSecretKey = loadSecretKey(refreshSecretKeyString);

        KeyManagementFacade.KeyPair accessKeys = keyManagementFacade.getAsymmetricKeys("ACCESS");
        this.accessPrivateKey = convertToPrivateKey(Objects.requireNonNull(accessKeys.privateKey()));
        this.accessPublicKey = convertToPublicKey(Objects.requireNonNull(accessKeys.publicKey()));

        this.accessSecretKey = convertToSecretKey(Objects.requireNonNull(keyManagementFacade.getSymmetricKey("ACCESS")));
        this.refreshSecretKey = convertToSecretKey(Objects.requireNonNull(keyManagementFacade.getSymmetricKey("REFRESH")));
    }

    private PrivateKey convertToPrivateKey(String privateKeyString) {
        validateKeyString(privateKeyString, "Private");

        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            return new PrivateKey(
                    keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)),
                    Version.V4
            );
        } catch (InvalidKeySpecException e) {
            log.error("Invalid private key specification", e);
            throw new KeyConversionException("Invalid private key specification", e);
        } catch (Exception e) {
            log.error("Failed to convert private key", e);
            throw new KeyConversionException("Failed to convert private key", e);
        }
    }

    private PublicKey convertToPublicKey(String publicKeyString) {
        if (publicKeyString == null || publicKeyString.isEmpty()) {
            throw new IllegalArgumentException("Public key string cannot be null or empty");
        }

        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            java.security.PublicKey javaPublicKey = java.security.KeyFactory.getInstance(KEY_CONVERSION_ALGORITHM).generatePublic(keySpec);
            return new PublicKey(javaPublicKey, Version.V4);
        } catch (NoSuchAlgorithmException e) {
            log.error(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE, e);
            throw new KeyConversionException(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE, e);
        } catch (InvalidKeySpecException e) {
            log.error("Invalid public key specification", e);
            throw new KeyConversionException("Invalid public key specification", e);
        } catch (Exception e) {
            log.error("Failed to convert public key", e);
            throw new KeyConversionException("Failed to convert public key", e);
        }
    }

    private SecretKey convertToSecretKey(String secretKeyString) {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            throw new IllegalArgumentException("Secret key string cannot be null or empty");
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
            return new SecretKey(decodedKey, Version.V4);
        } catch (Exception e) {
            log.error("Failed to convert secret key", e);
            throw new KeyConversionException("Failed to convert secret key", e);
        }
    }

//    private PrivateKey loadPrivateKey(KeyFactory keyFactory, String base64Key) throws InvalidKeySpecException {
//        if (base64Key == null || base64Key.isEmpty()) return null; // Or throw an exception if required
//        byte[] privateKeyBytes = Base64.getDecoder().decode(base64Key);
//        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
//        return (PrivateKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
//    }
//
//    private PublicKey loadPublicKey(KeyFactory keyFactory, String base64Key) throws InvalidKeySpecException {
//        if (base64Key == null || base64Key.isEmpty()) return null; // Or throw an exception if required
//        byte[] publicKeyBytes = Base64.getDecoder().decode(base64Key);
//        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyBytes);
//        return (PublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
//    }
//
//    private SecretKey loadSecretKey(String base64Key) {
//        return new SecretKey(Base64.getDecoder().decode(base64Key), Version.V4);
//    }

private void validateKeyString(String keyString, String keyType) {
    if (keyString == null || keyString.isEmpty()) {
        throw new IllegalArgumentException(keyType + " key string cannot be null or empty");
    }
}

    @Override
    public String generatePublicAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        try {
            // Create claims for the token
            PasetoClaims claims = createAccessPasetoClaims(userId, accessTokenDuration);
            String payload = objectMapper.writeValueAsString(claims);

            // Validate payload
            validatePayload(payload);

            // Create footer
            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_KID);
//            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_KID, wrappedPaserk); // Assuming PASETO_ACCESS_KID and wrappedPaserk are available
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            return Paseto.sign(accessPrivateKey, payload, footer);
        } catch (JsonProcessingException e) {
            log.error(FAILED_TO_SERIALIZE_PASETO_CLAIMS, e);
            throw new TokenGenerationFailureException(FAILED_TO_SERIALIZE_PASETO_CLAIMS, e);
        } catch (PasetoException e) {
            log.error("Failed to sign PASETO token", e);
            throw new TokenGenerationFailureException("Failed to sign PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Unexpected error during token generation", e);
        }
    }

    @Override
    public String generateLocalAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        try {
            PasetoClaims claims = createAccessPasetoClaims(userId, accessTokenDuration);
            String payload = objectMapper.writeValueAsString(claims);

            validatePayload(payload);

            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_LOCAL_KID);
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            return Paseto.encrypt(accessSecretKey, payload, footer);
        } catch (JsonProcessingException e) {
            log.error(FAILED_TO_SERIALIZE_PASETO_CLAIMS, e);
            throw new TokenGenerationFailureException(FAILED_TO_SERIALIZE_PASETO_CLAIMS, e);
        } catch (PasetoException e) {
            log.error("Failed to encrypt PASETO token", e);
            throw new TokenGenerationFailureException("Failed to encrypt PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
    }

    @Override
    public String generateRefreshToken(User user) {
        try {
            // Create claims for the token
            PasetoClaims claims = createRefreshPasetoClaims(String.valueOf(user.getUserId()), enhancedAuthenticationService.getRefreshTokenDuration());
            String payload = objectMapper.writeValueAsString(claims);

            // Validate payload
            validatePayload(payload);

            // Create footer
            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_REFRESH_LOCAL_KID);
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            String refreshToken = Paseto.encrypt(refreshSecretKey, payload, footer);

            // Hash and store the refresh token (example using BCrypt)
            // amazonq-ignore-next-line
            String hashedRefreshToken = BCrypt.hashpw(refreshToken, BCrypt.gensalt()); // Hash the refresh token
            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setToken(hashedRefreshToken);
            refreshTokenEntity.setUser(user);
            refreshTokenEntity.setExpiryDate(Instant.now().plus(enhancedAuthenticationService.getRefreshTokenDuration())); // Set expiry
            refreshTokenEntity.setCreatedAt(Instant.now());
            refreshTokenEntity.setUpdatedAt(Instant.now());
            refreshTokenEntity.setDeviceFingerprint(httpServletRequest.getHeader("User-Agent"));
            refreshTokenEntity.setRevoked(false);
            // Set other properties (deviceId, etc.) if needed

            // Save the RefreshToken entity to the database
            refreshTokenRepository.save(refreshTokenEntity);

            return refreshToken; // Return the *unhashed* refresh token to the client
        } catch (JsonProcessingException e) {
            log.error("Failed to generate refresh token due to JSON processing error", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token due to JSON processing error", e);
        } catch (PasetoException e) {
            log.error("Failed to generate refresh token due to PASETO encryption error", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token due to PASETO encryption error", e);
        } catch (Exception e) {
            log.error("Failed to generate refresh token", e);
            throw new TokenGenerationFailureException("Failed to generate refresh token", e);
        }
    }

    private Map<String, Object> extractClaimsFromPublicAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException(INVALID_TOKEN_FORMAT);
            }

            // Validate that each part is not empty
            for (String part : parts) {
                if (part == null || part.trim().isEmpty()) {
                    throw new IllegalArgumentException(INVALID_TOKEN_FORMAT);
                }
            }

            String payload = Paseto.parse(accessPublicKey, parts[0] + "." + parts[1], parts[2]);

            // Add null check before parsing
            if (payload == null) {
                throw new ClaimsExtractionFailureException("Null payload after parsing");
            }

            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors as-is
        } catch (Exception e) {
            throw new ClaimsExtractionFailureException("Error extracting claims from token", e);
        }
    }

    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException {
        try {
            String[] parts = splitToken(token);
            String signedMessage = createSignedMessage(parts);
            String footer = extractFooter(parts);
            String payload = parseAndVerifyToken(signedMessage, footer);
            Map<String, Object> claims = parseClaims(payload);
            validateKid(claims);
            validateAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));
            return claims;
        } catch (Exception e) {
            handleValidationException(e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED);
        }
    }

    private String[] splitToken(String token) throws TokenValidationFailureException {
        String[] parts = token.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new TokenValidationFailureException(INVALID_TOKEN_FORMAT);
        }
        return parts;
    }

    private String createSignedMessage(String[] parts) {
        return parts[0] + "." + parts[1];
    }

    private String extractFooter(String[] parts) {
        return parts.length == 3 ? parts[2] : "";
    }

    private String parseAndVerifyToken(String signedMessage, String footer) throws SignatureException {
        return Paseto.parse(accessPublicKey, signedMessage, footer);
    }

    private Map<String, Object> parseClaims(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, new TypeReference<>() {
        });
    }

    private void validateKid(Map<String, Object> claims) throws TokenValidationFailureException {
        String kid = (String) claims.get("kid");
        if (kid == null || kid.isEmpty()) {
            throw new TokenValidationFailureException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER);
        }
        if (!PASETO_ACCESS_KID.equals(kid)) {
            throw new TokenValidationFailureException(INVALID_KEY_ID);
        }
    }

    private void handleValidationException(Exception e) {
        if (e instanceof SignatureException) {
            log.error("Invalid PASETO signature", e);
            throw new InvalidTokenSignatureException("Invalid PASETO signature");
        } else if (e instanceof JsonProcessingException) {
            log.error(ERROR_PARSING_PASETO_CLAIMS, e);
            throw new TokenValidationFailureException(ERROR_PARSING_PASETO_CLAIMS);
        } else if (e instanceof IllegalArgumentException) {
            log.error(INVALID_PASETO_TOKEN, e);
            throw new TokenValidationFailureException(INVALID_PASETO_TOKEN);
        } else {
            log.error("Failed to validate public access PASETO token", e);
        }
    }

    @Override
    public Map<String, Object> validateLocalAcessToken(String token) throws TokenValidationFailureException {
        try {
            // Split token into header, payload, and footer
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new TokenValidationFailureException(INVALID_TOKEN_FORMAT);
            }

            String payload = Paseto.decrypt(accessSecretKey, parts[0] + "." + parts[1], parts[2]);

            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // Get kid from footer claims
            String kid = (String) claims.get("kid");
            if (kid == null || kid.isEmpty()) {
                throw new TokenValidationFailureException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER);
            }

            // Verify key ID matches

            if (!PASETO_ACCESS_LOCAL_KID.equals(kid)) {
                throw new TokenValidationFailureException(INVALID_KEY_ID);
            }

            validateAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));

            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(ERROR_PARSING_PASETO_CLAIMS, e);
            throw new TokenValidationFailureException(ERROR_PARSING_PASETO_CLAIMS);

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(INVALID_PASETO_TOKEN, e);
            throw new TokenValidationFailureException(INVALID_PASETO_TOKEN);

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate local access PASETO token", e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED);
        }
    }


    @Override
    public Map<String, Object> validateRefreshToken(String token) throws TokenValidationFailureException { // Separate refresh token validation
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new TokenValidationFailureException(INVALID_TOKEN_FORMAT);
            }

            String payload = Paseto.decrypt(refreshSecretKey, parts[0] + "." + parts[1], parts[2]);

            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // Get kid from footer claims
            String kid = (String) claims.get("kid");
            if (kid == null || kid.isEmpty()) {
                throw new TokenValidationFailureException(KID_IS_MISSING_IN_THE_TOKEN_FOOTER);
            }

            // Verify key ID matches
            if (!PASETO_REFRESH_LOCAL_KID.equals(kid)) {
                throw new TokenValidationFailureException(INVALID_KEY_ID);
            }

            validateRefreshTokenClaims(claims, String.valueOf(TokenType.REFRESH_TOKEN)); // Specific validation rules for refresh token claims
            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(ERROR_PARSING_PASETO_CLAIMS, e);
            throw new TokenValidationFailureException(ERROR_PARSING_PASETO_CLAIMS);

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(INVALID_PASETO_TOKEN, e);
            throw new TokenValidationFailureException(INVALID_PASETO_TOKEN);

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate refresh PASETO token", e);
            throw new TokenValidationFailureException(TOKEN_VALIDATION_FAILED);
        }
    }

//    private Map<String, Object> createClaims(String username, Duration validity) {
//        Instant now = Instant.now();
//        String currentSessionId = httpServletRequest.getSession().getId();
//
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("subject", username);
//        claims.put("jti", UUID.randomUUID().toString());
//        claims.put("sid", currentSessionId);
//        claims.put("iat", formatInstant(now));
//        claims.put("exp", formatInstant(now.plus(validity)));
//        claims.put("nbf", formatInstant(now)); // Not Before (now) - optional, but recommended
//        claims.put("deviceFingerprint", generateDeviceFingerprint());
//        claims.put("scope", determineUserScope());
//        return claims;
//    }

//    private String formatInstant(Instant instant) {
//        return DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).format(instant);
//    }

    @Override
    public PasetoClaims createAccessPasetoClaims(String userId, Duration validity) {
        User user = findUserById(userId);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String userScope = enhancedAuthenticationService.determineUserScope();

        return buildAccessPasetoClaims(user, now, validity, userScope);
    }

    private User findUserById(String userId) {
        try {
            return userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format", e);
        }
    }

    private PasetoClaims buildAccessPasetoClaims(User user, ZonedDateTime now, Duration validity, String userScope) {
        return new PasetoClaims()
                .setIssuer(DEFAULT_ISSUER)
                .setSubject(String.valueOf(user.getUserId()))
                .setAudience(DEFAULT_AUDIENCE)
                .setExpiration(now.plus(validity))
                .setNotBefore(now)
                .setIssuedAt(now)
                .setTokenId(generateTokenId())
                .setSessionId(httpServletRequest.getSession().getId())
                .setScope(userScope)
                .setTokenType(String.valueOf(TokenType.ACCESS_TOKEN));
    }

    @Override
    public PasetoClaims createRefreshPasetoClaims(String userId, Duration validity) {
        User user;
        try {
            user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format", e);
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return new PasetoClaims()
                .setIssuer(DEFAULT_ISSUER)
                .setSubject(String.valueOf(user.getUserId()))
                .setAudience(DEFAULT_AUDIENCE)
                .setExpiration(now.plus(validity))
                .setNotBefore(now)
                .setIssuedAt(now)
                .setTokenId(generateTokenId()) // Implement this method to generate a unique identifier
                .setTokenType(String.valueOf(TokenType.REFRESH_TOKEN));
    }

    @Override
    public PasetoClaims createPasetoFooterClaims(String keyId) {
        return new PasetoClaims()
                .setKeyId(keyId);
    }

//    // Use the method below if using wrapped paserk too.
//    @Override
//    public PasetoClaims createPasetoFooterClaims(String PASETO_ACCESS_KID, String wrappedPaserk) {
//        return new PasetoClaims()
//                .setKeyId(PASETO_ACCESS_KID)
//                .setWrappedPaserk(wrappedPaserk);
//    }

    private String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void validateAccessTokenClaims(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {

        // TOCHECK: is it true that exp, nbf, and iat are automatically validated by the PASETO library?
//        Instant now = Instant.now();
//
//        Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
//        if (now.isAfter(exp)) {
//            throw new TokenValidationFailureException("Token has expired");
//        }
//
//        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));
//        if (now.isBefore(nbf)) {
//            throw new TokenValidationFailureException("Token is not yet valid");
//        }
//
//        Instant iat = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));
//        if (now.isBefore(iat)) {
//            throw new TokenValidationFailureException("Token is not yet valid");
//        }

        String type = (String) claims.get("type");
        if (!expectedType.equals(type)) {
            throw new TokenValidationException("Invalid token type");
        }

        String issuer = (String) claims.get("iss");
        if (!DEFAULT_ISSUER.equals(issuer)) {
            throw new TokenValidationException("Invalid issuer");
        }

        String audience = (String) claims.get("aud");
        if (!DEFAULT_AUDIENCE.equals(audience)) {
            throw new TokenValidationException("Invalid audience");
        }

//        // Validate session ID - Use in sticky session
//        String currentSessionId = httpServletRequest.getSession().getId();
//        if (!currentSessionId.equals(claims.get("sid"))) {
//            throw new TokenValidationFailureException("Invalid session ID");
//        }

    }

    @Override
    public void validateRefreshTokenClaims(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        try {
            String userId = (String) claims.get("sub"); // Get the user ID from the token
            User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.isEnabled()) {
                throw new TokenValidationFailureException("User account is disabled");
            }

            //        Instant now = Instant.now();
//
//        Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
//        if (now.isAfter(exp)) {
//            throw new TokenValidationFailureException("Token has expired");
//        }
//
//        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));
//        if (now.isBefore(nbf)) {
//            throw new TokenValidationFailureException("Token is not yet valid");
//        }
//
//        Instant iat = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));
//        if (now.isBefore(iat)) {
//            throw new TokenValidationFailureException("Token is not yet valid");
//        }

            String type = (String) claims.get("type");
            if (!expectedType.equals(type)) {
                throw new TokenValidationException("Invalid token type");
            }

            if (!DEFAULT_ISSUER.equals(claims.get("iss"))) {
                throw new TokenValidationException("Invalid issuer");
            }

            if (!DEFAULT_AUDIENCE.equals(claims.get("aud"))) {
                throw new TokenValidationException("Invalid audience");
            }
        } catch (NumberFormatException e) {
            throw new TokenValidationFailureException("Invalid user ID format", e);
        } catch (UserNotFoundException | TokenValidationException e) {
            throw new TokenValidationFailureException(e.getMessage(), e);
        }
    }

    @Override
    public void validatePayload(String payload) throws TokenGenerationFailureException {
        if (payload == null || payload.isEmpty()) {
            throw new TokenGenerationFailureException("Payload cannot be null or empty");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_SIZE) {
            throw new TokenGenerationFailureException("Payload size exceeds maximum allowed size");
        }
    }

    @Transactional
    @Override
    public void revokeToken(String token, String reason) {
        try {
            // Parse token to get its ID and expiration
            Map<String, Object> claims = extractClaimsFromPublicAccessToken(token);
            String tokenId = (String) claims.get("jti");
            if (tokenId == null) {
                throw new SecurityException("Token ID missing");
            }

            String username = (String) claims.get("sub");
            if (username == null) {
                throw new SecurityException("Subject missing");
            }
            // Calculate remaining time until token expiration
            Instant expiration = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse((String) claims.get("exp")));

            // Check if token is already revoked
            if (isTokenRevoked(tokenId)) {
                log.info("Token already revoked: {}", tokenId);
                return;
            }

            // Save revoked token to database
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenId(tokenId);
            revokedToken.setUsername(username);
            revokedToken.setExpirationDate(expiration);
            revokedToken.setRevokedAt(Instant.now());
            revokedToken.setReason(reason);

            revokedTokenRepository.save(revokedToken);

            // Add to revoked tokens cache with remaining time until expiration
            revokedTokensCache.put(tokenId, true);

            log.info("Token revoked successfully: {}", tokenId);

        } catch (PasetoException e) {
            log.error("Error revoking token", e);
            throw new SecurityException("Could not revoke token", e);
        }
    }


    @Transactional
    @Override
    public void revokeAllUserTokens(String username, String reason) {
        try {
            List<RevokedToken> userTokens = revokedTokenRepository.findByUsername(username);
            for (RevokedToken token : userTokens) {
                token.setRevokedAt(Instant.now());
                token.setReason(reason);
                revokedTokensCache.put(token.getTokenId(), true);
            }
            revokedTokenRepository.saveAll(userTokens);
            log.info("Revoked all tokens for user: {}", username);
        } catch (Exception e) {
            log.error("Error revoking tokens for user: {}", username, e);
            throw new SecurityException("Could not revoke tokens for user", e);
        }
    }

    private boolean isTokenRevoked(String tokenId) {
        try {
            // First check cache
            Boolean cachedResult = revokedTokensCache.getIfPresent(tokenId);
            if (cachedResult != null) {
                return cachedResult;
            }

            // If not in cache, check database
            boolean isRevoked = revokedTokenRepository.existsByTokenId(tokenId);
            if (isRevoked) {
                revokedTokensCache.put(tokenId, true);
            }
            return isRevoked;
        } catch (Exception e) {
            log.error("Error checking token revocation status for tokenId: {}", tokenId, e);
            return true; // Fail-safe: assume token is revoked if there's an error
        }
    }

//    private boolean isTokenRevoked(String token) {
//        try {
//            PasetoToken parsedToken = Pasetos.V4..parser()
//                    .setKey(key)
//                    .parse(token);
//
//            String tokenId = parsedToken.getJti()
//                    .orElseThrow(() -> new SecurityException("Token ID missing"));
//
//            return revokedTokensCache.getIfPresent(tokenId) != null;
//
//        } catch (PasetoException e) {
//            log.warn("Error checking token revocation status", e);
//            return true; // Fail secure
//        }
//    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Map<String, Object> claims = validatePublicAccessToken(token);
            Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
            return Instant.now().isAfter(exp);
        } catch (TokenValidationFailureException e) {
            log.error("Failed to validate token expiration", e);
            return true;
        }
    }

    // Cleanup expired tokens
    @Scheduled(cron = "0 0 * * * *") // Run hourly
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            Instant now = Instant.now();
            revokedTokenRepository.deleteByExpirationDateBefore(now);
            revokedTokensCache.cleanUp();
            log.debug("Cleaned up expired tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }

    private int getConfigurableCacheSize() {
        return maxCacheSize;
    }

    // TODO: Invalidate old refresh tokens. e.g., by deleting it from the database or marking it as used).
    // TODO: Revocation:  Provide a mechanism to revoke refresh tokens (e.g., if a user logs out or if a token is suspected of being compromised).
    //
    //Rate Limiting:  Implement rate limiting on the refresh token grant endpoint to prevent brute-force attacks.

    public AuthResponse refreshAccessToken(String refreshToken) throws InvalidTokenException, TokenValidationFailureException {
        try {
            RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new InvalidTokenException(INVALID_REFRESH_TOKEN));

            User user = refreshTokenEntity.getUser(); // Get the user from the RefreshToken entity


            if (user == null) {
                throw new InvalidTokenException(INVALID_REFRESH_TOKEN);
            }

            Map<String, Object> refreshTokenClaims = validateRefreshToken(refreshToken);

            // Extract user information from refresh token
            String userId = refreshTokenClaims.get("sub").toString(); // Extract userId
            User existingUser = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found during refresh token validation"));

            if (existingUser == null) {
                throw new InvalidTokenException(INVALID_REFRESH_TOKEN);
            }

            // Generate new tokens
            String newAccessToken = generatePublicAccessToken(String.valueOf(user.getUserId()), enhancedAuthenticationService.getAccessTokenDuration());
            String newRefreshToken = generateRefreshToken(user);

            // Revoke/delete old refresh token (Delete is more secure)
            refreshTokenRepository.delete(refreshTokenEntity);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (UserNotFoundException e) {
            log.error("User not found during refresh token validation", e);
            throw new InvalidRefreshTokenException("Invalid refresh token: " + e.getMessage());
        } catch (TokenValidationFailureException e) {
            log.error("Refresh token validation failed", e);
            throw e; // Re-throw the validation exception
        } catch (Exception e) {
            log.error("Unexpected error during refresh token validation", e);
            throw new InvalidTokenException(INVALID_REFRESH_TOKEN, e);
        }
    }

    // Finish implementation if we would use wrapped paserk
//    private String getWrappedPaserkFromStorage() {
//        // Implement logic to retrieve the wpk from your secure storage (e.g., database, secrets manager)
//        // Example (using Spring's @Value):
//        // @Value("${PASETO_WRAPPED_PASERK}")
//        // private String wrappedPaserk;
//        // return wrappedPaserk;
//
//        return wrappedPaserk; // Replace with your actual implementation
//    }
//
//    private PASERK unwrapPaserk(String wpk, SecretKey masterKey) {
//        // Implement logic to decrypt the wpk using your master key
//        // You'll likely need a PASERK library that supports wrapping/unwrapping.
//        // Example (conceptual):
//        // return PASERK.unwrap(wpk, masterKey);
//
//        return null; // Replace with your actual implementation
//    }

}


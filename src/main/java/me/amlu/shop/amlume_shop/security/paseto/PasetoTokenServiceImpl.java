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
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PasetoTokenServiceImpl implements PasetoTokenService {

    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String INVALID_TOKEN_FORMAT = "Invalid token format";
    private static final String DEFAULT_ISSUER = "${SERVICE_NAME}";
    private static final String DEFAULT_AUDIENCE = "${SERVICE_AUDIENCE}";

    //    private final String tokenId = String.valueOf(UUID.randomUUID());
    private static final String keyId = "${KEY_ID}";
    @Value("${PASETO_WRAPPED_PASERK}")
    private String wrappedPaserk;

    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB limit

    // Asymmetric keys for public access tokens
    private final PrivateKey accessPrivateKey;
    private final PublicKey accessPublicKey;

    // Symmetric keys for local access tokens and refresh tokens
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;

    private final SecureRandom secureRandom;

    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final RevokedTokenRepository revokedTokenRepository;

    private final Cache<String, Boolean> revokedTokensCache;

    public PasetoTokenServiceImpl(
            @Value("${PASETO_ACCESS_PRIVATE_KEY}") String accessPrivateKeyBase64,
            @Value("${PASETO_ACCESS_PUBLIC_KEY}") String accessPublicKeyBase64,
            @Value("${PASETO_ACCESS_SECRET_KEY}") String accessSecretKeyString,
            @Value("${PASETO_REFRESH_SECRET_KEY}") String refreshSecretKeyString,
            HttpServletRequest httpServletRequest, UserRepository userRepository,
            SecureRandom secureRandom, RefreshTokenRepository refreshTokenRepository, EnhancedAuthenticationService enhancedAuthenticationService, RevokedTokenRepository revokedTokenRepository, Cache<String, Boolean> revokedTokensCache) {


        this.httpServletRequest = httpServletRequest;
        this.userRepository = userRepository;
        this.secureRandom = secureRandom;
        this.refreshTokenRepository = refreshTokenRepository;
        this.enhancedAuthenticationService = enhancedAuthenticationService;
        this.revokedTokenRepository = revokedTokenRepository;
        // Cache as performance optimization
        this.revokedTokensCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10000)
                .recordStats()
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");

            this.accessPrivateKey = loadPrivateKey(keyFactory, accessPrivateKeyBase64);
            this.accessPublicKey = loadPublicKey(keyFactory, accessPublicKeyBase64);

            this.accessSecretKey = loadSecretKey(accessSecretKeyString);
            this.refreshSecretKey = loadSecretKey(refreshSecretKeyString);

        } catch (Exception e) {
            log.error("Error loading private key", e);
            throw new PrivateKeyLoadException("Failed to load private key", e);
        }
    }

    private PrivateKey loadPrivateKey(KeyFactory keyFactory, String base64Key) throws InvalidKeySpecException {
        if (base64Key == null || base64Key.isEmpty()) return null; // Or throw an exception if required
        byte[] privateKeyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return (PrivateKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    private PublicKey loadPublicKey(KeyFactory keyFactory, String base64Key) throws InvalidKeySpecException {
        if (base64Key == null || base64Key.isEmpty()) return null; // Or throw an exception if required
        byte[] publicKeyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return (PublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
    }

    private SecretKey loadSecretKey(String base64Key) {
        return new SecretKey(Base64.getDecoder().decode(base64Key), Version.V4);
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
            PasetoClaims footerClaims = createPasetoFooterClaims(keyId, wrappedPaserk);
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            return Paseto.sign(accessPrivateKey, payload, footer);
        } catch (Exception e) {
            log.error("Failed to generate PASETO token", e);
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
    }

    @Override
    public String generateLocalAccessToken(String userId, Duration accessTokenDuration) throws TokenGenerationFailureException {
        try {
            PasetoClaims claims = createAccessPasetoClaims(userId, accessTokenDuration);
            String payload = objectMapper.writeValueAsString(claims);

            validatePayload(payload);

            PasetoClaims footerClaims = createPasetoFooterClaims(keyId, wrappedPaserk);
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            return Paseto.encrypt(accessSecretKey, payload, footer);
        } catch (Exception e) {
            log.error("Failed to generate PASETO token", e);
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
            PasetoClaims footerClaims = createPasetoFooterClaims(keyId, wrappedPaserk);
            String footer = objectMapper.writeValueAsString(footerClaims); // Convert to JSON string

            String refreshToken = Paseto.encrypt(refreshSecretKey, payload, footer);

            // Hash and store the refresh token (example using BCrypt)
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

            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors as-is
        } catch (Exception e) {
            throw new ClaimsExtractionFailureException("Error extracting claims from token", e);
        }
    }

    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException {
        try {
            // Verify signature using public key
            String[] parts = token.split("\\.");
            if (parts.length < 2 || parts.length > 3) { // Check for 2 or 3 parts
                throw new TokenValidationFailureException(INVALID_TOKEN_FORMAT);
            }

            String signedMessage = parts[0] + "." + parts[1]; // Header and payload
            String footer = parts.length == 3 ? parts[2] : ""; // Footer (if present)

            String payload = Paseto.parse(accessPublicKey, signedMessage, footer); // Parse and verify

            // Parse and validate claims
            Map<String, Object> claims = objectMapper.readValue(payload, new TypeReference<>() {
            });

            // 1. Get kid from footer claims
            String kid = (String) claims.get("kid");
            if (kid == null || kid.isEmpty()) {
                throw new TokenValidationFailureException("kid is missing in the token footer");
            }

            // 2. Get wpk (you'll need to fetch this from your secure storage)
            String wpk = getWrappedPaserkFromStorage(); // Implement this!

            // 3. Unwrap the PASERK (if not already unwrapped)
            PASERK paserk = unwrapPaserk(wpk, masterKey); // Implement this using your master key

            // 4. Get the correct public key from the PASERK using the kid
            PublicKey publicKey = paserk.getPublicKey(kid); // PASERK should have a method to get key by ID

            if (publicKey == null) {
                throw new TokenValidationFailureException("No public key found for kid: " + kid);
            }

            validateAccessTokenClaims(claims);

            return claims;

        } catch (SignatureException e) { // Catch SignatureException specifically
            log.error("Invalid PASETO signature", e);
            throw new InvalidTokenSignatureException("Invalid PASETO signature"); // Or a more specific exception

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error("Error parsing PASETO claims", e);
            throw new TokenValidationFailureException("Error parsing PASETO claims");

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error("Invalid PASETO Token", e);
            throw new TokenValidationFailureException("Invalid PASETO Token");

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate public access PASETO token", e);
            throw new TokenValidationFailureException("Token validation failed");
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
            validateAccessTokenClaims(claims);

            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error("Error parsing PASETO claims", e);
            throw new TokenValidationFailureException("Error parsing PASETO claims");

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error("Invalid PASETO Token", e);
            throw new TokenValidationFailureException("Invalid PASETO Token");

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate local access PASETO token", e);
            throw new TokenValidationFailureException("Token validation failed");
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

            validateRefreshTokenClaims(claims); // Specific validation rules for refresh token claims
            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error("Error parsing PASETO claims", e);
            throw new TokenValidationFailureException("Error parsing PASETO claims");

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error("Invalid PASETO Token", e);
            throw new TokenValidationFailureException("Invalid PASETO Token");

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate refresh PASETO token", e);
            throw new TokenValidationFailureException("Token validation failed");
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
        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String userScope = enhancedAuthenticationService.determineUserScope();

        return new PasetoClaims()
                .setIssuer(DEFAULT_ISSUER)
                .setSubject(String.valueOf(user.getUserId()))
                .setAudience(DEFAULT_AUDIENCE)
                .setExpiration(now.plus(validity))
                .setNotBefore(now)
                .setIssuedAt(now)
                .setTokenId(generateTokenId()) // Generate a unique identifier
                .setSessionId(httpServletRequest.getSession().getId())
                .setScope(userScope)
                .setTokenType(String.valueOf(TokenType.ACCESS_TOKEN));
//                .addClaim(null, null);
    }

    @Override
    public PasetoClaims createRefreshPasetoClaims(String userId, Duration validity) {
        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found"));
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
    public PasetoClaims createPasetoFooterClaims(String keyId, String wrappedPaserk) {
        return new PasetoClaims()
                .setKeyId(keyId)
                .setWrappedPaserk(wrappedPaserk);
    }

    private String generateTokenId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void validateAccessTokenClaims(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();

        Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
        if (now.isAfter(exp)) {
            throw new TokenValidationFailureException("Token has expired");
        }

        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));
        if (now.isBefore(nbf)) {
            throw new TokenValidationFailureException("Token is not yet valid");
        }

        Instant iat = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));
        if (now.isBefore(iat)) {
            throw new TokenValidationFailureException("Token is not yet valid");
        }

//        // Validate session ID - Use in sticky session
//        String currentSessionId = httpServletRequest.getSession().getId();
//        if (!currentSessionId.equals(claims.get("sid"))) {
//            throw new TokenValidationFailureException("Invalid session ID");
//        }

    }

    @Override
    public void validateRefreshTokenClaims(Map<String, Object> claims) throws TokenValidationFailureException {
        String userId = (String) claims.get("sub"); // Get the user ID from the token
        User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user == null) {
            throw new TokenValidationFailureException("Invalid user ID");
        }

        Instant now = Instant.now();

        Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
        if (now.isAfter(exp)) {
            throw new TokenValidationFailureException("Token has expired");
        }

        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));
        if (now.isBefore(nbf)) {
            throw new TokenValidationFailureException("Token is not yet valid");
        }

        Instant iat = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));
        if (now.isBefore(iat)) {
            throw new TokenValidationFailureException("Token is not yet valid");
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
        List<RevokedToken> userTokens = revokedTokenRepository.findByUsername(username);
        for (RevokedToken token : userTokens) {
            token.setRevokedAt(Instant.now());
            token.setReason(reason);
            revokedTokensCache.put(token.getTokenId(), true);
        }
        revokedTokenRepository.saveAll(userTokens);
        log.info("Revoked all tokens for user: {}", username);
    }

    private boolean isTokenRevoked(String tokenId) {
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

    // TODO: Invalidate old refresh tokens. e.g., by deleting it from the database or marking it as used).
    // TODO: Revocation:  Provide a mechanism to revoke refresh tokens (e.g., if a user logs out or if a token is suspected of being compromised).
    //
    //Rate Limiting:  Implement rate limiting on the refresh token grant endpoint to prevent brute-force attacks.

    public AuthResponse refreshAccessToken(String refreshToken) throws InvalidTokenException, TokenValidationFailureException {
        try {
            String hashedRefreshToken = BCrypt.hashpw(refreshToken, BCrypt.gensalt()); // Hash the incoming token
            RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(hashedRefreshToken)
                    .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

            User user = refreshTokenEntity.getUser(); // Get the user from the RefreshToken entity


            if (user == null) {
                throw new InvalidTokenException("Invalid refresh token");
            }

            Map<String, Object> refreshTokenClaims = validateRefreshToken(refreshToken);

            // Extract user information from refresh token
            String userId = refreshTokenClaims.get("sub").toString(); // Extract userId
            User existingUser = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (existingUser == null) {
                throw new InvalidTokenException("Invalid refresh token");
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
            throw new InvalidRefreshTokenException("Invalid refresh token");
        } catch (TokenValidationFailureException e) {
            log.error("Refresh token validation failed", e);
            throw e; // Re-throw the validation exception
        } catch (Exception e) {
            log.error("Unexpected error during refresh token validation", e);
            throw new InvalidTokenException("Invalid refresh token", e);
        }
    }

    private String getWrappedPaserkFromStorage() {
        // Implement logic to retrieve the wpk from your secure storage (e.g., database, secrets manager)
        // Example (using Spring's @Value):
        // @Value("${PASETO_WRAPPED_PASERK}")
        // private String wrappedPaserk;
        // return wrappedPaserk;

        return wrappedPaserk; // Replace with your actual implementation
    }

    private PASERK unwrapPaserk(String wpk, SecretKey masterKey) {
        // Implement logic to decrypt the wpk using your master key
        // You'll likely need a PASERK library that supports wrapping/unwrapping.
        // Example (conceptual):
        // return PASERK.unwrap(wpk, masterKey);

        return null; // Replace with your actual implementation
    }
}


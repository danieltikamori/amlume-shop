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
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.repositories.RefreshTokenRepository;
import me.amlu.shop.amlume_shop.repositories.UserRepository;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.model.RevokedTokenRepository;
import me.amlu.shop.amlume_shop.security.paseto.util.KeyConverter;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenClaimValidator;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenGenerator;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.security.service.KeyManagementFacade;
import org.paseto4j.commons.*;
import org.paseto4j.version4.Paseto;
import org.springframework.beans.factory.annotation.Value;
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

import static me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl.ErrorMessages.*;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PasetoTokenServiceImpl implements PasetoTokenService {

    // Constants as enum for better organization and type safety
    enum ErrorMessages {
        KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE(KEY_CONVERSION_ALGORITHM + " algorithm not available"),
        INVALID_TOKEN_FORMAT("Invalid token format"),
        INVALID_PASETO_TOKEN("Invalid PASETO Token"),
        INVALID_REFRESH_TOKEN("Invalid refresh token"),
        INVALID_KEY_ID("Invalid key ID"),
        KID_MISSING("kid is missing in the token footer"),
        TOKEN_VALIDATION_FAILED("Token validation failed"),
        ERROR_PARSING_CLAIMS("Error parsing PASETO claims"),
        FAILED_TO_SERIALIZE_CLAIMS("Failed to serialize PASETO claims"),
        KID_IS_MISSING_IN_THE_TOKEN_FOOTER("kid is missing in the token footer");

        private final String value;

        ErrorMessages(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    // Static configuration constants
    private static final int PASETO_TOKEN_PARTS_LENGTH = 4;
    public static final String KEY_CONVERSION_ALGORITHM = "Ed25519";
    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    private static final String DEFAULT_ISSUER = "${SERVICE_NAME}";
    private static final String DEFAULT_AUDIENCE = "${SERVICE_AUDIENCE}";
    private static final String PASETO_ACCESS_KID = "${PASETO_ACCESS_KID}";
    private static final String PASETO_ACCESS_LOCAL_KID = "${PASETO_ACCESS_LOCAL_KID}";
    private static final String PASETO_REFRESH_LOCAL_KID = "${PASETO_REFRESH_LOCAL_KID}";
    private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB limit
    public static final String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final KeyFactory KEY_FACTORY = initializeKeyFactory();
    private static final ObjectMapper OBJECT_MAPPER = initializeObjectMapper();
//    private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(10); // DEPRECATED: Use Constants.CLOCK_SKEW_TOLERANCE instead

    // Performance tuning constants
//    private static final int INITIAL_CLAIMS_MAP_CAPACITY = 16;
//
//    @Value("${token.processing.batch.size}")
//    private static int TOKEN_PROCESSING_BATCH_SIZE;
//    private static final Duration TOKEN_PROCESSING_TIMEOUT = Duration.ofSeconds(30);

    // Security constants
    private static final int MIN_TOKEN_LENGTH = 64;
    private static final int MAX_TOKEN_LENGTH = 700;
    private static final Duration MAX_TOKEN_LIFETIME = Duration.ofDays(7);

    // Thread pool for async operations
//    private Executor tokenBackgroundTasksExecutor;

    // Metrics
    private final PrometheusMeterRegistry meterRegistry;

    private final Counter tokenGenerationCounter = Counter.build()
            .name("paseto_token_generation_total")
            .help("Total number of PASETO tokens generated")
            .register();

    private final Counter tokenValidationCounter;
    private final Timer tokenValidationTimer;


    /**
     * Add a summary metric to track the latency of token validation.
     * This can help identify performance bottlenecks and potential issues.
     * The summary includes quantiles to provide a sense of the distribution of validation times.
     * For example, the 50th percentile (median) and 95th percentile are included with 5% tolerance.
     * This means that 95% of validation times are within 5% of the reported 95th percentile value.
     * The summary also includes a count of the number of observations (validations) and a sum of the observed values (total validation time).
     * This can be used to calculate the average validation time.
     * The summary is registered with the default Prometheus registry, which is used by the Prometheus client library to expose metrics to Prometheus.
     * The summary is also used by the Prometheus client library to expose metrics to Prometheus.
     * <p>
     * Usage example:
     * // Usage example in the validation method
     * public boolean validateToken(String token) {
     * Summary.Timer timer = tokenValidationLatency.startTimer();
     * try {
     * // Your token validation logic here
     * return true;
     * } finally {
     * timer.observeDuration();
     * }
     * }
     * <p>
     * public String generateToken(User user) {
     * try {
     * // Token generation logic here
     * String token = // ... generate token ...
     * tokenGenerationCounter.inc();
     * return token;
     * } catch (Exception e) {
     * log.error("Failed to generate token", e);
     * throw new TokenGenerationException("Failed to generate token", e);
     * }
     * }
     * <p>
     * This will track the time spent validating tokens and expose the results to Prometheus.
     * The results can be used to monitor the performance of the token validation process and identify potential issues.
     * The results can also be used to optimize the token validation process and improve performance.
     * The results can be used to identify trends and patterns in the token validation process and identify potential issues.
     * The results can be used to identify the impact of changes to the token validation process and identify potential issues.
     */
    private final Summary tokenValidationLatency = Summary.build()
            .name("paseto_token_validation_seconds")
            .help("Time spent validating PASETO tokens")
            .quantile(0.5, 0.05)   // Add 50th percentile with 5% tolerance
            .quantile(0.95, 0.01)  // Add 95th percentile with 1% tolerance
            .register();


    // Add caching
    private final LoadingCache<String, Optional<User>> userCache;
    private Cache<String, Boolean> revokedTokensCache;

    // Key holders - immutable
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class KeyPairHolder {
        PrivateKey privateKey;
        PublicKey publicKey;

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class SecretKeyHolder {
        SecretKey accessKey;
        SecretKey refreshKey;
    }

    // Key management
    private KeyPairHolder keyPairHolder;
    private SecretKeyHolder secretKeyHolder;

    // --- Key accessors ---
    // IMPORTANT: Getters for keys with null checks

    /**
     * Usage example:
     * public String signToken(String payload) {
     * try {
     * return Paseto.sign(payload, getAccessPrivateKey());
     * } catch (Exception e) {
     * throw new TokenSigningException("Failed to sign token", e);
     * }
     * }
     * <p>
     * public boolean verifyToken(String token) {
     * try {
     * return Paseto.verify(token, getAccessPublicKey());
     * } catch (Exception e) {
     * throw new TokenVerificationException("Failed to verify token", e);
     * }
     * }
     *
     * @return
     */
    protected PrivateKey getAccessPrivateKey() {
        return Objects.requireNonNull(keyPairHolder.getPrivateKey(), "Access private key not initialized");
    }

    protected PublicKey getAccessPublicKey() {
        return Objects.requireNonNull(keyPairHolder.getPublicKey(), "Access public key not initialized");
    }

    protected SecretKey getAccessSecretKey() {
        return Objects.requireNonNull(secretKeyHolder.getAccessKey(), "Access secret key not initialized");
    }

    protected SecretKey getRefreshSecretKey() {
        return Objects.requireNonNull(secretKeyHolder.getRefreshKey(), "Refresh secret key not initialized");
    }


    // Required services and repositories
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final KeyManagementFacade keyManagementFacade;
    private final TokenCleanupService tokenCleanupService;
//    private final TokenClaimValidator tokenClaimValidator;
//    private final TokenGenerator tokenGenerator;
//    private final KeyConverter keyConverter;


    // --- Configuration Properties ---

    @Value("${security.token.revoked-cache.max-size}")
    private int maxCacheSize;

    @Value("${token.cache.expiry.hours:1}")
    private int cacheExpiryHours;

    // --- Constructor ---

    public PasetoTokenServiceImpl(Timer tokenValidationTimer, Counter tokenValidationCounter, PrometheusMeterRegistry meterRegistry, RefreshTokenRepository refreshTokenRepository, RevokedTokenRepository revokedTokenRepository, KeyManagementFacade keyManagementFacade,
                                  HttpServletRequest httpServletRequest, UserRepository userRepository, EnhancedAuthenticationService enhancedAuthenticationService, TokenCleanupService tokenCleanupService) {
        this.meterRegistry = meterRegistry;
        this.tokenValidationTimer = Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);
        this.tokenCleanupService = tokenCleanupService;
        this.tokenValidationCounter = Counter.build()
                .name("paseto_token_validation_total")
                .help("Total number of PASETO token validations")
                .register();
        this.userCache = CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build(CacheLoader.from(userRepository::findByUsername));
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.keyManagementFacade = keyManagementFacade;
        this.httpServletRequest = httpServletRequest;
        this.userRepository = userRepository;
        this.enhancedAuthenticationService = enhancedAuthenticationService;
        this.revokedTokensCache = createRevokedTokensCache();
    }

    // --- Static initializers ---

    /**
     * Initializes the key factory for the specified algorithm.
     * This method is called during the initialization of the service.
     * It uses the KeyFactory.getInstance() method to get an instance of the KeyFactory for the specified algorithm.
     *
     * @return
     * @throws KeyConversionException   if the algorithm is not available.
     * @throws NoSuchAlgorithmException if the algorithm is not available.
     * @throws InvalidKeySpecException  if the key specification is invalid.
     */
    private static KeyFactory initializeKeyFactory() {
        try {
            return KeyFactory.getInstance(KEY_CONVERSION_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error(String.valueOf(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
            throw new KeyConversionException(String.valueOf(KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
        }
    }

    // Lazy initialization of ObjectMapper

    /**
     * Initializes the ObjectMapper for the service.
     * This method is called during the initialization of the service.
     * It configures the ObjectMapper to use the JavaTimeModule and to not write dates as timestamps.
     * It also registers the JavaTimeModule with the ObjectMapper.
     * It uses the ObjectMapper.registerModule() method to register the JavaTimeModule with the ObjectMapper.
     * It uses the ObjectMapper.configure() method to configure the ObjectMapper.
     * <p>
     * Usage example:
     * // Using the static ObjectMapper
     * Map<String, Object> claims = OBJECT_MAPPER.readValue(jsonString, new TypeReference<>() {});
     *
     * @return
     */
    private static ObjectMapper initializeObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // --- Initialization ---

    @PostConstruct
    private void initialize() {
        this.revokedTokensCache = createRevokedTokensCache();
        initializeKeys();
    }

    private Cache<String, Boolean> createRevokedTokensCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpiryHours, TimeUnit.HOURS)
                .maximumSize(maxCacheSize)
                .recordStats()
                .build();
    }

    private void initializeKeys() {
        try {
            // Initialize asymmetric keys
            KeyManagementFacade.KeyPair accessKeys = keyManagementFacade.getAsymmetricKeys("ACCESS");
            this.keyPairHolder = new KeyPairHolder(
                    convertToPrivateKey(validateKey(accessKeys.privateKey(), "private")),
                    convertToPublicKey(validateKey(accessKeys.publicKey(), "public"))
            );

            // Initialize symmetric keys
            this.secretKeyHolder = new SecretKeyHolder(
                    convertToSecretKey(validateKey(keyManagementFacade.getSymmetricKey("ACCESS"), "access secret")),
                    convertToSecretKey(validateKey(keyManagementFacade.getSymmetricKey("REFRESH"), "refresh secret"))
            );
        } catch (Exception e) {
            log.error("Failed to initialize keys", e);
            throw new KeyInitializationException("Failed to initialize keys", e);
        }
    }

    private <T> T validateKey(T key, String keyType) {
        return Objects.requireNonNull(key, keyType + " key cannot be null");
    }

    // --- Key Conversion ---

    /**
     * Converts a base64 encoded private key string to a {@link PrivateKey} object.
     *
     * @param privateKeyString the base64 encoded private key string
     * @return a {@link PrivateKey} object
     * @throws KeyConversionException if the private key string is invalid
     */
    private PrivateKey convertToPrivateKey(String privateKeyString) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            return new PrivateKey(
                    KEY_FACTORY.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)),
                    Version.V4
            );
        } catch (InvalidKeySpecException e) {
            throw new KeyConversionException("Invalid private key specification", e);
        } catch (Exception e) {
            log.error("Failed to convert private key", e);
            throw new KeyConversionException("Failed to convert private key", e);
        }
    }

    /**
     * Converts a base64 encoded public key string to a {@link PublicKey} object.
     *
     * @param publicKeyString the base64 encoded public key string
     * @return a {@link PublicKey} object
     * @throws KeyConversionException if the public key string is invalid
     */
    private PublicKey convertToPublicKey(String publicKeyString) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            return new PublicKey(
                    KEY_FACTORY.generatePublic(new X509EncodedKeySpec(publicKeyBytes)),
                    Version.V4
            );
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

    // --- Rate Limiting ---

    // Rate limiter for token validation to prevent abuse and ensure rate limiting
    private final RateLimiter tokenValidationRateLimiter = RateLimiter.create(100.0);

    // Rate limiter for token generation to prevent abuse and ensure rate limiting
    private final RateLimiter tokenGenerationRateLimiter = RateLimiter.create(100.0); // 100 tokens per second

    // Rate limiter for token refresh to prevent abuse and ensure rate limiting
    private final RateLimiter tokenRefreshRateLimiter = RateLimiter.create(100.0);

    // --- Token Generation ---

    /**
     * Generates a public access token for the specified user ID with the given duration.
     * <p>
     * This method creates and signs a PASETO token using the user's ID and the specified
     * access token duration. The token includes claims and a footer, both serialized to JSON
     * format. The method ensures payload validation and handles exceptions related to JSON
     * processing and token signing.
     * <p>
     * Format (4 parts):
     * public.v4.payload.footer
     * Where the payload is a JSON string containing the claims, and the footer is a JSON string
     * containing the footer claims.
     * The footer claims include the key ID used for signing the token.
     * The key ID is retrieved from the application properties.
     * The key ID is used to retrieve the private key for signing the token.
     * The private key is retrieved from the key management facade.
     * The private key is used to sign the token.
     * The signed token as a string.
     *
     * @param userId              the ID of the user for whom the token is generated
     * @param accessTokenDuration the duration for which the token is valid
     * @return the signed PASETO token as a string
     * @throws TokenGenerationFailureException if the token generation process fails
     */
    @Override
    public String generatePublicAccessToken(String userId, Duration accessTokenDuration) throws
            TokenGenerationFailureException {
        tokenGenerationRateLimiter.acquire();
        try {
            // Create claims for the token
            PasetoClaims claims = createAccessPasetoClaims(userId, accessTokenDuration);
            String payload = OBJECT_MAPPER.writeValueAsString(claims);

            // Validate payload
            validatePayload(payload);

            // Create footer
            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_KID);
//            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_KID, wrappedPaserk); // Assuming PASETO_ACCESS_KID and wrappedPaserk are available
            String footer = OBJECT_MAPPER.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            tokenGenerationCounter.inc();
            return Paseto.sign(getAccessPrivateKey(), payload, footer);
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to sign PASETO token", e);
            throw new TokenGenerationFailureException("Failed to sign PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Unexpected error during token generation", e);
        }
    }

    /**
     * Generates a local access token for the given user ID with the specified duration.
     * Local access tokens are encrypted with a secret key and are only valid for a short duration.
     * The generated token is signed with the access private key and the KID is set to the local KID.
     * <p>
     * Format (4 parts):
     * local.v4.payload.footer
     * Where the payload is a JSON string containing the claims, and the footer is a JSON string
     * containing the footer claims.
     * The footer claims include the key ID used for signing the token.
     * The key ID is set to the local KID.
     * The secret key is retrieved from the key management facade.
     * The secret key is used to encrypt the payload.
     * The encrypted token as a string.
     *
     * @param userId              the ID of the user for whom the token is generated
     * @param accessTokenDuration the duration for which the token is valid
     * @return the signed PASETO token as a string
     * @throws TokenGenerationFailureException if the token generation process fails
     */
    @Override
    public String generateLocalAccessToken(String userId, Duration accessTokenDuration) throws
            TokenGenerationFailureException {
        try {
            PasetoClaims claims = createAccessPasetoClaims(userId, accessTokenDuration);
            String payload = OBJECT_MAPPER.writeValueAsString(claims);

            validatePayload(payload);

            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_ACCESS_LOCAL_KID);
            String footer = OBJECT_MAPPER.writeValueAsString(footerClaims); // Convert to JSON string

            // Create final token string with footer
            return Paseto.encrypt(getAccessSecretKey(), payload, footer);
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to encrypt PASETO token", e);
            throw new TokenGenerationFailureException("Failed to encrypt PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Token generation failed", e);
        }
    }

    /**
     * Generates a refresh token for the specified user.
     * <p>
     * The refresh token is generated using the user's ID and the refresh token duration.
     * The token includes claims and a footer, both serialized to JSON format.
     * The footer claims include the key ID used for signing the token.
     * The key ID is set to the local KID.
     * The secret key is retrieved from the key management facade.
     * The secret key is used to encrypt the payload.
     * The encrypted token as a string.
     * <p>
     * The generated refresh token is hashed and stored in the database.
     * <p>
     * Format (4 parts):
     * local.v4.payload.footer
     * Where the payload is a JSON string containing the claims, and the footer is a JSON string
     * containing the footer claims.
     * The footer claims include the key ID used for signing the token.
     * The key ID is set to the local KID.
     * The secret key is retrieved from the key management facade.
     * The secret key is used to encrypt the payload.
     * The encrypted token as a string.
     *
     * @param user the user for whom the token is generated
     * @return the signed PASETO token as a string
     * @throws TokenGenerationFailureException if the token generation process fails
     */
    @Override
    public String generateRefreshToken(User user) {
        tokenGenerationRateLimiter.acquire(); // Apply rate limiting

        try {
            // Create claims for the token
            PasetoClaims claims = createRefreshPasetoClaims(String.valueOf(user.getUserId()), enhancedAuthenticationService.getRefreshTokenDuration());
            String payload = OBJECT_MAPPER.writeValueAsString(claims);

            // Validate payload
            validatePayload(payload);

            // Create footer
            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_REFRESH_LOCAL_KID);
            String footer = OBJECT_MAPPER.writeValueAsString(footerClaims); // Convert to JSON string

            String refreshToken = Paseto.encrypt(getRefreshSecretKey(), payload, footer);

            // Hash and store the refresh token in the database
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

    public Map<String, Object> extractClaimsFromPublicAccessToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null or empty"); // Checks if parameter is null
//        if (token.trim().isEmpty()) {
//            throw new IllegalArgumentException("Token cannot be empty"); // Checks if parameter is empty/whitespace
//        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != PASETO_TOKEN_PARTS_LENGTH) {
                throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT));
            }

            // Validate that each part is not empty
            for (String part : parts) {
                if (part == null || part.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT));
                }
            }

            String payload = Paseto.parse(getAccessPublicKey(), parts[2], parts[3]);

            // Null check after parsing
            if (payload == null) {
                throw new ClaimsExtractionFailureException("Null payload after parsing");
            }

            return OBJECT_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors as-is
        } catch (Exception e) {
            throw new ClaimsExtractionFailureException("Error extracting claims from token", e);
        }
    }

    private Timer createTokenValidationTimer(MeterRegistry registry) {
        return Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);
    }


    @Override
    public Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException {
        tokenValidationRateLimiter.acquire();

        try {
            //            Timer.Sample sample = Timer.start();
            Timer.Sample sample = Timer.start(meterRegistry);
            log.debug("Validating public access token");

            validateTokenStringLength(token);

            String[] parts = splitToken(token);
            String signedMessage = createSignedMessage(parts);
            String footer = extractFooter(parts);
            String payload = parseAndVerifyToken(signedMessage, footer);
            Map<String, Object> claims = parseClaims(payload);
            validateKid(claims);
            validateAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));
            sample.stop(tokenValidationTimer);
            log.debug("Public access token validation successful");
            return claims;
        } catch (Exception e) {
            handlePublicAccessTokenValidationException(e);
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED));
        }
    }


    private void validateTokenStringLength(String token) throws InvalidTokenLengthException {
        Objects.requireNonNull(token, "Token cannot be null");

        if (token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidTokenLengthException("Token length out of acceptable range");
        }
    }


    private String[] splitToken(String token) throws InvalidTokenFormatException {
        String[] parts = token.split("\\.");
        if (parts.length > PASETO_TOKEN_PARTS_LENGTH || parts.length < PASETO_TOKEN_PARTS_LENGTH) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
        return parts;
    }

    private String createSignedMessage(String[] parts) throws InvalidTokenFormatException {
        if (parts[2].isBlank()) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
//        return parts[0] + "." + parts[1];
        return parts[2]; // Payload part of the token
    }

    private String extractFooter(String[] parts) {
        if (parts[3].isBlank()) {
            throw new InvalidTokenFormatException(String.valueOf(INVALID_TOKEN_FORMAT));
        }
//        return parts.length == PASETO_TOKEN_PARTS_LENGTH ? parts[3] : ""; // Use this if the there's tokens with AND without footers
        return parts[3]; // Footer part of the token
    }

    private String parseAndVerifyToken(String signedMessage, String footer) throws SignatureException {
        try {
            return Paseto.parse(getAccessPublicKey(), signedMessage, footer);
        } catch (SignatureException e) {
            log.error("Invalid PASETO signature", e);
            throw new SignatureException("Invalid PASETO signature", e);
        }
    }

    private Map<String, Object> parseClaims(String payload) throws JsonProcessingException {
        try {
            if (payload.isBlank()) {
                throw new IllegalArgumentException("Payload is null or empty");
            }
            return OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException("Error parsing claims", e);
        }
    }

    private void validateKid(Map<String, Object> claims) throws InvalidKeyIdException {
        String kid = (String) claims.get("kid");
        if (kid.isBlank()) {
            throw new InvalidKeyIdException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
        }
        if (!PASETO_ACCESS_KID.equals(kid)) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
        }
    }

    private void handlePublicAccessTokenValidationException(Exception e) throws TokenValidationFailureException {
        if (e instanceof SignatureException) {
            log.error("Invalid PASETO signature", e);
            throw new InvalidTokenSignatureException("Invalid PASETO signature");
        } else if (e instanceof JsonProcessingException) {
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ERROR_PARSING_CLAIMS));
        } else if (e instanceof IllegalArgumentException) {
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN));
        } else if (e instanceof TokenValidationFailureException) {
            throw (TokenValidationFailureException) e;
        } else {
            log.error("Failed to validate public access PASETO token", e);
            throw new TokenValidationFailureException("Failed to validate public access PASETO token");
        }
    }

    @Override
    public Map<String, Object> validateLocalAcessToken(String token) throws TokenValidationFailureException {
        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(getAccessSecretKey(), parts[2], parts[3]);

            Map<String, Object> claims = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });

            // Validate kid from footer claims
            validateKid(claims);

            validateAccessTokenClaims(claims, String.valueOf(TokenType.ACCESS_TOKEN));

            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ERROR_PARSING_CLAIMS));

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN));

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate local access PASETO token", e);
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED));
        }
    }


    @Override
    public Map<String, Object> validateRefreshToken(String token) throws TokenValidationFailureException { // Separate refresh token validation
        tokenValidationRateLimiter.acquire();

        try {
            validateTokenStringLength(token);

            // Split token into header, payload, and footer
            String[] parts = splitToken(token);

            String payload = Paseto.decrypt(getRefreshSecretKey(), parts[2], parts[3]);

            Map<String, Object> claims = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });

            // Validate kid from footer claims
            validateKid(claims);

            validateRefreshTokenClaims(claims, String.valueOf(TokenType.REFRESH_TOKEN)); // Specific validation rules for refresh token claims
            return claims;

        } catch (JsonProcessingException e) { // Handle JSON parsing exceptions
            log.error(String.valueOf(ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ERROR_PARSING_CLAIMS));

        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN));

        } catch (Exception e) { // Catch other exceptions (e.g., during claim validation)
            log.error("Failed to validate refresh PASETO token", e);
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED));
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
                .setTokenId(generateTokenId()) // Method to generate a unique identifier
                .setTokenType(String.valueOf(TokenType.REFRESH_TOKEN));
    }

    @Override
    public void validatePayload(String payload) throws TokenGenerationFailureException {
        if (payload.isBlank()) {
            throw new TokenGenerationFailureException("Payload cannot be null or empty");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_SIZE) {
            throw new TokenGenerationFailureException("Payload size exceeds maximum allowed size");
        }
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
    public void validateAccessTokenClaims(Map<String, Object> claims, String expectedType) throws
            TokenValidationFailureException {
        // TOCHECK: is it true that exp, nbf, and iat are automatically validated by the PASETO library?

        // Check if token is revoked
        validateNotRevoked(claims);

        // Validate required claims
        validateRequiredClaims(claims);

        // Validate expiration
        validateExpiration(claims);

        // Validate not before
        validateNotBefore(claims);

        // Validate issuance time
        validateIssuanceTime(claims);

        // Validate issuer
        validateIssuer(claims);

//        Validate Token Type
        validateTokenType(claims, expectedType);

//        Validate audience
        validateAudience(claims);

        // Validate subject
        validateSubject(claims);

//        // Validate session ID - Use in sticky session
//        String currentSessionId = httpServletRequest.getSession().getId();
//        if (!currentSessionId.equals(claims.get("sid"))) {
//            throw new TokenValidationFailureException("Invalid session ID");
//        }

    }

    @Override
    public void validateRefreshTokenClaims(Map<String, Object> claims, String expectedType) throws
            TokenValidationFailureException {
        try {
            String userId = (String) claims.get("sub"); // Get the user ID from the token
            User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!user.isEnabled()) {
                throw new TokenValidationFailureException("User account is disabled");
            }

            // Check if token is revoked
            validateNotRevoked(claims);

            // Validate required claims
            validateRequiredClaims(claims);

            // Validate expiration
            validateExpiration(claims);

            // Validate not before
            validateNotBefore(claims);

            // Validate issuance time
            validateIssuanceTime(claims);

            // Validate issuer
            validateIssuer(claims);

//        Validate Token Type
            validateTokenType(claims, expectedType);

//        Validate audience
            validateAudience(claims);

            // Validate subject
            validateSubject(claims);
        } catch (NumberFormatException e) {
            throw new TokenValidationFailureException("Invalid user ID format", e);
        } catch (UserNotFoundException | TokenValidationException e) {
            throw new TokenValidationFailureException(e.getMessage(), e);
        }
    }

    // Methods to validate token claims
    private void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException {

        // Extract the token ID from the claims
        String tokenId = claims.get("jti").toString();
        // First check the cache
        Boolean isRevoked = revokedTokensCache.getIfPresent(tokenId);
        if (Boolean.TRUE.equals(isRevoked)) {
            throw new TokenRevokedException("Token has been revoked");
        }

        // If not in cache, check the database
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            // Add to cache for future checks
            revokedTokensCache.put(tokenId, true);
            throw new TokenRevokedException("Token has been revoked");
        }
    }

    private void validateRequiredClaims(Map<String, Object> claims) throws InvalidTokenException {
        List<String> requiredClaims = Arrays.asList("exp", "iat", "nbf", "sub", "iss");
        List<String> missingClaims = requiredClaims.stream()
                .filter(claim -> !claims.containsKey(claim))
                .toList(); // Unmodifiable list

        if (!missingClaims.isEmpty()) {
            // Token is missing required claims
            // Revoke the token as it's invalid
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "Missing required claims: " + String.join(", ", missingClaims) + ". Possible token tampering.");
            log.error("Missing required claims: {}", String.join(", ", missingClaims));
            throw new InvalidTokenException("Missing required claims: " + String.join(", ", missingClaims) + ". Possible token tampering.");
        }
    }

    public void validateExpiration(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant now = Instant.now();

        Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
        if (now.isAfter(exp.plus(Constants.CLOCK_SKEW_TOLERANCE))) {
            // Token has expired, so revoke it
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "Token has expired or possible replay attack detected");
            log.error("At expiration validation, Token with id {} has expired or possible replay attack detected", tokenId);
            throw new TokenValidationFailureException("Token has expired");
        }

    }

    private void validateNotBefore(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant nbf = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("nbf").toString()));
        if (Instant.now().minus(Constants.CLOCK_SKEW_TOLERANCE).isBefore(nbf)) {
            // Token is not yet valid, so revoke it as possible replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At not before validation, Token is not yet valid (possible replay attack)");
            log.error("At not before validation, Token with id {} is not yet valid, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException("Token is not yet valid");
        }
    }

    private void validateIssuanceTime(Map<String, Object> claims) throws TokenValidationFailureException {
        Instant iat = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("iat").toString()));
        if (Instant.now().minus(Constants.CLOCK_SKEW_TOLERANCE).isBefore(iat)) {
            // Token issuance time is not valid, so revoke it as possible replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At issuance time validation, Token issuance time is not valid (possible replay attack or another security issue)");
            log.error("At issuance time validation, Token with id {} issuance time is not valid, possible replay attack or another security issue", tokenId);
            throw new TokenValidationFailureException("Token issuance time is not valid");
        }
    }

    private void validateIssuer(Map<String, Object> claims) throws TokenValidationFailureException {
        String issuer = claims.get("iss").toString();
        if (!DEFAULT_ISSUER.equals(issuer)) {
            // Token issuer is not valid, so revoke it as possible Issuer Spoofing/Impersonation, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At issuer validation, Token issuer is not valid (possible Issuer Spoofing/Impersonation, replay attack or another security issue)");
            log.error("At issuer validation, Token with id {} issuer is not valid, expecting {} but found {}.Possible Issuer Spoofing/Impersonation, replay attack or another security issue", tokenId, DEFAULT_ISSUER, issuer);
            throw new TokenValidationFailureException("Invalid token issuer");
        }
    }

    private void validateTokenType(Map<String, Object> claims, String expectedType) throws TokenValidationFailureException {
        String tokenType = claims.get("type").toString();
        if (!expectedType.equals(tokenType)) {
            // Token type is not valid, so revoke it as possible Token Confusion/Type Mismatch attacks, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At token type validation, Token type is not valid (possible Token Confusion/Type Mismatch attacks, replay attack or another security issue)");
            log.error("At token type validation, Token type is not valid, expected {} but got {}. Possible Token Confusion/Type Mismatch attacks, replay attack or another security issue", expectedType, tokenType);
            throw new TokenValidationFailureException("Invalid token type");
        }
    }

    private void validateAudience(Map<String, Object> claims) throws TokenValidationFailureException {
        String audience = claims.get("aud").toString();
        if (!DEFAULT_AUDIENCE.equals(audience)) {
            // Token audience is not valid, so revoke it as possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At audience validation, Token audience is not valid (possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue)");
            log.error("At audience validation, Token with id {} audience is not valid, expecting {} but found {}. Possible Audience Restriction Bypass/Token Reuse attacks, replay attack or another security issue", tokenId, DEFAULT_AUDIENCE, audience);
            throw new TokenValidationFailureException("Invalid audience");
        }
    }

    private void validateSubject(Map<String, Object> claims) throws TokenValidationFailureException {

        String subject = claims.get("sub").toString();
        String userId = httpServletRequest.getSession().getAttribute("userId").toString();
        if (!subject.equals(userId)) {
            // Token subject is not valid, so revoke it as possible Subject Impersonation/Authorization Bypass, replay attack or another security issue
            String tokenId = claims.get("jti").toString();
            tokenCleanupService.revokeToken(tokenId, "At subject validation, Token subject is not valid (possible Subject Impersonation/Authorization Bypass, replay attack or another security issue)");
            log.error("At subject validation, Token with id {} subject is not valid, expecting {} but found {}. Possible Subject Impersonation/Authorization Bypass, replay attack or another security issue", tokenId, userId, subject);
            throw new TokenValidationFailureException("Invalid subject");
        }
    }

    // Methods to revoke tokens are at TokenCleanupService

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Map<String, Object> claims = validatePublicAccessToken(token);
            Instant exp = Instant.from(DateTimeFormatter.ofPattern(YYYY_MM_DD_T_HH_MM_SS_Z).withZone(ZoneOffset.UTC).parse(claims.get("exp").toString()));
            return Instant.now().isAfter(exp.plus(Constants.CLOCK_SKEW_TOLERANCE));
        } catch (TokenValidationFailureException e) {
            log.error("Failed to validate token expiration", e);
            return true;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    // Cleanup expired tokens
//    The Class TokenCleanupService is responsible for periodically cleaning up expired tokens from the cache and database.
//    It can be scheduled to run at regular intervals, such as every hour or every day, to ensure that the cache and database are kept up-to-date with the current state of token revocation.
//    The cleanup process involves two steps:
//    1. Remove expired tokens from the cache.
//    2. Delete expired tokens from the database.
//
//    This approach helps to maintain the integrity of the cache and database, ensuring that they remain in sync with the current state of token revocation.


    private int getConfigurableCacheSize() {
        return maxCacheSize;
    }

    // TODO: Revocation:  Provide a mechanism to revoke refresh tokens (e.g., if a user logs out or if a token is suspected of being compromised).
    //
    //Rate Limiting:  Implemented rate limiting on the refresh token grant endpoint to prevent brute-force attacks.

    public AuthResponse refreshAccessToken(String refreshToken) throws
            InvalidTokenException, TokenValidationFailureException {

        tokenRefreshRateLimiter.acquire(); // Apply rate limiting
        try {
            RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new InvalidTokenException(String.valueOf(INVALID_REFRESH_TOKEN)));

            User user = refreshTokenEntity.getUser(); // Get the user from the RefreshToken entity


            if (user == null) {
                throw new InvalidTokenException(String.valueOf(INVALID_REFRESH_TOKEN));
            }

            Map<String, Object> refreshTokenClaims = validateRefreshToken(refreshToken);

            // Extract user information from refresh token
            String userId = refreshTokenClaims.get("sub").toString(); // Extract userId
            User existingUser = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found during refresh token validation"));

            if (existingUser == null) {
                throw new InvalidTokenException(String.valueOf(INVALID_REFRESH_TOKEN));
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
            throw new InvalidTokenException(String.valueOf(INVALID_REFRESH_TOKEN), e);
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


    // Async token validation methods

//    /**
//     * Validate token asynchronously.
//     * This method is intended for use in scenarios where token validation can be performed in the background.
//     * It returns a CompletableFuture that will be completed with a boolean indicating whether the token is valid or not.
//     * This allows the caller to continue processing without waiting for the validation to complete.
//     * <p>
//     * Example usage:
//     * CompletableFuture<Boolean> validationFuture = tokenService.validateTokenAsync(token);
//     * validationFuture.thenAccept(valid -> {
//     * if (isValid) {
//     * // Token is valid, proceed with processing
//     * } else {
//     * // Token is invalid, handle error
//     * }
//     * });
//     * <p>
//     * Note: This method uses a separate executor for token processing to avoid blocking the main thread.
//     * The executor is configured with a fixed thread pool size to limit the number of concurrent token validations.
//     * <p>
//     *
//     * @param token
//     * @return
//     * @see CompletableFuture
//     * @see Executor
//     * @see Executors#newFixedThreadPool(int)
//     * @see Async
//     */
//    @Async
//    public CompletableFuture<Boolean> validatePublicAccessTokenAsync(String token, MeterRegistry meterRegistry) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                Timer.Sample sample = Timer.start(meterRegistry);
//                validatePublicAccessToken(token);
//                sample.stop(tokenValidationTimer);
//                return true;
//            } catch (Exception e) {
//                log.warn("Async token validation failed", e);
//                return false;
//            }
//        }, tokenBackgroundTasksExecutor);
//    }
//
//    @Async
//    public CompletableFuture<Boolean> validateLocalAccessTokenAsync(String token, MeterRegistry meterRegistry) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                Timer.Sample sample = Timer.start(meterRegistry);
//                validateLocalAcessToken(token);
//                sample.stop(tokenValidationTimer);
//                return true;
//            } catch (Exception e) {
//                log.warn("Async token validation failed", e);
//                return false;
//            }
//        }, tokenBackgroundTasksExecutor);
//    }
//
//    @Async
//    public CompletableFuture<Boolean> validateRefreshTokenAsync(String token, MeterRegistry meterRegistry) {
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                Timer.Sample sample = Timer.start(meterRegistry);
//                validateRefreshToken(token);
//                sample.stop(tokenValidationTimer);
//                return true;
//            } catch (Exception e) {
//                log.warn("Async token validation failed", e);
//                return false;
//            }
//        }, tokenBackgroundTasksExecutor);
//    }

    // Batch token processing implementation

//    /**
//     * Process a batch of tokens asynchronously.
//     * This method is intended for use in scenarios where token validation can be performed in the background for a batch of tokens.
//     * It returns a list of valid tokens.
//     * <p>
//     * Example usage:
//     * List<String> tokens = Arrays.asList("token1", "token2", "token3");
//     * List<String> validTokens = tokenValidator.processBatchTokens(tokens);
//     * <p>
//     *
//     * @param tokens
//     * @return
//     * @see List
//     * @see Arrays#asList(Object[])
//     * @see Stream#parallel()
//     * @see Stream#limit(long)
//     * @see CompletableFuture#allOf(CompletableFuture[])
//     * @see Optional#isPresent()
//     */
//    public List<String> processBatchPublicAccessTokens(List<String> tokens) {
//        return tokens.parallelStream()
//                .filter(Objects::nonNull)
//                .limit(TOKEN_PROCESSING_BATCH_SIZE)
//                .map(this::processPublicAccessTokenSafely)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .toList(); // Unmodifiable list
////                .collect(Collectors.toList());
//    }
//
//    // Process in chunks implementation - This will allow for better memory management.
//    //This will prevent a single large batch from failing, and causing the entire process to stop.
//    @Async
//    public List<String> processBatchInChunksPublicAccessTokens(List<String> tokens) {
//        return Lists.partition(tokens, TOKEN_PROCESSING_BATCH_SIZE)
//                .stream()
//                .flatMap(chunk -> chunk.parallelStream()
//                        .filter(Objects::nonNull)
//                        .map(this::processPublicAccessTokenSafely)
//                        .filter(Optional::isPresent)
//                        .map(Optional::get))
//                .toList();
//    }
//
//    private Optional<String> processPublicAccessTokenSafely(String token) {
//        try {
//            validatePublicAccessToken(token);
//            log.debug("Public Access Token processed successfully: {}", token);
//
//            return Optional.of(token);
//        } catch (Exception e) {
//            log.error("Public Access Token processing failed: {}", token, e);
//            return Optional.empty();
//        }
//    }
//
//    public List<String> processBatchLocalAccessTokens(List<String> tokens) {
//        return tokens.parallelStream()
//                .filter(Objects::nonNull)
//                .limit(TOKEN_PROCESSING_BATCH_SIZE)
//                .map(this::processLocalAccessTokenSafely)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .toList(); // Unmodifiable list
//    }
//
//    private Optional<String> processLocalAccessTokenSafely(String token) {
//        try {
//            validateLocalAcessToken(token);
//            log.debug("Local Access Token processed successfully: {}", token);
//            return Optional.of(token);
//        } catch (Exception e) {
//            log.error("Local Access Token processing failed: {}", token, e);
//            return Optional.empty();
//        }
//    }
//
//    public List<String> processBatchLocalRefreshTokens(List<String> tokens) {
//        return tokens.parallelStream()
//                .filter(Objects::nonNull)
//                .limit(TOKEN_PROCESSING_BATCH_SIZE)
//                .map(this::processLocalRefreshTokenSafely)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .toList(); // Unmodifiable list
//    }
//
//    private Optional<String> processLocalRefreshTokenSafely(String token) {
//        try {
//            validateRefreshToken(token);
//            log.debug("Local Refresh Token processed successfully: {}", token);
//
//            return Optional.of(token);
//        } catch (Exception e) {
//            log.error("Local Refresh Token processing failed: {}", token, e);
//            return Optional.empty();
//        }
//    }

//    // Add graceful shutdown if using Executors.newFixedThreadPool.
//    @PreDestroy
//    public void shutdown() {
//        tokenProcessingExecutor.shutdown();
//        try {
//            if (!tokenProcessingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
//                tokenProcessingExecutor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            tokenProcessingExecutor.shutdownNow();
//        }
//    }

}

/**
 * Considerations:
 * <p>
 * Cache Size:
 * <p>
 * Importance:
 * The cache size of your revokedTokensCache directly impacts performance and memory usage.
 * A large cache can improve performance by reducing database queries, but it can also consume more memory.
 * Factors to Consider:
 * Number of revoked tokens: Estimate the number of revoked tokens that you expect to have in your application.
 * Memory availability: Determine the amount of memory that you can allocate to the cache.
 * Cache eviction policy: Choose a cache eviction policy (e.g., LRU, LFU) that suits your application's needs.
 * Tuning:
 * Monitor cache hit rates and memory usage to determine the optimal cache size.
 * Adjust the cache size based on your application's performance and memory requirements.
 * Benefits:
 * Improved performance.
 * Reduced database load.
 * Considerations:
 * Cache invalidation: Ensure that your cache invalidation strategy is effective.
 * Memory usage: Monitor memory usage to prevent out-of-memory errors.
 */


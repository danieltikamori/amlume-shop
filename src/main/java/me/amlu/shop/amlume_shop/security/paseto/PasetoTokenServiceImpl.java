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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import org.springframework.scheduling.annotation.Async;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final Version PASETO_VERSION = Version.V4;
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
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final KeyFactory KEY_FACTORY = initializeKeyFactory();
    private static final ObjectMapper OBJECT_MAPPER = initializeObjectMapper();
    private static final String EXPECTED_ISSUER = "${SERVICE_NAME}";
    private static final String EXPECTED_AUDIENCE = "${SERVICE_AUDIENCE}";

    // Add performance tuning constants
    private static final int INITIAL_CLAIMS_MAP_CAPACITY = 16;
    private static final int TOKEN_PROCESSING_BATCH_SIZE = 1000;
    private static final Duration TOKEN_PROCESSING_TIMEOUT = Duration.ofSeconds(30);

    // Add security constants
    private static final int MIN_TOKEN_LENGTH = 64;
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final Duration MAX_TOKEN_LIFETIME = Duration.ofDays(7);

    // Add thread pool for async operations
    private final ExecutorService tokenProcessingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                    .setNameFormat("token-processor-%d")
                    .setDaemon(true)
                    .build()
    );

    // Add metrics
    private final PrometheusMeterRegistry meterRegistry;

    private final Counter tokenGenerationCounter = Counter.build()
            .name("paseto_token_generation_total")
            .help("Total number of PASETO tokens generated")
            .register();

    private final Counter tokenValidationCounter;
    private final Timer tokenValidationTimer;
//    private final Timer tokenValidationTimer = Timer.build()
//            .name("paseto_token_validation_seconds")
//            .help("Time spent validating PASETO tokens")
//            .register();


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
     *     // Usage example in the validation method
     *     public boolean validateToken(String token) {
     *         Summary.Timer timer = tokenValidationLatency.startTimer();
     *         try {
     *             // Your token validation logic here
     *             return true;
     *         } finally {
     *             timer.observeDuration();
     *         }
     *     }
     *
     *     public String generateToken(User user) {
     *         try {
     *             // Token generation logic here
     *             String token = // ... generate token ...
     *             tokenGenerationCounter.inc();
     *             return token;
     *         } catch (Exception e) {
     *             log.error("Failed to generate token", e);
     *             throw new TokenGenerationException("Failed to generate token", e);
     *         }
     *     }
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

    // Immutable key holders
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

    // IMPORTANT: Getters for keys with null checks
    /**
     * Usage example:
     * public String signToken(String payload) {
     *     try {
     *         return Paseto.sign(payload, getAccessPrivateKey());
     *     } catch (Exception e) {
     *         throw new TokenSigningException("Failed to sign token", e);
     *     }
     * }
     *
     * public boolean verifyToken(String token) {
     *     try {
     *         return Paseto.verify(token, getAccessPublicKey());
     *     } catch (Exception e) {
     *         throw new TokenVerificationException("Failed to verify token", e);
     *     }
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

    // Key management
    private KeyPairHolder keyPairHolder;
    private SecretKeyHolder secretKeyHolder;

    // Required services
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final KeyManagementFacade keyManagementFacade;

    public PasetoTokenServiceImpl(Timer tokenValidationTimer, Counter tokenValidationCounter, PrometheusMeterRegistry meterRegistry, RefreshTokenRepository refreshTokenRepository, RevokedTokenRepository revokedTokenRepository, KeyManagementFacade keyManagementFacade,
                                  HttpServletRequest httpServletRequest, UserRepository userRepository, EnhancedAuthenticationService enhancedAuthenticationService) {
        this.meterRegistry = meterRegistry;
        this.tokenValidationTimer = Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .register(meterRegistry);
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

    @Value("${security.token.revoked-cache.max-size}")
    private int maxCacheSize;

    @Value("${token.cache.expiry.hours:1}")
    private int cacheExpiryHours;

    // Static initializers

    /**
     * Initializes the key factory for the specified algorithm.
     * This method is called during the initialization of the service.
     * It uses the KeyFactory.getInstance() method to get an instance of the KeyFactory for the specified algorithm.
     *
     * @throws KeyConversionException if the algorithm is not available.
     * @throws NoSuchAlgorithmException if the algorithm is not available.
     * @throws InvalidKeySpecException if the key specification is invalid.
     * @return
     */
    private static KeyFactory initializeKeyFactory() {
        try {
            return KeyFactory.getInstance(KEY_CONVERSION_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error(String.valueOf(ErrorMessages.KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
            throw new KeyConversionException(String.valueOf(ErrorMessages.KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE), e);
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
     *
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

    // Add rate limiting
    private final RateLimiter tokenGenerationRateLimiter = RateLimiter.create(100.0); // 100 tokens per second

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
            log.error(String.valueOf(ErrorMessages.FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(ErrorMessages.FAILED_TO_SERIALIZE_CLAIMS), e);
        } catch (PasetoException e) {
            log.error("Failed to sign PASETO token", e);
            throw new TokenGenerationFailureException("Failed to sign PASETO token", e);
        } catch (Exception e) {
            log.error("Unexpected error during PASETO token generation", e);
            throw new TokenGenerationFailureException("Unexpected error during token generation", e);
        }
    }

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
            log.error(String.valueOf(ErrorMessages.FAILED_TO_SERIALIZE_CLAIMS), e);
            throw new TokenGenerationFailureException(String.valueOf(ErrorMessages.FAILED_TO_SERIALIZE_CLAIMS), e);
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
            String payload = OBJECT_MAPPER.writeValueAsString(claims);

            // Validate payload
            validatePayload(payload);

            // Create footer
            PasetoClaims footerClaims = createPasetoFooterClaims(PASETO_REFRESH_LOCAL_KID);
            String footer = OBJECT_MAPPER.writeValueAsString(footerClaims); // Convert to JSON string

            String refreshToken = Paseto.encrypt(getRefreshSecretKey(), payload, footer);

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
                throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT));
            }

            // Validate that each part is not empty
            for (String part : parts) {
                if (part == null || part.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.valueOf(INVALID_TOKEN_FORMAT));
                }
            }

            String payload = Paseto.parse(getAccessPublicKey(), parts[0] + "." + parts[1], parts[2]);

            // Add null check before parsing
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


    // Token validation
    private void validateToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null");

        if (token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidTokenException("Token length out of acceptable range");
        }

        try {
//            Timer.Sample sample = Timer.start();
            Timer.Sample sample = Timer.start(meterRegistry);
            validateTokenFormat(token);
            validateTokenSignature(token);
            validateTokenClaims(token);
            sample.stop(tokenValidationTimer);
        } catch (Exception e) {
            log.error("Token validation failed", e);
            throw new TokenValidationException("Token validation failed", e);
        }
    }

    private void validateTokenFormat(String token) {
        try {
            if (!token.startsWith("v4.public.") && !token.startsWith("v4.local.")) {
                throw new InvalidTokenException("Invalid token format: must start with v4.public. or v4.local.");
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidTokenException("Invalid token format: must contain exactly three parts");
            }

            // Check if the token contains valid base64 encoded parts
            try {
                Base64.getDecoder().decode(parts[2]);
            } catch (IllegalArgumentException e) {
                throw new InvalidTokenException("Invalid token format: payload is not properly base64 encoded");
            }
        } catch (Exception e) {
            log.error("Token format validation failed", e);
            throw new TokenValidationException("Token format validation failed", e);
        }
    }

    private void validateTokenSignature(String token) {
        try {
            if (token.startsWith("v4.public.")) {
                // For public tokens, verify using public key
                try {
                    PasetoParser.getV4PublicParser()
                            .setPublicKey(accessPublicKey)
                            .parse(token);
                } catch (PasetoParseException e) {
                    throw new InvalidTokenException("Invalid token signature for public token");
                }
            } else if (token.startsWith("v4.local.")) {
                // For local tokens, verify using secret key
                try {
                    PasetoParser.getV4LocalParser()
                            .setSharedKey(accessSecretKey)
                            .parse(token);
                } catch (PasetoParseException e) {
                    throw new InvalidTokenException("Invalid token signature for local token");
                }
            }
        } catch (Exception e) {
            log.error("Token signature validation failed", e);
            throw new TokenValidationException("Token signature validation failed", e);
        }
    }

    private void validateTokenClaims(String token) {
        try {
            // Parse the token based on its type
            PasetoParser parser;
            if (token.startsWith("v4.public.")) {
                parser = PasetoParser.getV4PublicParser()
                        .setPublicKey(accessPublicKey);
            } else {
                parser = PasetoParser.getV4LocalParser()
                        .setSharedKey(accessSecretKey);
            }

            // Parse and get claims
            PasetoToken pasetoToken = parser.parse(token);
            Map<String, Object> claims = pasetoToken.getClaims();

            // Validate required claims
            validateRequiredClaims(claims);

            // Validate expiration
            validateExpiration(claims);

            // Validate not before
            validateNotBefore(claims);

            // Validate issuer
            validateIssuer(claims);

            // Check if token is revoked
            validateNotRevoked(token);

        } catch (PasetoParseException e) {
            log.error("Token claims validation failed", e);
            throw new TokenValidationException("Token claims validation failed", e);
        }
    }

    private void validateRequiredClaims(Map<String, Object> claims) {
        List<String> requiredClaims = Arrays.asList("exp", "iat", "nbf", "sub", "iss");
        List<String> missingClaims = requiredClaims.stream()
                .filter(claim -> !claims.containsKey(claim))
                .collect(Collectors.toList());

        if (!missingClaims.isEmpty()) {
            throw new InvalidTokenException("Missing required claims: " + String.join(", ", missingClaims));
        }
    }

    private void validateExpiration(Map<String, Object> claims) {
        Instant expiration = Instant.parse(claims.get("exp").toString());
        if (Instant.now().isAfter(expiration)) {
            throw new TokenExpiredException("Token has expired");
        }
    }

    private void validateNotBefore(Map<String, Object> claims) {
        Instant notBefore = Instant.parse(claims.get("nbf").toString());
        if (Instant.now().isBefore(notBefore)) {
            throw new TokenNotValidYetException("Token is not valid yet");
        }
    }

    private void validateIssuer(Map<String, Object> claims) {
        String issuer = claims.get("iss").toString();
        if (!EXPECTED_ISSUER.equals(issuer)) {
            throw new InvalidTokenException("Invalid token issuer");
        }
    }

    private void validateNotRevoked(String token) {
        // First check the cache
        Boolean isRevoked = revokedTokensCache.getIfPresent(token);
        if (Boolean.TRUE.equals(isRevoked)) {
            throw new TokenRevokedException("Token has been revoked");
        }

        // If not in cache, check the database
        if (revokedTokenRepository.existsByToken(token)) {
            // Add to cache for future checks
            revokedTokensCache.put(token, true);
            throw new TokenRevokedException("Token has been revoked");
        }
    }


    // Add async token validation
    /**
     * Validate token asynchronously.
     * This method is intended for use in scenarios where token validation can be performed in the background.
     * It returns a CompletableFuture that will be completed with a boolean indicating whether the token is valid or not.
     * This allows the caller to continue processing without waiting for the validation to complete.
     * <p>
     * Example usage:
     * CompletableFuture<Boolean> validationFuture = tokenService.validateTokenAsync(token);
     * validationFuture.thenAccept(valid -> {
     *     if (isValid) {
     *         // Token is valid, proceed with processing
     *     } else {
     *         // Token is invalid, handle error
     *     }
     * });
     * <p>
     * Note: This method uses a separate executor for token processing to avoid blocking the main thread.
     * The executor is configured with a fixed thread pool size to limit the number of concurrent token validations.
     * <p>
     * @see CompletableFuture
     * @see Executor
     * @see Executors#newFixedThreadPool(int)
     * @see Async
     * @param token
     * @return
     */
    @Async
    public CompletableFuture<Boolean> validateTokenAsync(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateToken(token);
                return true;
            } catch (Exception e) {
                log.warn("Async token validation failed", e);
                return false;
            }
        }, tokenProcessingExecutor);
    }

    // Add batch token processing

    /**
     * Process a batch of tokens asynchronously.
     * This method is intended for use in scenarios where token validation can be performed in the background for a batch of tokens.
     * It returns a list of valid tokens.
     * <p>
     * Example usage:
     * List<String> tokens = Arrays.asList("token1", "token2", "token3");
     * List<String> validTokens = tokenValidator.processBatchTokens(tokens);
     * <p>
     * @see List
     * @see Arrays#asList(Object[])
     * @see Stream#parallel()
     * @see Stream#limit(long)
     * @see CompletableFuture#allOf(CompletableFuture[])
     * @see Optional#isPresent()
     * @param tokens
     * @return
     */
    public List<String> processBatchTokens(List<String> tokens) {
        return tokens.parallelStream()
                .filter(Objects::nonNull)
                .limit(TOKEN_PROCESSING_BATCH_SIZE)
                .map(this::processTokenSafely)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList(); // Unmodifiable list
//                .collect(Collectors.toList());
    }

    private Optional<String> processTokenSafely(String token) {
        try {
            validateToken(token);
            return Optional.of(token);
        } catch (Exception e) {
            log.debug("Token processing failed", e);
            return Optional.empty();
        }
    }

    // Add graceful shutdown
    @PreDestroy
    public void shutdown() {
        tokenProcessingExecutor.shutdown();
        try {
            if (!tokenProcessingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                tokenProcessingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tokenProcessingExecutor.shutdownNow();
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
            throw new TokenValidationFailureException(String.valueOf(TOKEN_VALIDATION_FAILED));
        }
    }

    private String[] splitToken(String token) throws TokenValidationFailureException {
        String[] parts = token.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_TOKEN_FORMAT));
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
        return Paseto.parse(getAccessPublicKey(), signedMessage, footer);
    }

    private Map<String, Object> parseClaims(String payload) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
        });
    }

    private void validateKid(Map<String, Object> claims) throws TokenValidationFailureException {
        String kid = (String) claims.get("kid");
        if (kid == null || kid.isEmpty()) {
            throw new TokenValidationFailureException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
        }
        if (!PASETO_ACCESS_KID.equals(kid)) {
            throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
        }
    }

    private void handleValidationException(Exception e) {
        if (e instanceof SignatureException) {
            log.error("Invalid PASETO signature", e);
            throw new InvalidTokenSignatureException("Invalid PASETO signature");
        } else if (e instanceof JsonProcessingException) {
            log.error(String.valueOf(ErrorMessages.ERROR_PARSING_CLAIMS), e);
            throw new TokenValidationFailureException(String.valueOf(ErrorMessages.ERROR_PARSING_CLAIMS));
        } else if (e instanceof IllegalArgumentException) {
            log.error(String.valueOf(INVALID_PASETO_TOKEN), e);
            throw new TokenValidationFailureException(String.valueOf(INVALID_PASETO_TOKEN));
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
                throw new TokenValidationFailureException(String.valueOf(INVALID_TOKEN_FORMAT));
            }

            String payload = Paseto.decrypt(getAccessSecretKey(), parts[0] + "." + parts[1], parts[2]);

            Map<String, Object> claims = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });

            // Get kid from footer claims
            String kid = (String) claims.get("kid");
            if (kid == null || kid.isEmpty()) {
                throw new TokenValidationFailureException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
            }

            // Verify key ID matches

            if (!PASETO_ACCESS_LOCAL_KID.equals(kid)) {
                throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
            }

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
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new TokenValidationFailureException(String.valueOf(INVALID_TOKEN_FORMAT));
            }

            String payload = Paseto.decrypt(getRefreshSecretKey(), parts[0] + "." + parts[1], parts[2]);

            Map<String, Object> claims = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });

            // Get kid from footer claims
            String kid = (String) claims.get("kid");
            if (kid == null || kid.isEmpty()) {
                throw new TokenValidationFailureException(String.valueOf(KID_IS_MISSING_IN_THE_TOKEN_FOOTER));
            }

            // Verify key ID matches
            if (!PASETO_REFRESH_LOCAL_KID.equals(kid)) {
                throw new TokenValidationFailureException(String.valueOf(INVALID_KEY_ID));
            }

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
    public void validateAccessTokenClaims(Map<String, Object> claims, String expectedType) throws
            TokenValidationFailureException {

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
    public void validateRefreshTokenClaims(Map<String, Object> claims, String expectedType) throws
            TokenValidationFailureException {
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
            userCache.cleanUp();
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

    public AuthResponse refreshAccessToken(String refreshToken) throws
            InvalidTokenException, TokenValidationFailureException {
        try {
            RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new InvalidTokenException(String.valueOf(ErrorMessages.INVALID_REFRESH_TOKEN)));

            User user = refreshTokenEntity.getUser(); // Get the user from the RefreshToken entity


            if (user == null) {
                throw new InvalidTokenException(String.valueOf(ErrorMessages.INVALID_REFRESH_TOKEN));
            }

            Map<String, Object> refreshTokenClaims = validateRefreshToken(refreshToken);

            // Extract user information from refresh token
            String userId = refreshTokenClaims.get("sub").toString(); // Extract userId
            User existingUser = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException("User not found during refresh token validation"));

            if (existingUser == null) {
                throw new InvalidTokenException(String.valueOf(ErrorMessages.INVALID_REFRESH_TOKEN));
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
            throw new InvalidTokenException(String.valueOf(ErrorMessages.INVALID_REFRESH_TOKEN), e);
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


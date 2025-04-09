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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * This class is responsible for creating and managing Paseto claims for access and refresh tokens.
 * It also handles token validation, processing,and rate limiting.
 */

@Slf4j
@Service
public class TokenClaimsServiceImpl implements TokenClaimsService {

    @Value("${rateLimit.token.validation}")
    private double tokenValidationRateLimiterValue;

    @Value("${service.name}")
    private String DEFAULT_ISSUER;

    @Value("${service.audience}")
    private String DEFAULT_AUDIENCE;

    // Metrics
    private final PrometheusMeterRegistry meterRegistry;

    private final Summary tokenValidationLatency = Summary.build()
            .name("paseto_token_validation_seconds")
            .help("Time spent validating PASETO tokens")
            .quantile(0.5, 0.05)    // Add 50th percentile with 5% tolerance
            .quantile(0.95, 0.01)   // Add 95th percentile with 1% tolerance
            .maxAgeSeconds(300) // 5 minutes maximum age for samples
            .ageBuckets(5)
            .register();

    private final Counter tokenValidationCounter;
    private final Timer tokenValidationTimer;

    //ObjectMapper
    private final ObjectMapper objectMapper;

    // Dependencies
    private final UserRepository userRepository;
    private final EnhancedAuthenticationService enhancedAuthenticationService;
    private final HttpServletRequest httpServletRequest;
    private final TokenUtilService tokenUtilService;
    private final TokenValidationService tokenValidationService;
    private final TokenRevocationService tokenRevocationService;


    public TokenClaimsServiceImpl(UserRepository userRepository, PrometheusMeterRegistry meterRegistry, Counter tokenValidationCounter, Timer tokenValidationTimer, ObjectMapper objectMapper, EnhancedAuthenticationService enhancedAuthenticationService, HttpServletRequest httpServletRequest, TokenUtilService tokenUtilService, TokenValidationService tokenValidationService, TokenRevocationService tokenRevocationService) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.tokenValidationCounter = Counter.build()
                .name("paseto_token_validation_total")
                .help("Total number of PASETO token validations")
                .register();
        this.tokenValidationTimer = Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99) // Add percentiles
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);
        this.userRepository = userRepository;

        this.enhancedAuthenticationService = enhancedAuthenticationService;
        this.httpServletRequest = httpServletRequest;
        this.tokenUtilService = tokenUtilService;
        this.tokenValidationService = tokenValidationService;
        this.tokenRevocationService = tokenRevocationService;
    }

    /**
     * Creates access token claims for a user.
     *
     * @param userId   the user's ID.
     * @param validity the duration the token is valid for.
     * @return the PasetoClaims for the access token.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws IllegalArgumentException  if the user ID is invalid.
     */
    @Override
    public PasetoClaims createPublicAccessPasetoClaims(String userId, Duration validity) {
//        User user = findUserById(userId);
//        User user = tokenUtilService.getUserId(userId);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String userScope = enhancedAuthenticationService.determineUserScope();
        return buildAccessPasetoClaims(userId, now, validity, userScope);
    }

    @Override
    public PasetoClaims createLocalAccessPasetoClaims(String serviceId, Duration validity) {
        // To implement if needed for Kubernetes or distributed services
//        Service service = tokenUtilService.getServiceId(serviceId);
//
//        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
//        String userScope = enhancedAuthenticationService.determineUserScope();
        return null;
    }

    /**
     * Creates refresh token claims for a user.
     *
     * @param userId   the user's ID.
     * @param validity the duration the token is valid for.
     * @return the PasetoClaims for the refresh token.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws IllegalArgumentException  if the user ID is invalid.
     */
    @Override
    public PasetoClaims createLocalRefreshPasetoClaims(String userId, Duration validity) {
//        User user = tokenUtilService.getUserId(userId);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return new PasetoClaims()
                .setIssuer(DEFAULT_ISSUER)
                .setSubject(userId)
                .setAudience(DEFAULT_AUDIENCE)
                .setExpiration(now.plus(validity))
                .setNotBefore(now)
                .setIssuedAt(now)
                .setTokenId(tokenUtilService.generateTokenId())
                .setTokenType(String.valueOf(TokenType.REFRESH_TOKEN));
    }

    /**
     * Creates the footer for a PASETO token.
     *
     * @param keyId the key ID.
     *              //     * @param wrappedPaserk the wrapped Paserk.
     * @return the PasetoClaims for the footer.
     */
    @Override
    public PasetoClaims createPasetoFooterClaims(String keyId) {
        PasetoClaims footerClaims = new PasetoClaims();
        footerClaims.setKeyId(keyId);
//        footerClaims.setWrappedPaserk(wrappedPaserk);
        return footerClaims;
    }

    /**
     * Builds access token claims for a user.
     *
     * @param userId    the userId for whom to build the claims.
     * @param now       the current date and time.
     * @param validity  the duration the token is valid for.
     * @param userScope the scope of the user.
     * @return the PasetoClaims for the access token.
     */
    private PasetoClaims buildAccessPasetoClaims(String userId, ZonedDateTime now, Duration validity, String userScope) {
        return new PasetoClaims()
                .setIssuer(DEFAULT_ISSUER)
                .setSubject(userId)
                .setAudience(DEFAULT_AUDIENCE)
                .setExpiration(now.plus(validity))
                .setNotBefore(now)
                .setIssuedAt(now)
                .setTokenId(tokenUtilService.generateTokenId())
                .setSessionId(Objects.requireNonNull(httpServletRequest.getSession()).getId())
                .setScope(userScope)
                .setTokenType(String.valueOf(TokenType.ACCESS_TOKEN));
    }

}

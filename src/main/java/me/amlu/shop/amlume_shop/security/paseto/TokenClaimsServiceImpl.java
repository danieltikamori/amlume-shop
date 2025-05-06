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

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.security.enums.TokenType;
import me.amlu.shop.amlume_shop.security.paseto.util.PasetoPropertyResolver;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService;
import me.amlu.shop.amlume_shop.auth.service.AuthenticationInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * This class is responsible for creating and managing Paseto claims for access and refresh tokens.
 * It also handles token validation, processing, and rate limiting.
 */

@Service
public class TokenClaimsServiceImpl implements TokenClaimsService {
    private static final Logger log = LoggerFactory.getLogger(TokenClaimsServiceImpl.class);

    @Value("${service.name}")
    private String DEFAULT_ISSUER;

    @Value("${service.audience}")
    private String DEFAULT_AUDIENCE;

    private final AuthenticationInterface authenticationInterface;
    private final HttpServletRequest httpServletRequest;
    private final TokenUtilService tokenUtilService;
    private final PasetoPropertyResolver pasetoPropertyResolver; // Inject the resolver


    public TokenClaimsServiceImpl(AuthenticationInterface authenticationInterface, HttpServletRequest httpServletRequest, TokenUtilService tokenUtilService, PasetoPropertyResolver pasetoPropertyResolver) {
        this.authenticationInterface = authenticationInterface;
        this.httpServletRequest = httpServletRequest;
        this.tokenUtilService = tokenUtilService;
        this.pasetoPropertyResolver = pasetoPropertyResolver;
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
        String userScope = authenticationInterface.determineUserScope();
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
     * @param purpose the purpose of the token (e.g., "public_access", "local_access", "local_refresh").
     *              //     * @param wrappedPaserk the wrapped Paserk.
     * @return the PasetoClaims for the footer.
     */
    @Override
    public PasetoClaims createPasetoFooterClaims(String purpose) {
        String keyId = switch (purpose) {
            case "public_access" -> pasetoPropertyResolver.resolvePublicAccessKid();
            case "local_access" -> pasetoPropertyResolver.resolveLocalAccessKid();
            case "local_refresh" -> pasetoPropertyResolver.resolveLocalRefreshKid();
            // Add cases for public_refresh if needed
            default -> {
                log.error("Unsupported purpose '{}' for creating PASETO footer claims.", purpose);
                throw new IllegalArgumentException("Unsupported purpose for footer claims: " + purpose);
            }
        };
        // Resolve KID based on purpose using the resolver

        PasetoClaims footerClaims = new PasetoClaims();
        footerClaims.setKeyId(keyId);
        // footerClaims.setWrappedPaserk(wrappedPaserk); // If needed
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

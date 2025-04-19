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
import me.amlu.shop.amlume_shop.security.paseto.util.TokenUtilService;
import me.amlu.shop.amlume_shop.security.service.AuthenticationService;
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

    @Value("${service.name}")
    private String DEFAULT_ISSUER;

    @Value("${service.audience}")
    private String DEFAULT_AUDIENCE;

    private final AuthenticationService authenticationService;
    private final HttpServletRequest httpServletRequest;
    private final TokenUtilService tokenUtilService;


    public TokenClaimsServiceImpl(AuthenticationService authenticationService, HttpServletRequest httpServletRequest, TokenUtilService tokenUtilService) {
        this.authenticationService = authenticationService;
        this.httpServletRequest = httpServletRequest;
        this.tokenUtilService = tokenUtilService;
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
        String userScope = authenticationService.determineUserScope();
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

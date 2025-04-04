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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.InvalidTokenSignatureException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.BEARER_TOKEN_PREFIX;

@Slf4j
@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

    private final PasetoTokenService pasetoTokenService;
//    private static final String BEARER_PREFIX = "Bearer ";

    public PasetoAuthenticationFilter(PasetoTokenService pasetoTokenService) {
        this.pasetoTokenService = pasetoTokenService;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Map<String, Object> tokenPayload = pasetoTokenService.validatePublicAccessToken(token);

                if (tokenPayload != null) {
                    TokenPayload payload = (TokenPayload) tokenPayload.get(token);

                    // Create authentication object with user details and authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    payload.getSubject(),
                                    null,
                                    extractAuthorities(payload.getClaims())
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource()
                            .buildDetails(request));

                    // Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Invalid token received");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    SecurityContextHolder.clearContext();
                    return;
                }
            }
        } catch (TokenValidationFailureException e) {
            log.error("Cannot set user authentication", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            SecurityContextHolder.clearContext();
        } catch (SignatureException e) {
            throw new InvalidTokenSignatureException("Invalid token signature", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * DO NOT USE Apache Commons Lang StringUtils because it doesn't check for hasText
     * Extracts the token from the request headers.
     * @param request the HTTP request
     * @return the token or null if no token is found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_TOKEN_PREFIX)) {
            return bearerToken.substring(BEARER_TOKEN_PREFIX.length());
        }
        return null;
    }

    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Object roles = claims.get("roles");
        if (roles instanceof Collection) {
            return ((Collection<?>) roles)
                    .stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

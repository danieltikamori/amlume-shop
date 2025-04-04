/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.amlu.shop.amlume_shop.exceptions.MfaAuthenticationException;
import me.amlu.shop.amlume_shop.exceptions.MfaValidationException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.payload.ErrorResponse;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final ObjectMapper objectMapper;

    public MfaAuthenticationFilter(MfaService mfaService,
                                   MfaTokenRepository mfaTokenRepository,
                                   ObjectMapper objectMapper) {
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        if (!requiresMfa(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            User user = validateAndGetUser();
            validateMfaIfEnabled(user, request);
            filterChain.doFilter(request, response);
        } catch (MfaAuthenticationException e) {
            sendErrorResponse(response, e.getStatus(), e.getCode(), e.getMessage());
        }
    }

    private boolean requiresMfa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/auth/") ||
                uri.startsWith("/public/") ||
                uri.equals("/health") ||
                uri.equals("/favicon.ico"));
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   String code,
                                   String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse(code, message);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated();
    }

    private User validateAndGetUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) {
            throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication is required");
        }

        if (!(auth.getPrincipal() instanceof User)) {
            throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "INVALID_USER", "Invalid user type");
        }

        User user = (User) auth.getPrincipal();
        if (user == null) {
            throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found");
        }

        return user;
    }

    private void validateMfaIfEnabled(User user, HttpServletRequest request) {
        if (!mfaService.isMfaEnabled(user)) {
            return;
        }

        String mfaCode = request.getHeader("X-MFA-Code");
        if (mfaCode == null || mfaCode.trim().isEmpty()) {
            throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "MFA_REQUIRED", "MFA code is required");
        }

        try {
            if (!validateMfaCode(user, mfaCode)) {
                throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "INVALID_MFA", "Invalid MFA code");
            }
        } catch (MfaValidationException e) {
            throw new MfaAuthenticationException(HttpStatus.UNAUTHORIZED, "MFA_ERROR", e.getMessage());
        } catch (RuntimeException e) {
            throw new MfaAuthenticationException(HttpStatus.INTERNAL_SERVER_ERROR, "MFA_ERROR", "Error validating MFA code");
        }
    }

    private boolean validateMfaCode(User user, String code) {
        return mfaTokenRepository.findByUser(user)
                .map(token -> {
                    try {
                        return mfaService.verifyCode(token.getSecret(), code);
                    } catch (TooManyAttemptsException e) {
                        throw new MfaValidationException("Too many MFA attempts", e);
                    } catch (ExecutionException e) {
                        throw new MfaValidationException("Error validating MFA code", e);
                    }
                })
                .orElseThrow(() -> new MfaValidationException("No MFA token found for user"));
    }

}


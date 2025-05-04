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
import me.amlu.shop.amlume_shop.security.handler.AuthenticationExceptionHandler;
import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.security.repository.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import me.amlu.shop.amlume_shop.user_management.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// Commented Component as it was filtering beyond the required scope
// Re-enable if the requirements change to a more restricted scope
//@Component
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final ObjectMapper objectMapper;
    private final List<String> excludedUris;

    public MfaAuthenticationFilter(MfaService mfaService,
                                   MfaTokenRepository mfaTokenRepository,
                                   ObjectMapper objectMapper,
                                   @Value("#{'${mfa.excluded-uris}'.split(',')}") List<String> excludedUris) {
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.objectMapper = objectMapper;
        this.excludedUris = excludedUris;
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
        } catch (MfaException e) {
            sendErrorResponse(response, request, HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, request, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    private boolean requiresMfa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return excludedUris.stream().noneMatch(uri::startsWith);
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   HttpServletRequest request,
                                   HttpStatus status,
                                   String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        AuthenticationExceptionHandler.ErrorResponse errorResponse = new AuthenticationExceptionHandler.ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private User validateAndGetUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new MfaException(MfaException.MfaErrorType.INVALID_TOKEN, "Authentication is required");
        }

        if (!(auth.getPrincipal() instanceof User user)) {
            throw new MfaException(MfaException.MfaErrorType.INVALID_TOKEN, "Invalid user type");
        }

        return user;
    }

    private void validateMfaIfEnabled(User user, HttpServletRequest request) {
        if (!mfaService.isMfaEnabled(user)) {
            return;
        }

        String mfaCode = request.getHeader("X-MFA-Code");
        if (mfaCode == null || mfaCode.trim().isEmpty()) {
            throw new MfaException(MfaException.MfaErrorType.INVALID_CODE, "MFA code is required");
        }

        validateMfaCode(user, mfaCode);
    }

    private void validateMfaCode(User user, String code) {
        mfaTokenRepository.findByUser(user)
                .ifPresentOrElse(token -> {
                    try {
                        if (!mfaService.verifyCode(token.getSecret(), code)) {
                            throw new MfaException(MfaException.MfaErrorType.INVALID_CODE, "Invalid MFA code");
                        }
                    } catch (TooManyAttemptsException e) {
                        throw new MfaException(MfaException.MfaErrorType.RECOVERY_CODE_USAGE_ERROR, "Too many MFA attempts", e);
                    } catch (Exception e) {
                        throw new MfaException(MfaException.MfaErrorType.RECOVERY_CODE_VERIFICATION_ERROR, "Error validating MFA code", e);
                    }
                }, () -> {
                    throw new MfaException(MfaException.MfaErrorType.TOKEN_NOT_FOUND, "No MFA token found for user");
                });
    }
}


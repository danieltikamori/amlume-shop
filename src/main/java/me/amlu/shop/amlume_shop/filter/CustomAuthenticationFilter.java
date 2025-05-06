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
import me.amlu.shop.amlume_shop.auth.dto.AuthResponse;
import me.amlu.shop.amlume_shop.auth.dto.LoginRequest;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.dto.ErrorResponse;
import me.amlu.shop.amlume_shop.exceptions.TokenGenerationFailureException;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;


// Disabled as authentication is being handled by Keycloak
//@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final PasetoTokenService pasetoTokenService;


    public CustomAuthenticationFilter(AuthenticationManager authenticationManager,
                                      ObjectMapper objectMapper,
                                      PasetoTokenServiceImpl pasetoTokenService) {
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
        this.pasetoTokenService = pasetoTokenService;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals(Constants.API_AUTH_V_1_LOGIN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Read login request from request body
            LoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(),
                    LoginRequest.class
            );

            // Create authentication token
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()

            );

            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(authRequest);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate token and create response
            AuthResponse authResponse = generateTokenResponse(authentication, response);
//            AuthResponse authResponse;
//            try {
//                String token = pasetoTokenService.generatePublicAccessToken(authentication.getName(), Duration.ofHours(1));
//                authResponse = AuthResponse.builder()
//
//                        .token(token)
//                        .username(authentication.getName())
//                        .authorities(authentication.getAuthorities())
//                        .success(true)
//                        .message("Login Successful").build();
//            } catch (TokenGenerationFailureException e) {
//                sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_GENERATION_ERROR", e.getMessage());
//                return; // Stop filter chain
//            }

            // Send response
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.OK.value());
            objectMapper.writeValue(response.getOutputStream(), authResponse);

        } catch (AuthenticationException e) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "AUTH_FAILED", e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR", "An error occurred during authentication");
        }
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

    private AuthResponse generateTokenResponse(Authentication authentication, HttpServletResponse response) throws IOException {
        try {
            String token = pasetoTokenService.generatePublicAccessToken(authentication.getName(), Duration.ofHours(1));
            return AuthResponse.builder()
                    .token(token)
                    .username(authentication.getName())
                    .authorities(authentication.getAuthorities())
                    .success(true)
                    .message("Login Successful").build();
        } catch (TokenGenerationFailureException e) {
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_GENERATION_ERROR", e.getMessage());
            throw e; // Rethrow the exception to stop the filter chain
        }
    }
}

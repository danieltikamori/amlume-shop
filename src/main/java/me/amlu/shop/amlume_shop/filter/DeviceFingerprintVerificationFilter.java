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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.DeviceFingerprintMismatchException;
import me.amlu.shop.amlume_shop.exceptions.ReplayAttackException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl;
import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
import me.amlu.shop.amlume_shop.security.service.EnhancedAuthenticationService;
import me.amlu.shop.amlume_shop.security.service.TokenJtiService;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeviceFingerprintVerificationFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final PasetoTokenServiceImpl pasetoTokenService;
    private EnhancedAuthenticationService enhancedAuthenticationService;
    private final TokenJtiService tokenJtiService;
    private final DeviceFingerprintService deviceFingerprintService;

    public DeviceFingerprintVerificationFilter(UserRepository userRepository, PasetoTokenServiceImpl pasetoTokenService, EnhancedAuthenticationService enhancedAuthenticationService, TokenJtiService tokenJtiService, DeviceFingerprintService deviceFingerprintService) {
        this.userRepository = userRepository;
        this.pasetoTokenService = pasetoTokenService;
        this.enhancedAuthenticationService = enhancedAuthenticationService;
        this.tokenJtiService = tokenJtiService;
        this.deviceFingerprintService = deviceFingerprintService;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromRequest(request);

        // TOCHECK: after implementing authorization, if these checks aren't needed, remove it
        if (token != null && token.startsWith("Bearer ")) { // Check for Bearer token. Perharps this is not needed
            token = token.substring(7); // Remove "Bearer " prefix

            try {
                Map<String, Object> claims = pasetoTokenService.validatePublicAccessToken(token); // Validate the token

                String jti = (String) claims.get("jti");

                // Check if jti is already used (Bloom filter)
                if (tokenJtiService.isJtiValid(jti)) {
                    log.warn("Token has already been used: {}", token);
                    throw new ReplayAttackException("Token has already been used");
                }

                // If jti is not used, add it to the Bloom filter
                // Set expiration for the jti (Important!)
                // Use a time-to-live (TTL) that is slightly longer than your access token's validity
                tokenJtiService.storeJti(jti, enhancedAuthenticationService.getJtiDuration());

                // *** Device Fingerprint Verification ***
                deviceFingerprintService.verifyDeviceFingerprint((String) claims.get("deviceFingerprint"), request, (String) claims.get("sub"));

                // If fingerprint is valid, proceed with authentication
                Authentication authentication = createAuthentication(claims); // Create a Spring Security Authentication object
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (TokenValidationFailureException | DeviceFingerprintMismatchException e) {
                // Handle token validation or fingerprint mismatch exceptions
                log.error("Authentication error: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                sendJsonErrorResponse(response, e.getMessage()); // Helper method for JSON response

//                response.getWriter().write(e.getMessage());
//                return; // Stop the filter chain
            } catch (UserNotFoundException e) { // Catch UserNotFoundException separately
                log.error("User not found: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
                sendJsonErrorResponse(response, e.getMessage());

            } catch (Exception e) { // Catch other exceptions
                log.error("Internal Server Error: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                sendJsonErrorResponse(response, "An internal server error occurred.");
            }

            filterChain.doFilter(request, response); // Continue the filter chain
        }
    }

    private void sendJsonErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        // Extract the token from the request (e.g., from Authorization header, cookie, etc.)
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
//        return request.getHeader("Authorization");
    }

    private Authentication createAuthentication(Map<String, Object> claims) {
        // Create a Spring Security Authentication object based on the claims
        String userId = (String) claims.get("sub");
        User user = userRepository.findByUserId(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Get authorities from the user
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities); // Don't need the password here because the token has already been validated.  The null value for the password is correct in this context
    }

}
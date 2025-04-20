/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.exceptions.GlobalExceptionHandler;
import me.amlu.shop.amlume_shop.exceptions.MfaException;
import me.amlu.shop.amlume_shop.exceptions.RoleNotFoundException;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.payload.RegisterResponse;
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.payload.user.UserResponse;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.security.paseto.TokenValidationService;
import me.amlu.shop.amlume_shop.security.service.MfaService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    // --- Constructors ---
    private final UserService userService;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final TokenValidationService tokenValidationService;
    private final TokenRevocationService tokenRevocationService;

    private final GlobalExceptionHandler globalExceptionHandler;

    public AuthController(UserService userService, MfaService mfaService, MfaTokenRepository mfaTokenRepository, TokenValidationService tokenValidationService, TokenRevocationService tokenRevocationService, GlobalExceptionHandler globalExceptionHandler) {
        this.userService = userService;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.tokenValidationService = tokenValidationService;
        this.tokenRevocationService = tokenRevocationService;
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @PostMapping("/v1/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request, BindingResult bindingResult) throws RoleNotFoundException {
        RegisterResponse response = new RegisterResponse();

        if (bindingResult.hasErrors()) {
            // Handle validation errors
            response.setErrors(bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(response);
        }
        User user = userService.registerUser(request);
        response.setUserResponse(new UserResponse(user));
        return ResponseEntity.ok(response);
    }

    // @Deprecated
    // CustomAuthenticationFilter handles this endpoint

//    @PostMapping("/v1/login")
//    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
//        String ipAddress = request.getRemoteAddr();
//
//        try {
//            AuthResponse authResponse = authService.authenticateUser(loginRequest, ipAddress);
//            LoginResponse response = LoginResponse.builder()
//                    .authResponse(authResponse)
//                    .build();
//
//            if (authResponse.isMfaRequired()) { // Check if MFA is required, using AuthResponse.isMfaRequired() method
//                return ResponseEntity.ok()
//                        .header("Mfa-Status", "required")
//                        .body(response); // Return details.toString() for setting up mfa.
//            }
//            return ResponseEntity.ok(response);
//
//        } catch (AuthenticationException | ExecutionException e) {

    /// /            return globalExceptionHandler.sendErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
//            ErrorResponse errorResponse = globalExceptionHandler.sendErrorResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage()).getBody();
//            LoginResponse response = LoginResponse.builder()
//                    .errorResponse(errorResponse)
//                    .build();
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//        }
//    }
    @PostMapping("/v1/mfa/enable")
    public ResponseEntity<Map<String, Object>> enableMfa(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findUserByUsername(userDetails.getUsername());
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            String secret = mfaService.generateSecretKey();
            String qrCodeUrl = mfaService.generateQrCodeImageUrl(user, secret);

            // Save the MFA token (important!)
            MfaToken mfaToken = mfaTokenRepository.findByUser(user).orElse(MfaToken.builder().user(user).secret(secret).enabled(true).build()); // Use User, set enabled to false initially

            mfaToken.setSecret(secret);
            mfaTokenRepository.save(mfaToken);

            return ResponseEntity.ok(Map.of( // Return necessary information
                    "secret", secret,
                    "qrCodeUrl", qrCodeUrl
            ));

        } catch (MfaException e) {
            log.error("Error enabling MFA", e);
            return globalExceptionHandler.sendErrorResponseAsMap(HttpStatus.INTERNAL_SERVER_ERROR, "MFA_ENABLE_ERROR", "An error occurred while enabling MFA.");
//            return globalExceptionHandler.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "MFA_ENABLE_ERROR", "An error occurred while enabling MFA.");

        }
    }

//    /**
//     * @deprecated Using @MfaAuthenticationProvider instead.
//    // MfaAuthenticationFilter handles this endpoint
//     * @param body request body containing the MFA code
//     * @return ResponseEntity<LoginResponse>
//     */
//    @PostMapping("/v1/mfa/validate")
//    public ResponseEntity<LoginResponse> validateMfa(@RequestBody Map<String, String> body) {
//
//        try {
//            String mfaCode = body.get("code");
//
//            if (mfaCode == null || mfaCode.isBlank()) {
//                throw new MfaException(MfaException.MfaErrorType.INVALID_CODE, "Mfa code is empty or null");
//            }
//
//            AuthResponse response = authService.validateMfa(mfaCode);
//            LoginResponse validationResponse =LoginResponse.builder()
//                    .authResponse(response)
//                    .build();
//
//            return ResponseEntity.ok(validationResponse);  // Assuming validateMfa returns a Paseto token if successful
//        } catch (MfaValidationException e) {
//            LoginResponse errorResponse = LoginResponse.builder()
//                    .errorResponse(new ErrorResponse("INVALID_MFA_CODE", e.getMessage()))
//                    .build();
//
//            return ResponseEntity.badRequest().body(errorResponse);
////            return globalExceptionHandler.sendErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_MFA_CODE", e.getMessage());
//        } catch (TooManyAttemptsException e) {
//            ErrorResponse errorResponse = new ErrorResponse("TOO_MANY_ATTEMPTS", e.getMessage());
//            LoginResponse tooManyAttemptsResponse = LoginResponse.builder()
//                    .errorResponse(errorResponse)
//                    .build();
//            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(tooManyAttemptsResponse);
////            return globalExceptionHandler.sendErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", e.getMessage());
//        } catch (LockedException e) {
//            ErrorResponse errorResponse = new ErrorResponse("ACCOUNT_LOCKED", e.getMessage());
//            LoginResponse lockedResponse = LoginResponse.builder()
//                    .errorResponse(errorResponse)
//                    .build();
//            return ResponseEntity.status(HttpStatus.LOCKED).body(lockedResponse);
////            return globalExceptionHandler.sendErrorResponse(HttpStatus.LOCKED, "ACCOUNT_LOCKED", e.getMessage());
//        } catch (MfaNotSetupException e) {
//            ErrorResponse errorResponse = new ErrorResponse("MFA_NOT_SETUP", e.getMessage());
//            LoginResponse notSetupResponse = LoginResponse.builder()
//                    .errorResponse(errorResponse)
//                    .build();
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(notSetupResponse);

    /// /            return globalExceptionHandler.sendErrorResponse(HttpStatus.BAD_REQUEST, "MFA_NOT_SETUP", e.getMessage());
//        }
//    }
    @GetMapping("/v1/mfa/qrcode")
    public ResponseEntity<Map<String, Object>> getMfaQrCode(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String secret = mfaTokenRepository.findByUser(user)
                .map(MfaToken::getSecret)
                .orElseThrow(() -> new BadCredentialsException("MFA not enabled or secret not found for user"));

        String qrCode = mfaService.generateQrCodeImageUrl(user, secret);

        return ResponseEntity.ok(Map.of("qrCodeUrl", qrCode));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Parse token to get its ID and expiration
                Map<String, Object> claims = tokenValidationService.extractClaimsFromPublicAccessToken(token);
                String tokenId = (String) claims.get("jti");
                if (tokenId == null) {
                    throw new SecurityException("Token ID missing");
                }
                tokenRevocationService.revokeToken(tokenId, "User logged out");
                log.info("Logging out user with token: {}", token);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    //TODO: Implement logout-all endpoint
////    @PreAuthorize("hasRole('ROLE_ADMIN')" + "hasRole('ROLE_SUPER_ADMIN')" + "hasRole('ROLE_ROOT')")
////    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
////    @PreFilter("filterObject != authentication.name")
//    @PostMapping("/logout-all")
//    public ResponseEntity<Void> logoutAll(Authentication authentication) {
//        try {
//            tokenRevocationService.revokeAllUserTokens(authentication.getName(), "User logged out from all devices");
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            log.error("Error during logout-all", e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
}

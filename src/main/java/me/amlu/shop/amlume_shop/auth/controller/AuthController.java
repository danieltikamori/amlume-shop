/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.auth.service.AuthenticationInterface;
import me.amlu.shop.amlume_shop.dto.ErrorResponse;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.security.paseto.TokenValidationService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import me.amlu.shop.amlume_shop.user_management.dto.GetRegisterResponse;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.user_management.dto.UserResponse;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    // Keep dependencies for remaining functionality
    private final UserService userService; // For local user data access (if any)
    private final AuthenticationInterface authService; // Use the interface for the authenticator
    private final TokenValidationService tokenValidationService; // REMOVE or adapt for JWT validation
    private final TokenRevocationService tokenRevocationService; // REMOVE or adapt for OAuth2 token revocation

    // Keep GlobalExceptionHandler if used for other error mapping
    private final GlobalExceptionHandler globalExceptionHandler;

    // --- Constructors ---
    // Update constructor parameters
    public AuthController(UserService userService,
                          AuthenticationInterface authService, // Inject the interface
                          TokenValidationService tokenValidationService, // REMOVE or adapt
                          TokenRevocationService tokenRevocationService, // REMOVE or adapt
                          GlobalExceptionHandler globalExceptionHandler) {
        this.userService = userService;
        this.authService = authService; // Assign the injected authenticator
        this.tokenValidationService = tokenValidationService; // REMOVE or adapt
        this.tokenRevocationService = tokenRevocationService; // REMOVE or adapt
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @PostMapping("/v1/register")
    // REMOVE throws RoleNotFoundException as authserver handles role assignment
    public ResponseEntity<GetRegisterResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request, BindingResult bindingResult, HttpServletRequest httpRequest) { // Inject HttpServletRequest for IP
        if (bindingResult.hasErrors()) {
            log.warn("Registration validation failed for user email: {}", request.userEmail());
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, bindingResult.getAllErrors()));
        }

        String ipAddress = httpRequest.getRemoteAddr(); // Get IP address

        try {
            // Call the updated register method which delegates to authserver
            authService.register(request, ipAddress);

            // If authService.register completes without throwing an exception, registration was successful on authserver.
            // We don't get user details back here, just confirmation of success.
            // The local amlume-shop user will be provisioned on first login via OAuth2.

            log.info("Registration request successfully processed for email: {}", request.userEmail());
            // Return 201 Created on success
            return ResponseEntity.status(HttpStatus.CREATED).body(new GetRegisterResponse(null, null));

        } catch (TooManyAttemptsException e) {
            log.warn("Registration failed due to too many attempts for email {}: {}", request.userEmail(), e.getMessage());
            // Map TooManyAttemptsException to 429 Too Many Requests
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new GetRegisterResponse(null, List.of(new ObjectError("rate.limit.exceeded", e.getMessage()))));
        } catch (InvalidCaptchaException e) {
            log.warn("Registration failed due to invalid captcha for email {}: {}", request.userEmail(), e.getMessage());
            // Map InvalidCaptchaException to 400 Bad Request
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, List.of(new ObjectError("captcha.invalid", e.getMessage()))));
        } catch (UserAlreadyExistsException e) {
            log.warn("Registration failed: User already exists for email {}: {}", request.userEmail(), e.getMessage());
            // Map UserAlreadyExistsException (from authserver 409) to 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new GetRegisterResponse(null, List.of(new ObjectError("user.exists", e.getMessage()))));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed due to invalid data for email {}: {}", request.userEmail(), e.getMessage());
            // Map IllegalArgumentException (from authserver 400) to 400 Bad Request
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, List.of(new ObjectError("invalid.data", e.getMessage()))));
        } catch (UserRegistrationException e) {
            log.error("Authserver registration failed for email {}: {}", request.userEmail(), e.getMessage(), e);
            // Map generic UserRegistrationException to 500 Internal Server Error
            ErrorResponse errorResponse = globalExceptionHandler.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", e.getMessage()).getBody();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GetRegisterResponse(null, List.of(new ObjectError("internal.error", errorResponse != null ? errorResponse.message() : "Internal server error"))));
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            log.error("Unexpected error during registration for email {}: {}", request.userEmail(), e.getMessage(), e);
            ErrorResponse errorResponse = globalExceptionHandler.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", "An unexpected error occurred during registration.").getBody();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new GetRegisterResponse(null, List.of(new ObjectError("unexpected.error", errorResponse != null ? errorResponse.message() : "Unexpected server error"))));
        }
    }

    /**
     * Registers a new user with administrative privileges.
     * IMPORTANT: This endpoint MUST be secured to prevent unauthorized admin creation.
     * Only existing administrators (or perhaps during initial setup) should be able to call this.
     *
     * @param request       The registration request details.
     * @param bindingResult Validation results.
     * @return ResponseEntity containing the registered admin user details or errors.
     */
    @PostMapping("/v1/register/admin")
    // --- SECURITY: PROTECT THIS ENDPOINT ---
    // Only allow users with ROLE_ADMIN or ROLE_SUPER_ADMIN to call this
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    // -----------------------------------------
    public ResponseEntity<GetRegisterResponse> registerAdminUser(@Valid @RequestBody UserRegistrationRequest request, BindingResult bindingResult) {
        log.info("Attempting to register new admin user: {}", request.userEmail().getEmail()); // Log attempt

        if (bindingResult.hasErrors()) {
            log.warn("Admin registration validation failed for user {}: {}", request.userEmail().getEmail(), bindingResult.getAllErrors());
            // Use the same response structure as regular registration for consistency
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, bindingResult.getAllErrors()));
        }

        try {
            // NOTE: This method in UserServiceImpl still creates the user locally.
            // If admin users should also be managed centrally by authserver,
            // this logic would also need to call authserver's API (perhaps a different admin endpoint).
            // Assuming for now admin users are managed locally in amlume-shop's DB, but authenticated centrally.
            User registeredAdmin = userService.registerAdminUser(request);
            UserResponse userResponse = new UserResponse(registeredAdmin); // Map to response DTO
            GetRegisterResponse successResponse = new GetRegisterResponse(userResponse, null);
            log.info("Successfully registered admin user: {} (ID: {})", registeredAdmin.getUsername(), registeredAdmin.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(successResponse); // 201 Created

        } catch (UserAlreadyExistsException e) {
            log.warn("Admin registration failed: {}", e.getMessage());
            GetRegisterResponse conflictResponse = new GetRegisterResponse(null, List.of(new ObjectError("user.exists", e.getMessage())));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse); // 409 Conflict

        } catch (DataIntegrityViolationException e) {
            // This might happen if unique constraints (like email) are violated concurrently
            log.error("Admin registration failed due to data integrity violation for user {}: {}", request.userEmail().getEmail(), e.getMessage());
            GetRegisterResponse errorResponse = new GetRegisterResponse(null, List.of(new ObjectError("database.error", "Username or email might already exist.")));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during admin registration for user {}: {}", request.userEmail().getEmail(), e.getMessage(), e);
            // Use GlobalExceptionHandler or return a generic error response
            ErrorResponse errorDetails = globalExceptionHandler.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", "An unexpected error occurred during admin registration.").getBody();
            GetRegisterResponse errorResponse = new GetRegisterResponse(null, List.of(new ObjectError("internal.error", errorDetails != null ? errorDetails.message() : "Internal server error")));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PostMapping("/logout")
    // This endpoint is part of the old PASETO flow.
    // It should be removed or adapted for OAuth2/OIDC logout.
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.warn("/api/auth/logout endpoint is part of the old PASETO flow. Needs adaptation for OAuth2/OIDC logout.");
        // The logout logic in UserAuthenticator.logout needs to be adapted first.
        // If you adapt it to trigger OIDC logout, this controller endpoint might still be the entry point.
        // If you remove local token revocation, this method might just call the adapted authService.logout()
        // and return ResponseEntity.noContent().build();
        // For now, marking as unsupported as the underlying service method is also marked.
        throw new UnsupportedOperationException("Logout endpoint needs to be adapted for OAuth2/OIDC flow.");
    }

    //TODO: Implement logout-all endpoint
////    @PreAuthorize("hasRole('ROLE_ADMIN')" + "hasRole('ROLE_SUPER_ADMIN')" + "hasRole('ROLE_ROOT')")
////    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_ROOT"})
////    @PreFilter("filterObject != authentication.name")
//    @PostMapping("/logout-all")
//    public ResponseEntity<Void> logoutAll(Authentication authentication) {
//        // This endpoint is part of the old PASETO flow.
//        // It should be removed or adapted for OAuth2/OIDC logout.
//        log.warn("/api/auth/logout-all endpoint is part of the old PASETO flow. Needs adaptation for OAuth2/OIDC logout.");
//        throw new UnsupportedOperationException("Logout-all endpoint needs to be adapted for OAuth2/OIDC flow.");
//    }
}

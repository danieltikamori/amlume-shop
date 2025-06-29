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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.auth.service.AuthenticationInterface;
import me.amlu.shop.amlume_shop.dto.ErrorResponse;
import me.amlu.shop.amlume_shop.exceptions.*;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    // Keep dependencies for remaining functionality
    private final UserService userService; // For local user data access (if any)
    private final AuthenticationInterface authService; // Use the interface for the authenticator

    // Keep GlobalExceptionHandler if used for other error mapping
    private final GlobalExceptionHandler globalExceptionHandler;

    // --- Constructors ---
    // Update constructor parameters
    public AuthController(UserService userService,
                          AuthenticationInterface authService, // Inject the interface
                          // REMOVE or adapt
                          // REMOVE or adapt
                          GlobalExceptionHandler globalExceptionHandler) {
        this.userService = userService;
        this.authService = authService; // Assign the injected authenticator
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @PostMapping("/register")
    // REMOVE throws RoleNotFoundException as authserver handles role assignment
    public ResponseEntity<GetRegisterResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request, BindingResult bindingResult, HttpServletRequest httpRequest) { // Inject HttpServletRequest for IP
        if (bindingResult.hasErrors()) {
            log.warn("Registration validation failed for user userEmail: {}", request.userEmail());
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, bindingResult.getAllErrors()));
        }

        String ipAddress = httpRequest.getRemoteAddr(); // Get IP address

            // Call the updated register method which delegates to authserver
            authService.register(request, ipAddress);

            // If authService.register completes without throwing an exception, registration was successful on authserver.
            // We don't get user details back here, just confirmation of success.
            // The local amlume-shop user will be provisioned on first login via OAuth2.

            log.info("Registration request successfully processed for userEmail: {}", request.userEmail());
            // Return 201 Created on success
            return ResponseEntity.status(HttpStatus.CREATED).body(new GetRegisterResponse(null, null));
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
    @PostMapping("/register/admin")
    // --- SECURITY: PROTECT THIS ENDPOINT ---
    // Only allow users with ROLE_ROOT or ROLE_SUPER_ADMIN to call this, perhaps ROLE_ADMIN too.
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    // -----------------------------------------
    public ResponseEntity<GetRegisterResponse> registerAdminUser(@Valid @RequestBody UserRegistrationRequest request, HttpServletRequest httpRequest, BindingResult bindingResult) {
        log.info("Attempting to register new admin user: {}", request.userEmail()); // Log attempt

        if (bindingResult.hasErrors()) {
            log.warn("Admin registration validation failed for user {}: {}", request.userEmail(), bindingResult.getAllErrors());
            // Use the same response structure as regular registration for consistency
            return ResponseEntity.badRequest().body(new GetRegisterResponse(null, bindingResult.getAllErrors()));
        }

        String ipAddress = httpRequest.getRemoteAddr();

        try {
            // NOTE: This method in UserServiceImpl still creates the user locally.
            // If admin users should also be managed centrally by authserver,
            // this logic would also need to call authserver's API (perhaps a different admin endpoint).

            // Call the updated register method which delegates to authserver
            // This will now call a method in authService that specifically targets authserver's admin API
            authService.registerAdmin(request, ipAddress); // New method in AuthenticationInterface

            log.info("Admin registration request successfully processed via authserver for userEmail: {}", request.userEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(new GetRegisterResponse(null, null));

        } catch (UserAlreadyExistsException e) {
            log.warn("Admin registration failed: {}", e.getMessage());
            GetRegisterResponse conflictResponse = new GetRegisterResponse(null, List.of(new ObjectError("user.exists", e.getMessage())));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse); // 409 Conflict

        } catch (DataIntegrityViolationException e) {
            // This might happen if unique constraints (like userEmail) are violated concurrently
            log.error("Admin registration failed due to data integrity violation for user {}: {}", request.userEmail(), e.getMessage());
            GetRegisterResponse errorResponse = new GetRegisterResponse(null, List.of(new ObjectError("database.error", "Username or userEmail might already exist.")));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during admin registration for user {}: {}", request.userEmail(), e.getMessage(), e);
            // Use GlobalExceptionHandler or return a generic error response
            ErrorResponse errorDetails = globalExceptionHandler.sendErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", "An unexpected error occurred during admin registration.").getBody();
            GetRegisterResponse errorResponse = new GetRegisterResponse(null, List.of(new ObjectError("internal.error", errorDetails != null ? errorDetails.message() : "Internal server error")));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Spring Security's LogoutFilter will handle the actual logout.
        // If OidcClientInitiatedLogoutSuccessHandler is configured in SecurityConfig,
        // it will redirect to the authserver's end session endpoint.
        try {
            request.logout(); // Triggers Spring Security's logout mechanism
            // The OidcClientInitiatedLogoutSuccessHandler will then handle the redirect.
            // No need to return a ResponseEntity here, as the redirect will take over.
            // If you need to return a response for an API client, you'd need to
            // configure the logout success handler to return a JSON response instead of redirecting.
            return ResponseEntity.noContent().build(); // Or a 200 OK if no redirect is expected by the client
        } catch (ServletException e) {
            log.error("Error during HTTP session logout: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Logout failed.", e);
        }
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

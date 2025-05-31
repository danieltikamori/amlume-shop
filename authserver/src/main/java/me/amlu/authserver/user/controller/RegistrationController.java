/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import me.amlu.authserver.user.service.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for handling user registration requests.
 * Provides an endpoint for creating new user accounts.
 */
@RestController
@RequestMapping("/api/register") // Or /api/users
public class RegistrationController {

    /**
     * Logger for the RegistrationController class.
     */
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);


    private final UserManager userManager;

    public RegistrationController(UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * Handles user registration requests.
     * Creates a new user account based on the provided registration details.
     *
     * @param request The registration request containing user details.
     * @return A ResponseEntity with status 201 Created on successful registration, or an appropriate error status.
     */
    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody RegistrationRequest request) {
        try {
            // You might want to return a UserProfileResponse DTO or just 201 Created
            userManager.createUser(
                    request.firstName(),
                    request.lastName(),
                    request.nickname(),
                    request.email(), // PRIMARY LOGIN email
                    request.password(),
                    request.mobileNumber(),
                    request.defaultRegion(),
                    request.backupEmail()
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration attempt for already existing email or backup email: {} / {}", request.email(), request.backupEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email or backup email already exists.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid registration request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        // Add other exception handling as needed
    }

    /**
     * Data Transfer Object (DTO) for user registration requests.
     * Contains the necessary fields for creating a new user account.
     *
     * @param firstName The user's first name (required).
     * @param lastName The user's last name (optional).
     * @param nickname The user's nickname (optional).
     * @param email The user's primary email address, used for login (required and must be a valid email format).
     * @param password The user's password (required).
     * @param mobileNumber The user's mobile number (optional).
     * @param defaultRegion The user's default region, potentially used for phone number parsing (optional).
     * @param backupEmail The user's backup email address (optional and must be a valid email format).
     */
    public record RegistrationRequest(
            @NotBlank String firstName,
            String lastName, // Optional
            String nickname, // Optional
            @NotBlank @Email String email, // PRIMARY LOGIN email
            @NotBlank String password, // Consider password complexity rules
            String mobileNumber, // Optional
            String defaultRegion, // Optional, for phone number parsing
            @Email String backupEmail // Optional backup email
    ) {
    }
}

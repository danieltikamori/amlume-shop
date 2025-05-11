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

// In a new RegistrationController.java or similar within authserver
@RestController
@RequestMapping("/api/register") // Or /api/users
public class RegistrationController {
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final UserManager userManager;

    public RegistrationController(UserManager userManager) {
        this.userManager = userManager;
    }

    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody RegistrationRequest request) {
        try {
            // You might want to return a UserProfileResponse DTO or just 201 Created
            userManager.createUser(
                    request.firstName(),
                    request.lastName(),
                    request.nickname(),
                    request.email(),
                    request.password(),
                    request.mobileNumber(),
                    request.defaultRegion()
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (DataIntegrityViolationException e) { // Or a custom UserAlreadyExistsException
            log.warn("Registration attempt for already existing email: {}", request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid registration request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        // Add other exception handling as needed
    }

    // Define a RegistrationRequest DTO
    public record RegistrationRequest(
            @NotBlank String firstName,
            String lastName, // Optional
            String nickname, // Optional
            @NotBlank @Email String email,
            @NotBlank String password, // Consider password complexity rules
            String mobileNumber, // Optional
            String defaultRegion // Optional, for phone number parsing
    ) {
    }
}
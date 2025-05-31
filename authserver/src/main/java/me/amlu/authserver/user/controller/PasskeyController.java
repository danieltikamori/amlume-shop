/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

/*
 * Passkey registration and management endpoints.
 * - POST /api/profile/passkeys/registration-options : Begin registration (returns challenge/options)
 * - POST /api/profile/passkeys                      : Finish registration (save credential)
 * - GET /api/profile/passkeys                      : List user's passkeys
 * - DELETE /api/profile/passkeys/{credentialId}       : Delete a passkey
 */

package me.amlu.authserver.user.controller;

import com.webauthn4j.util.exception.WebAuthnException;
import jakarta.validation.Valid;
import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse;
import me.amlu.authserver.passkey.dto.PostPasskeyRegistrationRequest;
import me.amlu.authserver.passkey.service.PasskeyService;
import me.amlu.authserver.security.util.AuthenticationHelperService;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for managing user passkeys (WebAuthn credentials).
 * Provides endpoints for:
 * <ul>
 *     <li>Beginning passkey registration (getting creation options).</li>
 *     <li>Finishing passkey registration (saving the new credential).</li>
 *     <li>Listing a user's existing passkeys.</li>
 *     <li>Deleting a user's passkey.</li>
 * </ul>
 * All endpoints require an authenticated user.
 */
@RestController
@RequestMapping("/api/profile/passkeys") // Base path for passkey management
public class PasskeyController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyController.class);

    private final PasskeyService passkeyService;
    private final AuthenticationHelperService authenticationHelperService;

    /**
     * Constructs a new PasskeyController.
     *
     * @param passkeyService The service responsible for passkey logic.
     * @param userRepository The repository for user data.
     */
    public PasskeyController(PasskeyService passkeyService, UserRepository userRepository, AuthenticationHelperService authenticationHelperService) {
        this.passkeyService = passkeyService;
        this.authenticationHelperService = authenticationHelperService;
    }

    /**
     * Begins the passkey registration process for the authenticated user.
     * Generates and returns {@link PublicKeyCredentialCreationOptions} which include a challenge
     * that the client (browser) will use to create a new passkey.
     *
     * @param authentication Represents the token for an authentication request or for an authenticated principal once the request has been processed by the AuthenticationManager.
     * @return A {@link ResponseEntity} containing the {@link PublicKeyCredentialCreationOptions} on success.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if there's an issue generating options (400 for {@link IllegalStateException}),
     *                                 or for any other unexpected errors (500).
     */
    @PostMapping("/registration-options")
    public ResponseEntity<PublicKeyCredentialCreationOptions> beginPasskeyRegistration(Authentication authentication) { // Changed parameter
        User currentUser = authenticationHelperService.getAppUserFromAuthentication(authentication); // Resolve User
        if (currentUser == null) {
            log.warn("Attempt to begin passkey registration without resolvable authenticated user.");
            // Consider throwing ResponseStatusException for cleaner error handling if preferred
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            log.debug("User {} requesting passkey registration options", currentUser.getEmail().getValue());
            PublicKeyCredentialCreationOptions options = passkeyService.beginPasskeyRegistration(currentUser);

            // Let Spring MVC's Jackson message converters handle serialization
            return ResponseEntity.ok(options);

        } catch (IllegalStateException e) {
            log.error("Error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate passkey registration options.", e);
        }
    }

    /**
     * Finishes the passkey registration process.
     * The client sends the public key credential created in response to the challenge
     * obtained from {@link #beginPasskeyRegistration(Authentication)} This endpoint validates
     * and saves the new passkey for the authenticated user.
     *
     * @param authentication      Represents the token for an authentication request or for an authenticated principal once the request has been processed by the AuthenticationManager.
     * @param registrationRequest The request body containing the passkey registration data and a friendly name.
     * @return A {@link ResponseEntity} with status 201 (Created) on successful registration.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if the request is invalid or validation fails (400 for {@link IllegalArgumentException},
     *                                 {@link IllegalStateException}, or {@link WebAuthnException}),
     *                                 or for any other unexpected errors (500).
     */
    @PostMapping
    public ResponseEntity<Void> finishPasskeyRegistration(
            Authentication authentication,
            @Valid @RequestBody PostPasskeyRegistrationRequest registrationRequest) {
        User currentUser = authenticationHelperService.getAppUserFromAuthentication(authentication); // Resolve User
        if (currentUser == null) {
            log.warn("Attempt to finish passkey registration without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            log.debug("User {} finishing passkey registration with friendly name: {}",
                    currentUser.getEmail().getValue(), registrationRequest.friendlyName());
            passkeyService.finishPasskeyRegistration(currentUser, registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request finishing passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (WebAuthnException e) {
            log.warn("WebAuthn validation failed finishing passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passkey validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error finishing passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save passkey.", e);
        }
    }

    /**
     * Lists all passkeys registered by the authenticated user.
     *
     * @param authentication Represents the token for an authentication request or for an authenticated principal once the request has been processed by the AuthenticationManager.
     * @return A {@link ResponseEntity} containing a list of {@link GetPasskeyDetailResponse} objects.
     * @throws ResponseStatusException if the user is not authenticated (401).
     */
    @GetMapping
    public ResponseEntity<List<GetPasskeyDetailResponse>> listUserPasskeys(Authentication authentication) { // Changed parameter
        log.debug("/api/profile/passkeys GET called. Authentication object: {}", authentication);
        if (authentication != null && authentication.getPrincipal() != null) {
            log.debug("Principal type: {}, Principal value: {}",
                    authentication.getPrincipal().getClass().getName(),
                    authentication.getPrincipal());
        } else {
            log.warn("/api/profile/passkeys: No authentication object in SecurityContextHolder!");
        }

        User currentUser = authenticationHelperService.getAppUserFromAuthentication(authentication); // Resolve User

        if (currentUser == null) {
            log.warn("Attempt to list passkeys without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("User {} listing their passkeys", currentUser.getEmail().getValue());
        List<GetPasskeyDetailResponse> passkeys = passkeyService.listUserPasskeys(currentUser);
        return ResponseEntity.ok(passkeys);
    }

    /**
     * Deletes a specific passkey belonging to the authenticated user.
     *
     * @param authentication Represents the token for an authentication request or for an authenticated principal once the request has been processed by the AuthenticationManager.
     * @param credentialId   The ID of the passkey credential to delete.
     * @return A {@link ResponseEntity} with status 204 (No Content) on successful deletion.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if the passkey is not found (404 for {@link IllegalArgumentException}),
     *                                 if the user is not authorized to delete the passkey (403 for {@link SecurityException}),
     *                                 or for any other unexpected errors (500).
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Void> deleteUserPasskey(
            Authentication authentication,
            @PathVariable String credentialId) {
        User currentUser = authenticationHelperService.getAppUserFromAuthentication(authentication); // Resolve User
        if (currentUser == null) {
            log.warn("Attempt to delete passkey {} without resolvable authenticated user.", credentialId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            log.debug("User {} attempting to delete passkey with ID: {}", currentUser.getEmail().getValue(), credentialId);
            passkeyService.deleteUserPasskey(currentUser, credentialId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) { // e.g., passkey not found
            log.warn("Failed to delete passkey {} for user {}: {}", credentialId, currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (SecurityException e) { // e.g., user not authorized
            log.error("Authorization failure deleting passkey {} for user {}: {}", credentialId, currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error deleting passkey {} for user {}: {}", credentialId, currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete passkey.", e);
        }
    }
}

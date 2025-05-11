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

import com.webauthn4j.util.exception.WebAuthnException;
import jakarta.validation.Valid;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.passkey.service.PasskeyService;
import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse;
import me.amlu.authserver.passkey.dto.PostPasskeyRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // For cleaner error responses

import java.util.List;

@RestController
@RequestMapping("/api/profile/passkeys") // Base path for passkey management
public class PasskeyController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyController.class);

    private final PasskeyService passkeyService;

    public PasskeyController(PasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    @PostMapping("/registration-options")
    public ResponseEntity<PublicKeyCredentialCreationOptions> beginPasskeyRegistration(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            log.warn("Attempt to begin passkey registration without authentication.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            log.debug("User {} requesting passkey registration options", currentUser.getEmail().getValue());
            PublicKeyCredentialCreationOptions options = passkeyService.beginPasskeyRegistration(currentUser);
            return ResponseEntity.ok(options);
        } catch (IllegalStateException e) {
            log.error("Error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate passkey registration options.", e);
        }
    }

    @PostMapping
    public ResponseEntity<Void> finishPasskeyRegistration(
            @AuthenticationPrincipal User currentUser, @Valid
            @RequestBody PostPasskeyRegistrationRequest registrationRequest) {
        if (currentUser == null) {
            log.warn("Attempt to finish passkey registration without authentication.");
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

    @GetMapping
    public ResponseEntity<List<GetPasskeyDetailResponse>> listUserPasskeys(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            log.warn("Attempt to list passkeys without authentication.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("User {} listing their passkeys", currentUser.getEmail().getValue());
        List<GetPasskeyDetailResponse> passkeys = passkeyService.listUserPasskeys(currentUser);
        return ResponseEntity.ok(passkeys);
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Void> deleteUserPasskey(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String credentialId) {
        if (currentUser == null) {
            log.warn("Attempt to delete passkey {} without authentication.", credentialId);
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
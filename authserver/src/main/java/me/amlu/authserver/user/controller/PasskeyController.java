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
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
// For .getValue()
// For .getValue()
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.web.webauthn.api.ResidentKeyRequirement.*;

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

    /**
     * Constructs a new PasskeyController.
     *
     * @param passkeyService The service responsible for passkey logic.
     */
    public PasskeyController(PasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    /**
     * Begins the passkey registration process for the authenticated user.
     * Generates and returns {@link PublicKeyCredentialCreationOptions} which include a challenge
     * that the client (browser) will use to create a new passkey.
     *
     * @param currentUser The currently authenticated user, injected by Spring Security.
     * @return A {@link ResponseEntity} containing the {@link PublicKeyCredentialCreationOptions} on success.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if there's an issue generating options (400 for {@link IllegalStateException}),
     *                                 or for any other unexpected errors (500).
     */
    @PostMapping("/registration-options")
    public ResponseEntity<Map<String, Object>> beginPasskeyRegistration(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            log.warn("Attempt to begin passkey registration without authentication.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            log.debug("User {} requesting passkey registration options", currentUser.getEmail().getValue());
            PublicKeyCredentialCreationOptions options = passkeyService.beginPasskeyRegistration(currentUser);

            // Manually convert to a Map ensuring correct format for byte arrays and timeout
            Map<String, Object> responseMap = convertCreationOptionsToMap(options);

            return ResponseEntity.ok(responseMap);
        } catch (IllegalStateException e) {
            log.error("Error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error beginning passkey registration for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate passkey registration options.", e);
        }
    }

    private Map<String, Object> convertCreationOptionsToMap(PublicKeyCredentialCreationOptions options) {
        Map<String, Object> map = new HashMap<>();

        // RP Information
        Map<String, String> rpMap = new HashMap<>();
        rpMap.put("id", options.getRp().getId());
        rpMap.put("name", options.getRp().getName());
        map.put("rp", rpMap);

        // User Information
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", options.getUser().getName());
        userMap.put("displayName", options.getUser().getDisplayName());
        userMap.put("id", options.getUser().getId().toBase64UrlString()); // Bytes to Base64URL String
        map.put("user", userMap);

        // Challenge
        map.put("challenge", options.getChallenge().toBase64UrlString()); // Bytes to Base64URL String

        // PubKeyCredParams (usually serializes fine, but ensure alg is just the number)
        map.put("pubKeyCredParams", options.getPubKeyCredParams().stream()
                .map(param -> {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("type", param.getType().getValue()); // e.g., "public-key"
                    paramMap.put("alg", param.getAlg().getValue());   // e.g., -7
                    return paramMap;
                }).collect(Collectors.toList()));


        // Timeout (convert Duration to milliseconds as per WebAuthn spec)
        if (options.getTimeout() != null) {
            map.put("timeout", options.getTimeout().toMillis());
        }

        // ExcludeCredentials
        if (options.getExcludeCredentials() != null) {
            map.put("excludeCredentials", options.getExcludeCredentials().stream()
                    .map(desc -> {
                        Map<String, Object> descMap = new HashMap<>();
                        descMap.put("type", desc.getType().getValue());
                        descMap.put("id", desc.getId().toBase64UrlString()); // Bytes to Base64URL String
                        if (desc.getTransports() != null && !desc.getTransports().isEmpty()) {
                            descMap.put("transports", desc.getTransports().stream()
                                    .map(AuthenticatorTransport::getValue)
                                    .collect(Collectors.toList()));
                        }
                        return descMap;
                    }).collect(Collectors.toList()));
        } else {
            map.put("excludeCredentials", List.of());
        }

        // AuthenticatorSelection
        if (options.getAuthenticatorSelection() != null) {
            AuthenticatorSelectionCriteria authSelect = options.getAuthenticatorSelection();
            Map<String, Object> authSelectMap = new HashMap<>();

            if (authSelect.getAuthenticatorAttachment() != null) {
                authSelectMap.put("authenticatorAttachment", authSelect.getAuthenticatorAttachment().getValue());
            }

            // Handle residentKey (enum) and derive requireResidentKey (boolean)
            if (authSelect.getResidentKey() != null) {
                String residentKeyStringValue = authSelect.getResidentKey().getValue();
                authSelectMap.put("residentKey", residentKeyStringValue); // "discouraged", "preferred", "required"

                // Derive the boolean 'requireResidentKey' based on the string value
                switch (residentKeyStringValue) {
                    case "required": // Compare the string value
                        authSelectMap.put("requireResidentKey", true);
                        break;
                    case "preferred":
                        // For "preferred", requireResidentKey is effectively false.
                        authSelectMap.put("requireResidentKey", false);
                        break;
                    case "discouraged":
                        authSelectMap.put("requireResidentKey", false);
                        break;
                    default:
                        // This case should ideally not be reached if getResidentKey() always returns
                        // one of the defined string values.
                        log.warn("Unknown ResidentKeyRequirement value: {}", residentKeyStringValue);
                        // Decide on a default for requireResidentKey or if an error should be thrown.
                        // Setting to false is a safe default.
                        authSelectMap.put("requireResidentKey", false);
                        break;
                }
            } else {
                // If residentKey property itself is not set on AuthenticatorSelectionCriteria,
                // then requireResidentKey would also typically default to false or be omitted.
                authSelectMap.put("requireResidentKey", false); // Optional: if you want to always include it
            }

            if (authSelect.getUserVerification() != null) {
                authSelectMap.put("userVerification", authSelect.getUserVerification().getValue());
            }

            map.put("authenticatorSelection", authSelectMap);
        }

        // Attestation
        if (options.getAttestation() != null) {
            map.put("attestation", options.getAttestation().getValue());
        }

        // Extensions (usually serializes fine)
        if (options.getExtensions() != null && options.getExtensions().getInputs() != null && !options.getExtensions().getInputs().isEmpty()) {
            // Assuming "credProps" is the only relevant extension for now based on previous logs
            // This might need to be more dynamic if other extensions are used.
            if (options.getExtensions().getInputs().stream().anyMatch(ext -> "credProps".equals(ext.getExtensionId()) && Boolean.TRUE.equals(ext.getInput()))) {
                map.put("extensions", Map.of("credProps", true)); // Simplified based on your log, adjust if more complex
            }
        }


        return map;
    }


    /**
     * Finishes the passkey registration process.
     * The client sends the public key credential created in response to the challenge
     * obtained from {@link #beginPasskeyRegistration(User)}. This endpoint validates
     * and saves the new passkey for the authenticated user.
     *
     * @param currentUser         The currently authenticated user.
     * @param registrationRequest The request body containing the passkey registration data and a friendly name.
     * @return A {@link ResponseEntity} with status 201 (Created) on successful registration.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if the request is invalid or validation fails (400 for {@link IllegalArgumentException},
     *                                 {@link IllegalStateException}, or {@link WebAuthnException}),
     *                                 or for any other unexpected errors (500).
     */
    @PostMapping
    public ResponseEntity<Void> finishPasskeyRegistration(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PostPasskeyRegistrationRequest registrationRequest) {
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

    /**
     * Lists all passkeys registered by the authenticated user.
     *
     * @param currentUser The currently authenticated user.
     * @return A {@link ResponseEntity} containing a list of {@link GetPasskeyDetailResponse} objects.
     * @throws ResponseStatusException if the user is not authenticated (401).
     */
    @GetMapping
    public ResponseEntity<List<GetPasskeyDetailResponse>> listUserPasskeys(
            @AuthenticationPrincipal User currentUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("/api/profile/passkeys called. Authentication object: {}", authentication);
        if (authentication != null) {
            log.debug("Principal type: {}, Principal value: {}",
                    (authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null"),
                    authentication.getPrincipal());
        } else {
            log.warn("/api/profile/passkeys: No authentication object in SecurityContextHolder!");
        }

        if (currentUser == null) {
            log.warn("Attempt to list passkeys without authentication (currentUser is null).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("User {} listing their passkeys", currentUser.getEmail().getValue());
        List<GetPasskeyDetailResponse> passkeys = passkeyService.listUserPasskeys(currentUser);
        return ResponseEntity.ok(passkeys);
    }

    /**
     * Deletes a specific passkey belonging to the authenticated user.
     *
     * @param currentUser  The currently authenticated user.
     * @param credentialId The ID of the passkey credential to delete.
     * @return A {@link ResponseEntity} with status 204 (No Content) on successful deletion.
     * @throws ResponseStatusException if the user is not authenticated (401),
     *                                 if the passkey is not found (404 for {@link IllegalArgumentException}),
     *                                 if the user is not authorized to delete the passkey (403 for {@link SecurityException}),
     *                                 or for any other unexpected errors (500).
     */
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

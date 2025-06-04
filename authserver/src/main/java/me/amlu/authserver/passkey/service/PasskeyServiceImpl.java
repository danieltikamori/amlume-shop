/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.passkey.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.util.exception.WebAuthnException;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse;
import me.amlu.authserver.passkey.dto.PostPasskeyRegistrationRequest;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing passkey (WebAuthn) operations.
 * This service handles the initiation and completion of passkey registration,
 * listing registered passkeys for a user, and deleting passkeys.
 * It interacts with Spring Security's WebAuthn components and custom repositories
 * for managing passkey credentials and user entities.
 */
@Service
public class PasskeyServiceImpl implements PasskeyService {

    private static final Logger log = LoggerFactory.getLogger(PasskeyServiceImpl.class);
    public static final String PASSKEY_REGISTRATION_OPTIONS_SESSION_ATTR = "PASSKEY_REGISTRATION_OPTIONS_JSON";

    /**
     * The timeout duration for WebAuthn relying party operations, must be in milliseconds.
     */
    public static final Duration PASSKEY_RELYING_PARTY_TIMEOUT = Duration.ofMillis(120000L);


    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest; // Inject HttpServletRequest

    /**
     * Constructs a new PasskeyServiceImpl.
     *
     * @param relyingPartyOperations                  The WebAuthn relying party operations.
     * @param publicKeyCredentialUserEntityRepository The repository for PublicKeyCredentialUserEntity.
     * @param passkeyCredentialRepository             The repository for PasskeyCredential.
     * @param objectMapper                            The ObjectMapper for JSON processing.
     * @param httpServletRequest                      The current HttpServletRequest.
     */
    public PasskeyServiceImpl(WebAuthnRelyingPartyOperations relyingPartyOperations,
                              DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository,
                              PasskeyCredentialRepository passkeyCredentialRepository,
                              ObjectMapper objectMapper,
                              HttpServletRequest httpServletRequest) {
        this.relyingPartyOperations = relyingPartyOperations;
        this.publicKeyCredentialUserEntityRepository = publicKeyCredentialUserEntityRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.objectMapper = objectMapper;
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * Initiates the passkey registration process for a given user.
     *
     * @param currentUser The user for whom to begin registration.
     * @return The PublicKeyCredentialCreationOptions to be sent to the client.
     */
    @Override
    @Transactional(readOnly = true) // Typically read-only, no state change here
    @Timed(value = "authserver.passkey.service.begin", description = "Time taken to begin passkey registration")
    public PublicKeyCredentialCreationOptions beginPasskeyRegistration(User currentUser) {
        log.info("Beginning passkey registration for user: {}", currentUser.getEmail().getValue());
        Assert.notNull(currentUser.getExternalId(), "Current user must have an externalId (user handle) for WebAuthn registration.");

        final PublicKeyCredentialUserEntity userEntity = publicKeyCredentialUserEntityRepository.findById(Bytes.fromBase64(currentUser.getExternalId()));
        if (userEntity == null) {
            // This should ideally not happen if externalId is always set.
            // It might imply the user was created without an externalId or mapping failed.
            log.error("Could not find PublicKeyCredentialUserEntity for user {} (externalId {}). This indicates an issue with user setup or mapping.",
                    currentUser.getEmail().getValue(), currentUser.getExternalId());
            throw new IllegalStateException("User entity for WebAuthn not found. Ensure user's externalId is correctly set and mapped.");
        }

        final List<PublicKeyCredentialDescriptor> excludeCredentials =
                passkeyCredentialRepository.findByUserId(currentUser.getId())
                        .stream()
                        .map(cred -> PublicKeyCredentialDescriptor.builder()
                                .type(PublicKeyCredentialType.PUBLIC_KEY)
                                .id(Bytes.fromBase64(cred.getCredentialId()))
                                .transports(parseTransportsToAuthenticatorTransportSet(cred.getTransports()))
                                .build())
                        .collect(Collectors.toList());

        final Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
            // This should ideally not happen if the controller endpoint is secured
            log.error("No authenticated Authentication object found in SecurityContext for user {}.", currentUser.getEmail().getValue());
            throw new IllegalStateException("User is not properly authenticated in the security context.");
        }

        final User finalCurrentUser = currentUser;

        // Implement PublicKeyCredentialCreationOptionsRequest directly
        PublicKeyCredentialCreationOptionsRequest request = new PublicKeyCredentialCreationOptionsRequest() {
            @Override
            public Authentication getAuthentication() {
                // For registering a new passkey for an *already authenticated* user,
                // the current authentication principal is implicitly known.
                // This method might be more relevant for unauthenticated registration flows
                // or if the RelyingPartyOperations needs it explicitly.
                // For Webauthn4JRelyingPartyOperations, it might not be strictly used if userEntity is provided.
                // Or pass the current Authentication object if available and needed

                // Return the current authentication if needed, otherwise null is fine for this flow
                // return SecurityContextHolder.getContext().getAuthentication();
                return currentAuthentication;

            }

            public PublicKeyCredentialUserEntity getUserEntity() {
                return userEntity;
            }

            public List<PublicKeyCredentialDescriptor> getExcludeCredentials() {
                return excludeCredentials;
            }

            public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
                AuthenticatorSelectionCriteria.AuthenticatorSelectionCriteriaBuilder builder = AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.PREFERRED); // Generally good for passkeys

                // Check user roles from finalCurrentUser
                boolean isPrivilegedUser = finalCurrentUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority) // Assuming the User.getAuthorities() returns Spring's GrantedAuthority
                        .anyMatch(roleName -> "ROLE_ADMIN".equals(roleName) || "ROLE_SUPER_ADMIN".equals(roleName)); // Add other sensitive roles

                if (isPrivilegedUser) {
                    log.debug("Privileged user ({}): Applying stricter authenticator selection for passkey registration.", finalCurrentUser.getEmail().getValue());
                    builder.userVerification(UserVerificationRequirement.REQUIRED); // Require PIN/Biometric for admins
                    // Optionally, you could restrict attachment for admins if desired:
                    // builder.authenticatorAttachment(AuthenticatorAttachment.PLATFORM);
                    // Or keep it flexible if they might use synced passkeys from a trusted device.
                } else {
                    log.debug("Standard user ({}): Applying preferred authenticator selection for passkey registration.", finalCurrentUser.getEmail().getValue());
                    builder.userVerification(UserVerificationRequirement.PREFERRED);
                    // For common users, omitting authenticatorAttachment (or setting it to null/ANY)
                    // allows the browser to offer all suitable authenticators, including synced ones
                    // like Google Account passkeys, which is convenient.
                    // builder.authenticatorAttachment(null); // Explicitly allow any
                }
                return builder.build();
            }

            /**
             * DIRECT:
             * The direct preference indicates
             * that the Relying Party wants to receive the attestation statement as generated by the authenticator.
             * ENTERPRISE:
             * The enterprise preference indicates
             * that the Relying Party wants
             * to receive an attestation statement that may include uniquely identifying information.
             * INDIRECT (Preferable option as some devices may not provide attestation statements and thus would cause errors at syncing):
             * The indirect preference indicates
             * that the Relying Party wants to receive a verifiable attestation statement,
             * but allows the client to decide how to obtain such an attestation statement.
             * NONE (default):
             * The none preference indicates that the Relying Party is not interested in authenticator attestation.
             *
             * @return attestation statement conveyance preference (prefer INDIRECT)
             */
            public AttestationConveyancePreference getAttestation() {
                return AttestationConveyancePreference.INDIRECT; // Explicitly NONE, or try DIRECT if issues persist
            }

            public Map<String, Object> getExtensions() {
                // Client-side extensions to be passed to the authenticator
                return Collections.emptyMap();
            }

            public Duration getTimeout() {
                // RP-specific timeout for the ceremony

                return PASSKEY_RELYING_PARTY_TIMEOUT; // 5 seconds by default, can be customized per RP;
            }

        };

        PublicKeyCredentialCreationOptions options = relyingPartyOperations.createPublicKeyCredentialCreationOptions(request);

        // --- Store options in session AS JSON STRING ---
        HttpSession session = httpServletRequest.getSession(true);
        try {
            String optionsJson = objectMapper.writeValueAsString(options);
            session.setAttribute(PASSKEY_REGISTRATION_OPTIONS_SESSION_ATTR, optionsJson);
            log.debug("Stored PublicKeyCredentialCreationOptions (as JSON) in session for user {}", currentUser.getEmail().getValue());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PublicKeyCredentialCreationOptions to JSON for session storage for user {}", currentUser.getEmail().getValue(), e);
            throw new IllegalStateException("Could not serialize passkey options for session", e);
        }

        return options;
    }

    /**
     * Completes the passkey registration process using the response from the client.
     *
     * @param currentUser         The user completing the registration.
     * @param registrationRequest The request containing the client's response data.
     */
    @Override
    @Transactional
    @Timed(value = "authserver.passkey.service", description = "Time taken to finish passkey registration")
    public void finishPasskeyRegistration(User currentUser, PostPasskeyRegistrationRequest registrationRequest) {
        log.info("Finishing passkey registration for user: {} with friendlyName: {}",
                currentUser.getEmail().getValue(), registrationRequest.friendlyName());

        Assert.notNull(currentUser.getExternalId(), "Current user must have an externalId (user handle).");
        Assert.hasText(registrationRequest.friendlyName(), "Friendly name for the passkey is required.");

        // --- Retrieve options from session (AS JSON STRING) ---
        HttpSession session = httpServletRequest.getSession(false); // Get existing session, don't create
        if (session == null) {
            log.error("No active HTTP session found during finishPasskeyRegistration for user {}. Cannot retrieve registration options.", currentUser.getEmail().getValue());
            throw new IllegalStateException("Session expired or not found. Please restart the passkey registration process.");
        }

        String optionsJson = (String) session.getAttribute(PASSKEY_REGISTRATION_OPTIONS_SESSION_ATTR);
        if (optionsJson == null) {
            log.error("PublicKeyCredentialCreationOptions (JSON) not found in session for user {}. Registration flow might be incorrect or session expired.", currentUser.getEmail().getValue());
            throw new IllegalStateException("Passkey registration options not found in session. Please restart the registration process.");
        }
        session.removeAttribute(PASSKEY_REGISTRATION_OPTIONS_SESSION_ATTR); // Clean up
        log.debug("Retrieved and removed PublicKeyCredentialCreationOptions from session for user {}", currentUser.getEmail().getValue());

        // Logs to confirm that the ObjectMapper is the same instance as in LocalSecurityConfig
        log.info("ObjectMapper instance in PasskeyServiceImpl: {} (Hash: {})", objectMapper, System.identityHashCode(objectMapper));
        log.info("PasskeyServiceImpl - Registered module IDs: {}", objectMapper.getRegisteredModuleIds());

        final PublicKeyCredentialCreationOptions creationOptions;
        try {
            // Log TOBEDELETED
            log.info("JSON from sessionto be deserializedL: {}", optionsJson);

            // Deserialize JSON string back to object
            creationOptions = objectMapper.readValue(optionsJson, PublicKeyCredentialCreationOptions.class);
            log.debug("Retrieved and deserialized PublicKeyCredentialCreationOptions from session for user {}", currentUser.getEmail().getValue());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PublicKeyCredentialCreationOptions from JSON in session for user {}", currentUser.getEmail().getValue(), e);
            throw new IllegalStateException("Could not deserialize passkey options from session", e);
        }

        try {
            // Use Spring Security's API types for building the request
            AuthenticatorAttestationResponse attestationResponse = AuthenticatorAttestationResponse.builder()
                    .clientDataJSON(Bytes.fromBase64(registrationRequest.clientDataJSON()))
                    .attestationObject(Bytes.fromBase64(registrationRequest.attestationObject()))
                    .transports(parseTransportsList(registrationRequest.authenticatorAttachment())) // Assuming attachment maps to a single transport for simplicity
                    .build();

            // Handle clientExtensionResults using ObjectMapper
            AuthenticationExtensionsClientOutputs clientExtOutputs = null;
            if (registrationRequest.clientExtensionResults() != null && !registrationRequest.clientExtensionResults().isEmpty()) {
                try {
                    // Convert Map<String, Object> to JSON string, then deserialize to the target type
                    // This relies on WebAuthnJSONModule being registered with the ObjectMapper
                    String extensionsJson = objectMapper.writeValueAsString(registrationRequest.clientExtensionResults());
                    clientExtOutputs = objectMapper.readValue(extensionsJson, ImmutableAuthenticationExtensionsClientOutputs.class);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to process clientExtensionResults for user {}: {}. Proceeding without extensions.", currentUser.getEmail().getValue(), e.getMessage());
                    // Decide if this is a hard error or if registration can proceed without extensions
                }
            }

            // Store the fully constructed credential in a final variable to be used in the anonymous class
            @SuppressWarnings("unchecked") final PublicKeyCredential<AuthenticatorAttestationResponse> finalCredential =
                    (PublicKeyCredential<AuthenticatorAttestationResponse>) PublicKeyCredential.builder()
                            .id(registrationRequest.id())
                            .rawId(Bytes.fromBase64(registrationRequest.rawId()))
                            .response(attestationResponse)
                            .type(PublicKeyCredentialType.valueOf(registrationRequest.type().toUpperCase(Locale.ROOT).replace("-", "_")))
                            .clientExtensionResults(clientExtOutputs)
                            .authenticatorAttachment(registrationRequest.authenticatorAttachment() != null ?
                                    AuthenticatorAttachment.valueOf(registrationRequest.authenticatorAttachment().toUpperCase(Locale.ROOT)) : null)
                            .build();

            RelyingPartyRegistrationRequest relyingPartyRequest = new RelyingPartyRegistrationRequest() {

                //            This typically returns the PublicKeyCredentialCreationOptions used in the registration ceremony.
                @Override
                public PublicKeyCredentialCreationOptions getCreationOptions() {
                    // --- Return retrieved options ---
                    return creationOptions;
                }

//              getPublicKey():

                //            This would return a RelyingPartyPublicKey if your flow needs to reference a specific public key.
//            If your implementation does not use this (which is common in many flows), returning null is fine.
//
//            How to be sure:
//            Check your implementation of registerCredential() (or the library's source code/javadoc) to see if it ever calls these methods and expects a non-null value.
//            If you are not seeing any errors or exceptions at runtime, and registration works as expected, returning null is safe for your use case.
//                Best Practice
//                If you are unsure, add a comment explaining why you are returning null.
//                        If you later add features that require these values, update the implementation accordingly.
                @Override
                public RelyingPartyPublicKey getPublicKey() {
                    return new RelyingPartyPublicKey(finalCredential, registrationRequest.friendlyName());
                }
            };

            relyingPartyOperations.registerCredential(relyingPartyRequest);
            log.info("WebAuthn createPublicKeyCredential (finish registration) successful for user: {}", currentUser.getEmail().getValue());

            // Now, update the friendlyName for the newly created PasskeyCredential
            // The credentialId from the request is the Base64URL string of the rawId.
            // Spring Security's UserCredentialRepository saves it using this ID.
            PasskeyCredential savedPasskey = passkeyCredentialRepository.findByCredentialId(registrationRequest.id())
                    .orElseThrow(() -> {
                        log.error("Failed to find newly registered passkey with ID: {} for user: {}",
                                registrationRequest.id(), currentUser.getEmail().getValue());
                        return new IllegalStateException("Passkey was not saved correctly after registration.");
                    });

            savedPasskey.setFriendlyName(registrationRequest.friendlyName());
            // lastUsedAt might be set by some authenticators during registration, or we can set it now.
            // For simplicity, we'll let the authenticator/library handle it or set it on first use.
            // savedPasskey.setLastUsedAt(Instant.now());
            passkeyCredentialRepository.save(savedPasskey);
            log.info("Updated friendlyName for passkey ID: {} to '{}' for user: {}",
                    registrationRequest.id(), registrationRequest.friendlyName(), currentUser.getEmail().getValue());

        } catch (WebAuthnException e) {
            log.error("WebAuthn finishRegistration failed for user: {}. Error: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw e; // Re-throw to be caught by controller and result in a 400
        } catch (Exception e) {
            log.error("Unexpected error during finishPasskeyRegistration for user: {}. Error: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            // Wrap the original exception to preserve its stack trace
            throw new RuntimeException("Failed to finish passkey registration.", e);
        }
    }

    /**
     * Lists all passkeys registered for the current user.
     *
     * @param currentUser The user whose passkeys are to be listed.
     * @return A list of GetPasskeyDetailResponse objects.
     */
    @Override
    @Transactional(readOnly = true)
    @Timed(value = "authserver.passkey.service.list", description = "Time taken to list user passkeys")
    public List<GetPasskeyDetailResponse> listUserPasskeys(User currentUser) {
        log.debug("Listing passkeys for user: {}", currentUser.getEmail().getValue());
        return passkeyCredentialRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToPasskeyDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a specific passkey for the current user.
     *
     * @param currentUser        The user requesting the deletion.
     * @param credentialIdString The ID of the passkey to delete.
     */
    @Override
    @Transactional
    @Timed(value = "authserver.passkey.service.delete", description = "Time taken to delete user passkey")
    public void deleteUserPasskey(User currentUser, String credentialIdString) {
        log.info("Attempting to delete passkey with ID: {} for user: {}", credentialIdString, currentUser.getEmail().getValue());

        PasskeyCredential passkey = passkeyCredentialRepository.findByCredentialId(credentialIdString)
                .orElseThrow(() -> {
                    log.warn("Passkey not found with ID: {} for deletion attempt by user: {}", credentialIdString, currentUser.getEmail().getValue());
                    return new IllegalArgumentException("Passkey not found."); // Or a more specific "NotFoundException"
                });

        // Important: Verify the passkey belongs to the currently authenticated user
        if (passkey.getUser() == null || !passkey.getUser().getId().equals(currentUser.getId())) {
            log.error("Security violation: User {} attempted to delete passkey ID {} which does not belong to them.",
                    currentUser.getEmail().getValue(), credentialIdString);
            throw new SecurityException("User not authorized to delete this passkey.");
        }

        passkeyCredentialRepository.delete(passkey);
        // Also, ensure Spring Security's UserCredentialRepository is cleaned up if it caches
        // For DbUserCredentialRepository, deleting from PasskeyCredentialRepository is sufficient.
        log.info("Successfully deleted passkey ID: {} for user: {}", credentialIdString, currentUser.getEmail().getValue());
    }

    /**
     * Maps a PasskeyCredential entity to a GetPasskeyDetailResponse DTO.
     *
     * @param credential The PasskeyCredential entity.
     * @return The corresponding GetPasskeyDetailResponse DTO.
     */
    private GetPasskeyDetailResponse mapToPasskeyDetailDto(PasskeyCredential credential) {
        return new GetPasskeyDetailResponse(
                credential.getCredentialId(),
                credential.getFriendlyName(),
                credential.getCreatedAt(),
                credential.getLastUsedAt(),
                parseTransportsToStringSet(credential.getTransports()), // DTO uses Set<String>
                credential.getBackupEligible(),
                credential.getBackupState()
        );
    }

    /**
     * Parses a comma-separated string of authenticator transports into a Set of AuthenticatorTransport enums.
     *
     * @param transportsString The string containing transports.
     * @return A Set of AuthenticatorTransport enums.
     */
    // Returns Set<AuthenticatorTransport> for API objects
    private Set<AuthenticatorTransport> parseTransportsToAuthenticatorTransportSet(String transportsString) {
        if (transportsString == null || transportsString.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(transportsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        // Normalize common variations like "cross-platform" to "CROSS_PLATFORM"
                        return AuthenticatorTransport.valueOf(s.toUpperCase(Locale.ROOT).replace("-", "_"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown authenticator transport string: '{}'. Ignoring.", s);
                        return null; // Will be filtered out by Objects::nonNull if added
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Parses a comma-separated string of authenticator transports into a Set of String representations.
     *
     * @param transportsString The string containing transports.
     * @return A Set of String representations of transports.
     */
    // Returns Set<String> for DTO mapping
    private Set<String> parseTransportsToStringSet(String transportsString) {
        if (transportsString == null || transportsString.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(transportsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Parses a comma-separated string of authenticator transports into a List of AuthenticatorTransport enums.
     *
     * @param transportsString The string containing transports.
     * @return A List of AuthenticatorTransport enums.
     */
    private List<AuthenticatorTransport> parseTransportsList(String transportsString) {
        if (transportsString == null || transportsString.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(transportsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        // Normalize common variations like "cross-platform" to "CROSS_PLATFORM"
                        return AuthenticatorTransport.valueOf(s.toUpperCase(Locale.ROOT).replace("-", "_"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown authenticator transport string: '{}'. Ignoring.", s);
                        return null; // Will be filtered out by Objects::nonNull if added
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

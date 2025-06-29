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
import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse;
import me.amlu.authserver.passkey.dto.PostPasskeyRegistrationRequest;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientOutputs;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * The timeout duration for WebAuthn relying party operations, must be in milliseconds.
     */
    public static final Duration PASSKEY_RELYING_PARTY_TIMEOUT = Duration.ofMillis(120000L);

    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final ObjectMapper webAuthnApiMapper;
    private final HttpServletRequest httpServletRequest;
    private final WebAuthnSessionService webAuthnSessionService;

    /**
     * Constructs a new PasskeyServiceImpl.
     *
     * @param relyingPartyOperations                  The WebAuthn relying party operations.
     * @param publicKeyCredentialUserEntityRepository The repository for PublicKeyCredentialUserEntity.
     * @param passkeyCredentialRepository             The repository for PasskeyCredential.
     * @param webAuthnApiMapper                       The ObjectMapper for JSON processing.
     * @param httpServletRequest                      The current HttpServletRequest.
     * @param webAuthnSessionService                  The service for managing WebAuthn objects in the session.
     */
    public PasskeyServiceImpl(WebAuthnRelyingPartyOperations relyingPartyOperations,
                              DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository,
                              PasskeyCredentialRepository passkeyCredentialRepository,
                              @Qualifier("webAuthnApiMapper") ObjectMapper webAuthnApiMapper,
                              HttpServletRequest httpServletRequest,
                              WebAuthnSessionService webAuthnSessionService) {
        this.relyingPartyOperations = relyingPartyOperations;
        this.publicKeyCredentialUserEntityRepository = publicKeyCredentialUserEntityRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.webAuthnApiMapper = webAuthnApiMapper;
        this.httpServletRequest = httpServletRequest;
        this.webAuthnSessionService = webAuthnSessionService;
    }

    /**
     * Initiates the passkey registration process for a given user.
     *
     * @param currentUser The user for whom to begin registration.
     * @return The PublicKeyCredentialCreationOptions to be sent to the client.
     */
    @Override
    @Transactional(readOnly = true)
    @Timed(value = "authserver.passkey.service.begin", description = "Time taken to begin passkey registration")
    public PublicKeyCredentialCreationOptions beginPasskeyRegistration(User currentUser) {
        log.info("Beginning passkey registration for user: {}", currentUser.getEmail().getValue());
        Assert.notNull(currentUser.getExternalId(), "Current user must have an externalId (user handle) for WebAuthn registration.");

        final PublicKeyCredentialUserEntity userEntity = publicKeyCredentialUserEntityRepository.findById(Bytes.fromBase64(currentUser.getExternalId()));
        if (userEntity == null) {
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
            log.error("No authenticated Authentication object found in SecurityContext for user {}.", currentUser.getEmail().getValue());
            throw new IllegalStateException("User is not properly authenticated in the security context.");
        }

        final User finalCurrentUser = currentUser;
        AuthenticatorSelectionCriteria.AuthenticatorSelectionCriteriaBuilder authSelectorBuilder = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.PREFERRED);
        // ... (your logic for userVerification based on roles) ...
        AuthenticatorSelectionCriteria authenticatorSelection = authSelectorBuilder.build();

        PublicKeyCredentialCreationOptionsRequest request = new CustomCreationOptionsRequest(
                currentAuthentication,
                userEntity,
                excludeCredentials,
                PASSKEY_RELYING_PARTY_TIMEOUT,
                AttestationConveyancePreference.INDIRECT,
                new ImmutableAuthenticationExtensionsClientInputs(), // Or your actual extensions
                authenticatorSelection
        );

        // Implement PublicKeyCredentialCreationOptionsRequest directly
//        PublicKeyCredentialCreationOptionsRequest request = new PublicKeyCredentialCreationOptionsRequest() {
//            @Override
//            public Authentication getAuthentication() {
//                return currentAuthentication;
//            }
//
//            public PublicKeyCredentialUserEntity getUserEntity() {
//                return userEntity;
//            }
//
//            public List<PublicKeyCredentialDescriptor> getExcludeCredentials() {
//                return excludeCredentials;
//            }
//
//            public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
//                AuthenticatorSelectionCriteria.AuthenticatorSelectionCriteriaBuilder builder = AuthenticatorSelectionCriteria.builder()
//                        .residentKey(ResidentKeyRequirement.PREFERRED);
//
//                boolean isPrivilegedUser = finalCurrentUser.getRoles().stream()
//                        .map(GrantedAuthority::getAuthority)
//                        .anyMatch(roleName -> "ROLE_ADMIN".equals(roleName) || "ROLE_SUPER_ADMIN".equals(roleName));
//
//                if (isPrivilegedUser) {
//                    log.debug("Privileged user ({}): Applying stricter authenticator selection for passkey registration.", finalCurrentUser.getEmail().getValue());
//                    builder.userVerification(UserVerificationRequirement.REQUIRED);
//                } else {
//                    log.debug("Standard user ({}): Applying preferred authenticator selection for passkey registration.", finalCurrentUser.getEmail().getValue());
//                    builder.userVerification(UserVerificationRequirement.PREFERRED);
//                }
//                return builder.build();
//            }
//
//            public AttestationConveyancePreference getAttestation() {
//                return AttestationConveyancePreference.INDIRECT;
//            }
//
//            public Map<String, Object> getExtensions() {
//                return Collections.emptyMap();
//            }
//
//            public Duration getTimeout() {
//                return PASSKEY_RELYING_PARTY_TIMEOUT;
//            }
//        };

        PublicKeyCredentialCreationOptions options = relyingPartyOperations.createPublicKeyCredentialCreationOptions(request);

        // Store options in session using WebAuthnSessionService
        webAuthnSessionService.storeCreationOptions(options);
        log.debug("Stored PublicKeyCredentialCreationOptions in session for user {} using WebAuthnSessionService",
                currentUser.getEmail().getValue());

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

        // Retrieve options from session using WebAuthnSessionService
        final PublicKeyCredentialCreationOptions creationOptions = webAuthnSessionService.getCreationOptions();
        if (creationOptions == null) {
            log.error("PublicKeyCredentialCreationOptions not found in session for user {}. Registration flow might be incorrect or session expired.",
                    currentUser.getEmail().getValue());
            throw new IllegalStateException("Passkey registration options not found in session. Please restart the registration process.");
        }

        // Clear options from session after retrieving
        webAuthnSessionService.clearOptions();
        log.debug("Retrieved and cleared PublicKeyCredentialCreationOptions from session for user {}",
                currentUser.getEmail().getValue());

        try {
            // Use Spring Security's API types for building the request
            AuthenticatorAttestationResponse attestationResponse = AuthenticatorAttestationResponse.builder()
                    .clientDataJSON(Bytes.fromBase64(registrationRequest.clientDataJSON()))
                    .attestationObject(Bytes.fromBase64(registrationRequest.attestationObject()))
                    .transports(parseTransportsList(registrationRequest.authenticatorAttachment()))
                    .build();

            // Handle clientExtensionResults using ObjectMapper
            AuthenticationExtensionsClientOutputs clientExtOutputs = null;
            AuthenticationExtensionsClientOutputs receivedClientExtensions = registrationRequest.clientExtensionResults();

            if (receivedClientExtensions != null) {
                ImmutableAuthenticationExtensionsClientOutputs processedExtensions = null;
                if (receivedClientExtensions instanceof ImmutableAuthenticationExtensionsClientOutputs castedImmutable) {
                    // If it's already the correct concrete type, use it directly
                    processedExtensions = castedImmutable;
                } else {
                    // If it's not the expected concrete type, attempt to convert it.
                    // This handles cases where it might be a different implementation of
                    // AuthenticationExtensionsClientOutputs, though less common from Jackson deserialization
                    // with WebauthnJackson2Module.
                    log.warn("clientExtensionResults is of type {}, not ImmutableAuthenticationExtensionsClientOutputs. Attempting conversion.",
                            receivedClientExtensions.getClass().getName());
                    try {
                        String extensionsJson = webAuthnApiMapper.writeValueAsString(receivedClientExtensions);
                        // When deserializing, Jackson will use the constructor that takes List<AuthenticationExtensionsClientOutput<?>>
                        // if the JSON structure matches (e.g., an array of output objects).
                        processedExtensions = webAuthnApiMapper.readValue(extensionsJson, ImmutableAuthenticationExtensionsClientOutputs.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to process/convert clientExtensionResults (type: {}) for user {}: {}. Proceeding without extensions.",
                                receivedClientExtensions.getClass().getName(), currentUser.getEmail().getValue(), e.getMessage());
                        // processedExtensions will remain null
                    }
                }

                // Now, check the (potentially converted) processedExtensions
                if (processedExtensions != null) {
                    List<AuthenticationExtensionsClientOutput<?>> outputsList = processedExtensions.getOutputs();
                    if (outputsList != null && !outputsList.isEmpty()) {
                        clientExtOutputs = processedExtensions; // Use the processed, non-empty extensions
                        log.debug("Successfully processed and will use non-empty clientExtensionResults (based on getOutputs list).");
                    } else if (receivedClientExtensions != null) { // Log only if there was something to begin with
                        log.debug("Received clientExtensionResults were processed but resulted in an empty/null outputs list, or processing failed.");
                    }
                }
            } else {
                log.debug("clientExtensionResults in registrationRequest is null.");
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
                @Override
                public PublicKeyCredentialCreationOptions getCreationOptions() {
                    return creationOptions;
                }

                @Override
                public RelyingPartyPublicKey getPublicKey() {
                    return new RelyingPartyPublicKey(finalCredential, registrationRequest.friendlyName());
                }
            };

            relyingPartyOperations.registerCredential(relyingPartyRequest);
            log.info("WebAuthn createPublicKeyCredential (finish registration) successful for user: {}", currentUser.getEmail().getValue());

            // Update the friendlyName for the newly created PasskeyCredential
            PasskeyCredential savedPasskey = passkeyCredentialRepository.findByCredentialId(registrationRequest.id())
                    .orElseThrow(() -> {
                        log.error("Failed to find newly registered passkey with ID: {} for user: {}",
                                registrationRequest.id(), currentUser.getEmail().getValue());
                        return new IllegalStateException("Passkey was not saved correctly after registration.");
                    });

            savedPasskey.setFriendlyName(registrationRequest.friendlyName());
            passkeyCredentialRepository.save(savedPasskey);
            log.info("Updated friendlyName for passkey ID: {} to '{}' for user: {}",
                    registrationRequest.id(), registrationRequest.friendlyName(), currentUser.getEmail().getValue());

        } catch (WebAuthnException e) {
            log.error("WebAuthn finishRegistration failed for user: {}. Error: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during finishPasskeyRegistration for user: {}. Error: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
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
                    return new IllegalArgumentException("Passkey not found.");
                });

        // Important: Verify the passkey belongs to the currently authenticated user
        if (passkey.getUser() == null || !passkey.getUser().getId().equals(currentUser.getId())) {
            log.error("Security violation: User {} attempted to delete passkey ID {} which does not belong to them.",
                    currentUser.getEmail().getValue(), credentialIdString);
            throw new SecurityException("User not authorized to delete this passkey.");
        }

        passkeyCredentialRepository.delete(passkey);
        log.info("Successfully deleted passkey ID: {} for user: {}", credentialIdString, currentUser.getEmail().getValue());
    }

    // --- Helper Methods ---

    private PublicKeyCredentialUserEntity findUserEntity(User currentUser) {
        PublicKeyCredentialUserEntity userEntity = publicKeyCredentialUserEntityRepository.findById(Bytes.fromBase64(currentUser.getExternalId()));
        if (userEntity == null) {
            log.error("Could not find PublicKeyCredentialUserEntity for user {} (externalId {}).",
                    currentUser.getEmail().getValue(), currentUser.getExternalId());
            throw new IllegalStateException("User entity for WebAuthn not found.");
        }
        return userEntity;
    }

    private List<PublicKeyCredentialDescriptor> getExistingCredentialDescriptors(User currentUser) {
        return passkeyCredentialRepository.findByUserId(currentUser.getId())
                .stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder()
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .id(Bytes.fromBase64(cred.getCredentialId()))
                        .transports(parseTransportsToAuthenticatorTransportSet(cred.getTransports()))
                        .build())
                .collect(Collectors.toList());
    }

    private Authentication getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not properly authenticated in the security context.");
        }
        return authentication;
    }

    private PublicKeyCredentialCreationOptionsRequest createCreationOptionsRequest(User currentUser, PublicKeyCredentialUserEntity userEntity, List<PublicKeyCredentialDescriptor> excludeCredentials, Authentication authentication) {
        AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(isPrivilegedUser(currentUser) ? UserVerificationRequirement.REQUIRED : UserVerificationRequirement.PREFERRED)
                .build();

        return new CustomCreationOptionsRequest(
                authentication,
                userEntity,
                excludeCredentials,
                PASSKEY_RELYING_PARTY_TIMEOUT,
                AttestationConveyancePreference.INDIRECT,
                new ImmutableAuthenticationExtensionsClientInputs(),
                authenticatorSelection
        );
    }

    private boolean isPrivilegedUser(User user) {
        return user.getRoles().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleName -> "ROLE_ADMIN".equals(roleName) || "ROLE_SUPER_ADMIN".equals(roleName));
    }

    private PublicKeyCredential<AuthenticatorAttestationResponse> buildPublicKeyCredential(PostPasskeyRegistrationRequest request) {
        AuthenticatorAttestationResponse attestationResponse = AuthenticatorAttestationResponse.builder()
                .clientDataJSON(Bytes.fromBase64(request.clientDataJSON()))
                .attestationObject(Bytes.fromBase64(request.attestationObject()))
                .transports(parseTransportsList(request.authenticatorAttachment()))
                .build();

        AuthenticationExtensionsClientOutputs clientExtOutputs = processClientExtensions(request.clientExtensionResults());

        return PublicKeyCredential.<AuthenticatorAttestationResponse>builder()
                .id(request.id())
                .rawId(Bytes.fromBase64(request.rawId()))
                .response(attestationResponse)
                .type(PublicKeyCredentialType.valueOf(request.type().toUpperCase(Locale.ROOT).replace("-", "_")))
                .clientExtensionResults(clientExtOutputs)
                .authenticatorAttachment(request.authenticatorAttachment() != null ?
                        AuthenticatorAttachment.valueOf(request.authenticatorAttachment().toUpperCase(Locale.ROOT)) : null)
                .build();
    }

    private AuthenticationExtensionsClientOutputs processClientExtensions(AuthenticationExtensionsClientOutputs receivedExtensions) {
        if (receivedExtensions == null) {
            return null;
        }
        try {
            // Attempt to convert to a known, concrete type if necessary.
            // This handles potential deserialization into a generic map-based implementation.
            String json = webAuthnApiMapper.writeValueAsString(receivedExtensions);
            ImmutableAuthenticationExtensionsClientOutputs processed = webAuthnApiMapper.readValue(json, ImmutableAuthenticationExtensionsClientOutputs.class);
            return (processed.getOutputs() != null && !processed.getOutputs().isEmpty()) ? processed : null;
        } catch (JsonProcessingException e) {
            log.warn("Failed to process clientExtensionResults. Proceeding without extensions. Error: {}", e.getMessage());
            return null;
        }
    }

    private void updatePasskeyFriendlyName(User currentUser, PostPasskeyRegistrationRequest request) {
        PasskeyCredential savedPasskey = passkeyCredentialRepository.findByCredentialId(request.id())
                .orElseThrow(() -> {
                    log.error("Failed to find newly registered passkey with ID: {} for user: {}",
                            request.id(), currentUser.getEmail().getValue());
                    return new IllegalStateException("Passkey was not saved correctly after registration.");
                });

        savedPasskey.setFriendlyName(request.friendlyName());
        passkeyCredentialRepository.save(savedPasskey);
        log.info("Updated friendlyName for passkey ID: {} to '{}' for user: {}",
                request.id(), request.friendlyName(), currentUser.getEmail().getValue());
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
                parseTransportsToStringSet(credential.getTransports()),
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
    private Set<AuthenticatorTransport> parseTransportsToAuthenticatorTransportSet(String transportsString) {
        if (transportsString == null || transportsString.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(transportsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return AuthenticatorTransport.valueOf(s.toUpperCase(Locale.ROOT).replace("-", "_"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown authenticator transport string: '{}'. Ignoring.", s);
                        return null;
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
                .map(this::safelyParseTransport)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

//    private Set<String> parseTransportsToStringSet(String transportsString) {
//        if (transportsString == null || transportsString.isEmpty()) {
//            return Collections.emptySet();
//        }
//        return new HashSet<>(Arrays.asList(transportsString.split(",")));
//    }

    private AuthenticatorTransport safelyParseTransport(String transport) {
        try {
            return AuthenticatorTransport.valueOf(transport.toUpperCase(Locale.ROOT).replace("-", "_"));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown authenticator transport string: '{}'. Ignoring.", transport);
            return null;
        }
    }

    // A simple, concrete implementation of PublicKeyCredentialCreationOptionsRequest for internal use.
    private static class CustomCreationOptionsRequest implements PublicKeyCredentialCreationOptionsRequest {
        private final Authentication authentication;
        private final PublicKeyCredentialUserEntity userEntity;
        private final List<PublicKeyCredentialDescriptor> excludeCredentials;
        private final Duration timeout;
        private final AttestationConveyancePreference attestation;
        private final AuthenticationExtensionsClientInputs extensions;
        private final AuthenticatorSelectionCriteria authenticatorSelection;

        public CustomCreationOptionsRequest(Authentication authentication, PublicKeyCredentialUserEntity userEntity,
                                            List<PublicKeyCredentialDescriptor> excludeCredentials,
                                            Duration timeout, AttestationConveyancePreference attestation,
                                            AuthenticationExtensionsClientInputs extensions,
                                            AuthenticatorSelectionCriteria authenticatorSelection) {
            this.authentication = authentication;
            this.userEntity = userEntity;
            this.excludeCredentials = excludeCredentials;
            this.timeout = timeout;
            this.attestation = attestation;
            this.extensions = extensions;
            this.authenticatorSelection = authenticatorSelection;
        }

        @Override
        public Authentication getAuthentication() {
            return authentication;
        }


        public PublicKeyCredentialUserEntity getUserEntity() {
            return userEntity;
        }

        public List<PublicKeyCredentialDescriptor> getExcludeCredentials() {
            return excludeCredentials;
        }

        public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
            return authenticatorSelection;
        }

        public AttestationConveyancePreference getAttestation() {
            return attestation;
        }

        public Map<String, Object> getExtensions() {
            return extensions != null ? (Map<String, Object>) extensions.getInputs() : Collections.emptyMap();
        } // Ensure it returns Map<String, Object>

        public Duration getTimeout() {
            return timeout;
        }
    }

}

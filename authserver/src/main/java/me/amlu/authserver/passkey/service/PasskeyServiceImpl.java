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
import me.amlu.authserver.passkey.dto.GetPasskeyDetailResponse;
import me.amlu.authserver.passkey.dto.PostPasskeyRegistrationRequest;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.passkey.repository.DbPublicKeyCredentialUserEntityRepository;
import me.amlu.authserver.passkey.repository.PasskeyCredentialRepository;
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PasskeyServiceImpl implements PasskeyService {

    private static final Logger log = LoggerFactory.getLogger(PasskeyServiceImpl.class);

    private final WebAuthnRelyingPartyOperations relyingPartyOperations;
    private final DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final ObjectMapper objectMapper; // Inject ObjectMapper

    public PasskeyServiceImpl(WebAuthnRelyingPartyOperations relyingPartyOperations,
                              DbPublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository,
                              PasskeyCredentialRepository passkeyCredentialRepository,
                              ObjectMapper objectMapper) { // Add ObjectMapper
        this.relyingPartyOperations = relyingPartyOperations;
        this.publicKeyCredentialUserEntityRepository = publicKeyCredentialUserEntityRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true) // Typically read-only, no state change here
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
                return null;

            }

            public PublicKeyCredentialUserEntity getUserEntity() {
                return userEntity;
            }

            public List<PublicKeyCredentialDescriptor> getExcludeCredentials() {
                return excludeCredentials;
            }

            public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
                // Return null for RP defaults, or customize
                // Example: return AuthenticatorSelectionCriteria.builder().userVerification(UserVerificationRequirement.PREFERRED).build();
                return null;
            }

            public AttestationConveyancePreference getAttestation() {
                // Return null for RP defaults, or customize
                // Example: return AttestationConveyancePreference.DIRECT;
                return null;
            }

            public Map<String, Object> getExtensions() {
                // Client-side extensions to be passed to the authenticator
                return Collections.emptyMap();
            }

            public Long getTimeout() {
                // RP-specific timeout for the ceremony
                return null;
            }

        };

        return relyingPartyOperations.createPublicKeyCredentialCreationOptions(request);
    }

    @Override
    @Transactional
    public void finishPasskeyRegistration(User currentUser, PostPasskeyRegistrationRequest registrationRequest) {
        log.info("Finishing passkey registration for user: {} with friendlyName: {}",
                currentUser.getEmail().getValue(), registrationRequest.friendlyName());

        Assert.notNull(currentUser.getExternalId(), "Current user must have an externalId (user handle).");
        Assert.hasText(registrationRequest.friendlyName(), "Friendly name for the passkey is required.");

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

            // Explicitly type the PublicKeyCredential builder
            @SuppressWarnings("unchecked") final PublicKeyCredential<AuthenticatorAttestationResponse> credential = (PublicKeyCredential<AuthenticatorAttestationResponse>) PublicKeyCredential.builder()
                    .id(registrationRequest.id())
                    .rawId(Bytes.fromBase64(registrationRequest.rawId()))
                    .response(attestationResponse)
                    .type(PublicKeyCredentialType.valueOf(registrationRequest.type().toUpperCase().replace("-", "_")))
                    .clientExtensionResults(clientExtOutputs)
                    .authenticatorAttachment(registrationRequest.authenticatorAttachment() != null ?
                            AuthenticatorAttachment.valueOf(registrationRequest.authenticatorAttachment().toUpperCase()) : null)
                    .build();

            final Bytes userHandle = Bytes.fromBase64(currentUser.getExternalId()); // Defined once

            RelyingPartyRegistrationRequest relyingPartyRequest = new RelyingPartyRegistrationRequest() {
                public PublicKeyCredential<AuthenticatorAttestationResponse> getPublicKeyCredential() {
                    return credential;
                }

                public Bytes getUserEntityUserId() {
                    return userHandle;
                }

                //            It is generally OK to return null from getCreationOptions() and getPublicKey() in your anonymous RelyingPartyRegistrationRequest implementation if your WebAuthnRelyingPartyOperations.registerCredential() implementation does not require or use these values.
//
//              Details:
//              getCreationOptions():
//
//            This typically returns the PublicKeyCredentialCreationOptions used in the registration ceremony.
//            If your registration flow does not require this (for example, if all necessary data is provided via other methods or is not needed for your backend logic), returning null is fine.
//            If the method is called and expects a non-null value, you will get a NullPointerException.
                @Override
                public PublicKeyCredentialCreationOptions getCreationOptions() {
                    return null;
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
                    return null;
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
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during finishPasskeyRegistration for user: {}. Error: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new RuntimeException("Failed to finish passkey registration.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetPasskeyDetailResponse> listUserPasskeys(User currentUser) {
        log.debug("Listing passkeys for user: {}", currentUser.getEmail().getValue());
        return passkeyCredentialRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToPasskeyDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
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
                        return AuthenticatorTransport.valueOf(s.toUpperCase().replace("-", "_"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown authenticator transport string: '{}'. Ignoring.", s);
                        return null; // Will be filtered out by Objects::nonNull if added
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

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
                        return AuthenticatorTransport.valueOf(s.toUpperCase().replace("-", "_"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown authenticator transport string: '{}'. Ignoring.", s);
                        return null; // Will be filtered out by Objects::nonNull if added
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

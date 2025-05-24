/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.passkey.repository;

import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.springframework.security.web.webauthn.api.*;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component // Ensure this is a Spring managed bean
public class DbUserCredentialRepository implements UserCredentialRepository {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DbUserCredentialRepository.class);

    private final PasskeyCredentialRepository credentialRepository;
    private final UserRepository userRepository;

    public DbUserCredentialRepository(PasskeyCredentialRepository credentialRepository, UserRepository userRepository) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
    }

    private static CredentialRecord toCredentialRecord(PasskeyCredential credential, Bytes userHandle) {
        // log.info("toCredentialRecord: credentialId={}, userHandle={}", credential.getCredentialId(), userHandle.toBase64UrlString());
        ImmutableCredentialRecord.ImmutableCredentialRecordBuilder
                builder = ImmutableCredentialRecord.builder()
                .userEntityUserId(userHandle) // This is the User.externalId
                .label(credential.getFriendlyName()) // Map from friendlyName
                .credentialType(PublicKeyCredentialType.valueOf(credential.getCredentialType()))
                // credential.getCredentialId() is a Base64URL string, Bytes.fromBase64 handles this
                .credentialId(Bytes.fromBase64(credential.getCredentialId()))
                // credential.getPublicKeyCose() is byte[], use new ImmutablePublicKeyCose(byte[])
                .publicKey(new ImmutablePublicKeyCose(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignatureCount())
                .uvInitialized(credential.getUvInitialized() != null && credential.getUvInitialized())
                .transports(asTransportSet(credential.getTransports()))
                .backupEligible(credential.getBackupEligible() != null && credential.getBackupEligible())
                .backupState(credential.getBackupState() != null && credential.getBackupState());

        if (credential.getAttestationObject() != null) {
            // credential.getAttestationObject() is byte[], use new Bytes()
            builder.attestationObject(new Bytes(credential.getAttestationObject()));
        }
        if (credential.getLastUsedAt() != null) {
            builder.lastUsed(credential.getLastUsedAt());
        }
        if (credential.getCreatedAt() != null) {
            builder.created(credential.getCreatedAt());
        }
        return builder.build();
    }

    private static Set<AuthenticatorTransport> asTransportSet(String transports) {
        if (transports == null || transports.isEmpty()) {
            return Collections.emptySet(); // Return empty set, not Set.of() which is for non-empty
        }
        return Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AuthenticatorTransport::valueOf)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void delete(Bytes credentialId) {
        log.info("delete PasskeyCredential by credentialId: {}", credentialId.toBase64UrlString());
        credentialRepository.findByCredentialId(credentialId.toBase64UrlString())
                .ifPresent(credentialRepository::delete);
    }

    @Override
    @Transactional
    public void save(CredentialRecord credentialRecord) {
        log.info("Attempting to save CredentialRecord for userHandle: {}, credentialId: {}",
                credentialRecord.getUserEntityUserId().toBase64UrlString(),
                credentialRecord.getCredentialId().toBase64UrlString());

        userRepository.findByExternalId(credentialRecord.getUserEntityUserId().toBase64UrlString())
                .ifPresentOrElse(user -> {
                    PasskeyCredential passkeyToSave = credentialRepository.findByCredentialId(credentialRecord.getCredentialId().toBase64UrlString())
                            .map(existingCredential -> {
                                log.debug("Updating existing PasskeyCredential with id: {}", existingCredential.getId());
                                return updatePasskeyCredential(existingCredential, credentialRecord, user);
                            })
                            .orElseGet(() -> {
                                log.debug("Creating new PasskeyCredential for user: {}", user.getId());
                                return newPasskeyCredential(credentialRecord, user);
                            });

                    credentialRepository.save(passkeyToSave);
                    log.info("Saved PasskeyCredential: userFirstName={}, externalId={}, passkeyFriendlyName={}",
                            user.getFirstName(), user.getExternalId(), passkeyToSave.getFriendlyName());
                }, () -> log.warn("User not found with externalId: {}. Cannot save PasskeyCredential.",
                        credentialRecord.getUserEntityUserId().toBase64UrlString()));
    }

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        log.info("findByCredentialId: id={}", credentialId.toBase64UrlString());
        return credentialRepository.findByCredentialId(credentialId.toBase64UrlString())
                .map(cred -> {
                    User user = cred.getUser(); // Get user directly from PasskeyCredential
                    if (user == null || !StringUtils.hasText(user.getExternalId())) {
                        log.error("PasskeyCredential with id {} is missing a valid associated user or user externalId.", cred.getId());
                        return null; // Or throw exception
                    }
                    // User.externalId is a Base64URL string, Bytes.fromBase64 handles this
                    return toCredentialRecord(cred, Bytes.fromBase64(user.getExternalId()));
                })
                .orElse(null);
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userHandle) { // Parameter name changed for clarity (userHandle is externalId)
        log.info("findByUserId (userHandle): {}", userHandle.toBase64UrlString());

        return userRepository.findByExternalId(userHandle.toBase64UrlString())
                .map(user -> credentialRepository.findByUserId(user.getId()) // Use User's internal ID
                        .stream()
                        // User.externalId is a Base64URL string, Bytes.fromBase64 handles this
                        .map(cred -> toCredentialRecord(cred, Bytes.fromBase64(user.getExternalId())))
                        .collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    private PasskeyCredential updatePasskeyCredential(PasskeyCredential credential, CredentialRecord credentialRecord, User user) {
        // User should already be set and correct for an existing credential
        // credential.setUser(user); // Not AggregateReference for JPA
        credential.setFriendlyName(credentialRecord.getLabel());
        credential.setCredentialType(credentialRecord.getCredentialType().getValue());
        // credentialRecord.getPublicKey() is PublicKeyCose, use its instance method getBytes()
        credential.setPublicKeyCose(credentialRecord.getPublicKey().getBytes());
        credential.setSignatureCount(credentialRecord.getSignatureCount());
        credential.setUvInitialized(credentialRecord.isUvInitialized());
        credential.setTransports(credentialRecord.getTransports().stream().map(AuthenticatorTransport::getValue).collect(Collectors.joining(",")));
        credential.setBackupEligible(credentialRecord.isBackupEligible());
        credential.setBackupState(credentialRecord.isBackupState());
        if (credentialRecord.getAttestationObject() != null) {
            // credentialRecord.getAttestationObject() is Bytes, use its instance method getBytes()
            credential.setAttestationObject(credentialRecord.getAttestationObject().getBytes());
        }
        credential.setLastUsedAt(credentialRecord.getLastUsed());
        // Created date should not change for an existing credential
        // credential.setCreatedAt(credentialRecord.getCreated());
        return credential;
    }

    private PasskeyCredential newPasskeyCredential(CredentialRecord credentialRecord, User user) {
        PasskeyCredential credential = new PasskeyCredential();
        credential.setUser(user); // Set the JPA User entity
        credential.setUserHandle(user.getExternalId()); // Store the user handle used during registration
        credential.setFriendlyName(credentialRecord.getLabel());
        credential.setCredentialType(credentialRecord.getCredentialType().getValue());
        // credentialRecord.getCredentialId() is Bytes, use its instance method toBase64UrlString()
        credential.setCredentialId(credentialRecord.getCredentialId().toBase64UrlString());
        // credentialRecord.getPublicKey() is PublicKeyCose, use its instance method getBytes()
        credential.setPublicKeyCose(credentialRecord.getPublicKey().getBytes());
        credential.setSignatureCount(credentialRecord.getSignatureCount());
        credential.setUvInitialized(credentialRecord.isUvInitialized());
        credential.setTransports(credentialRecord.getTransports().stream().map(AuthenticatorTransport::getValue).collect(Collectors.joining(",")));
        credential.setBackupEligible(credentialRecord.isBackupEligible());
        credential.setBackupState(credentialRecord.isBackupState());
        if (credentialRecord.getAttestationObject() != null) {
            // credentialRecord.getAttestationObject() is Bytes, use its instance method getBytes()
            credential.setAttestationObject(credentialRecord.getAttestationObject().getBytes());
        }
        credential.setLastUsedAt(credentialRecord.getLastUsed());
        // CreatedAt will be set by @CreationTimestamp
        // credential.setCreatedAt(credentialRecord.getCreated());
        return credential;
    }
}

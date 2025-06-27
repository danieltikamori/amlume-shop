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

import me.amlu.authserver.oauth2.repository.AuthorityRepository;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.repository.UserRepository;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;


@Component
public class DbPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DbPublicKeyCredentialUserEntityRepository.class);
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public DbPublicKeyCredentialUserEntityRepository(UserRepository userRepository, AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
    }

    public static PublicKeyCredentialUserEntity mapUserToPublicKeyCredentialUserEntity(User user) {
        // Optional logging
        log.debug("mapUserToPublicKeyCredentialUserEntity: Attempting to map User with id: {}, email: {}",
                user.getId(), user.getEmail() != null ? user.getEmail().getValue() : "null");

        if (user.getExternalId() == null) {
            log.error("USER_MAP_FAIL: User with internal id {} and email '{}' has a null externalId. Cannot map to PublicKeyCredentialUserEntity.",
                    user.getId(), user.getEmail() != null ? user.getEmail().getValue() : "null");
            return null;
        }
        if (user.getEmail() == null || !StringUtils.hasText(user.getEmail().getValue())) {
            log.error("USER_MAP_FAIL: User with internal id {} and externalId '{}' has a null or blank email. Cannot map to PublicKeyCredentialUserEntity.name.",
                    user.getId(), user.getExternalId());
            return null;
        }

        Bytes userHandleBytes;
        try {
            // Assuming user.getExternalId() is Base64URL encoded string
            userHandleBytes = Bytes.fromBase64(user.getExternalId());
        } catch (IllegalArgumentException e) {
            log.error("USER_MAP_FAIL: Failed to decode Base64URL externalId '{}' for user internal_id={}: {}",
                    user.getExternalId(), user.getId(), e.getMessage(), e);
            return null;
        }

        ImmutablePublicKeyCredentialUserEntity.PublicKeyCredentialUserEntityBuilder builder =
                ImmutablePublicKeyCredentialUserEntity.builder()
                        .id(userHandleBytes) // Use the successfully created Bytes object
                        .name(user.getEmail().getValue());

        // Construct displayName for WebAuthn from givenName, middleName, and surname
        StringBuilder webAuthnDisplayName = new StringBuilder();
        if (StringUtils.hasText(user.getGivenName())) {
            webAuthnDisplayName.append(user.getGivenName());
        }
        if (StringUtils.hasText(user.getMiddleName())) {
            if (!webAuthnDisplayName.isEmpty()) {
                webAuthnDisplayName.append(" ");
            }
            webAuthnDisplayName.append(user.getMiddleName());
        }
        if (StringUtils.hasText(user.getSurname())) {
            if (!webAuthnDisplayName.isEmpty()) {
                webAuthnDisplayName.append(" ");
            }
            webAuthnDisplayName.append(user.getSurname());
        }

        // If nickname is preferred for display and available, use it. Otherwise, use concatenated first/last.
        // For WebAuthn's "displayName", the concatenated real name is usually more appropriate.
        // If nickname were to be used, the logic would be:
        // String finalDisplayName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : webAuthnDisplayName.toString();

        if (!webAuthnDisplayName.isEmpty()) {
            builder.displayName(webAuthnDisplayName.toString());
        } else if (StringUtils.hasText(user.getNickname())) {
            // Fallback to nickname if first/last name is blank but nickname exists
            builder.displayName(user.getNickname());
        } else {
            builder.displayName("Customer");
        }
        // If all are blank, PublicKeyCredentialUserEntity.displayName will be null.

        PublicKeyCredentialUserEntity mappedEntity = builder.build();
        log.debug("mapUserToPublicKeyCredentialUserEntity: Successfully MAPPED to PublicKeyCredentialUserEntity with id: {}, name: {}, displayName: {}",
                mappedEntity.getId().toBase64UrlString(), mappedEntity.getName(), mappedEntity.getDisplayName());

        return mappedEntity;
    }

    /**
     * Parses a full name string into first name, middle name, and last name.
     *
     * @param fullName The full name string.
     * @return A String array where [0] is givenName, [1] is middleName, and [2] is surname (can be null).
     */
    private static String[] parseFullName(String fullName) {
        // Use these logs if encounter frequent parsing issues
        // log.debug("parseFullName: Input fullName='{}'", fullName);

        String[] parts = new String[3]; // [0] = givenName, [1] = middleName, [2] = surname
        if (!StringUtils.hasText(fullName)) {
            return parts; // All null
        }

        String trimmedFullName = fullName.trim();
        String[] nameParts = trimmedFullName.split(" ");

        if (nameParts.length == 1) {
            // Only given name
            parts[0] = nameParts[0];
        } else if (nameParts.length == 2) {
            // Given name and surname
            parts[0] = nameParts[0];
            parts[2] = nameParts[1];
        } else if (nameParts.length >= 3) {
            // Given name, middle name(s), and surname
            parts[0] = nameParts[0];

            // Middle part - combine all middle names
            StringBuilder middleName = new StringBuilder();
            for (int i = 1; i < nameParts.length - 1; i++) {
                if (i > 1) middleName.append(" ");
                middleName.append(nameParts[i]);
            }
            parts[1] = middleName.toString();

            // Last part is surname
            parts[2] = nameParts[nameParts.length - 1];
        }

        // log.debug("parseFullName: Output parts=[{}, {}, {}]", parts[0], parts[1], parts[2]);
        return parts;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        // 'id' here is the user handle (User.externalId) from WebAuthn perspective
        log.debug("findById (WebAuthn UserHandle): id={}", id.toBase64UrlString());
        String externalId = id.toBase64UrlString();
        PublicKeyCredentialUserEntity result = userRepository.findByExternalId(externalId)
                .map(DbPublicKeyCredentialUserEntityRepository::mapUserToPublicKeyCredentialUserEntity)
                .orElse(null);
        if (result != null) {
            log.debug("findById: Found PublicKeyCredentialUserEntity for externalId: {}", externalId);
        } else {
            log.debug("findById: No PublicKeyCredentialUserEntity found for externalId: {}", externalId);
        }
        return result;
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        // 'username' here is the WebAuthn user.name, which we map to User.email
        log.debug("findByUsername (WebAuthn Username, which is Email): username={}", username);
        PublicKeyCredentialUserEntity result = userRepository.findByEmail_Value(username)
                .map(DbPublicKeyCredentialUserEntityRepository::mapUserToPublicKeyCredentialUserEntity)
                .orElse(null);
        if (result != null) {
            log.debug("findByUsername: Found PublicKeyCredentialUserEntity for username (email): {}", username);
        } else {
            log.debug("findByUsername: No PublicKeyCredentialUserEntity found for username (email): {}", username);
        }
        return result;
    }

    @Override
    @Transactional
    public void save(PublicKeyCredentialUserEntity webAuthnUserEntity) {
        log.info("DbPublicKeyCredentialUserEntityRepository.save called with WebAuthn UserEntity: name='{}', displayName='{}', id='{}'",
                webAuthnUserEntity.getName(), webAuthnUserEntity.getDisplayName(), webAuthnUserEntity.getId().toBase64UrlString());

        String externalIdFromWebAuthnEntity = webAuthnUserEntity.getId().toBase64UrlString();
        String nameFromWebAuthnEntity = webAuthnUserEntity.getName();
        String displayNameFromWebAuthnEntity = webAuthnUserEntity.getDisplayName();

        // 1. Try to find an existing user by the externalId (WebAuthn user handle)
        // By the externalId provided in the webAuthnUserEntity.
        // This externalId is the one generated by WebAuthnRelyingPartyOperations if it created a new user entity.
        User userToSave = userRepository.findByExternalId(externalIdFromWebAuthnEntity)
                .map(existingUser -> {
                    // This user was found by the externalId/userHandle that WebAuthn library is using.
                    log.debug("Found and Updating existing user (ID: {}) for WebAuthn externalId: {}", existingUser.getId(), externalIdFromWebAuthnEntity);

                    // Update display name if provided and different.
                    // The email (existingUser.getEmail()) should NOT be changed by webAuthnUserEntity.getName() here.
                    if (StringUtils.hasText(displayNameFromWebAuthnEntity) && !displayNameFromWebAuthnEntity.equals(existingUser.getDisplayableFullName())) {
                        String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                        if (!Objects.equals(nameParts[0], existingUser.getGivenName())) {
                            existingUser.updateGivenName(nameParts[0]);
                        }
                        if (!Objects.equals(nameParts[1], existingUser.getMiddleName())) {
                            existingUser.updateMiddleName(nameParts[1]);
                        }
                        if (!Objects.equals(nameParts[2], existingUser.getSurname())) {
                            existingUser.updateSurname(nameParts[2]);
                        }
                    }
                    // Ensure the nameFromWebAuthnEntity (which could be the GitHub ID) does not overwrite a valid email.
                    // If existingUser.getEmail() is valid, it's the source of truth.
                    log.debug("Existing user's email: {}. WebAuthn entity name field: {}. Email will not be changed by this WebAuthn save operation if already valid.",
                            existingUser.getEmail().getValue(), nameFromWebAuthnEntity);

                    return existingUser;
                })
                .orElseGet(() -> {
                    // No user found with externalIdFromWebAuthnEntity.
                    // This means it's either a passkey-first registration (nameFromWebAuthnEntity is email)
                    // or an error if nameFromWebAuthnEntity is not an email.

                    // 2. If no user found by externalId, try to find by email (nameFromWebAuthnEntity)
                    // This covers the case where an existing user (email/password, OAuth2) is adding their first passkey.
                    if (EmailAddress.isValid(nameFromWebAuthnEntity)) {
                        return userRepository.findByEmail_Value(nameFromWebAuthnEntity)
                                .map(existingUserByEmail -> {
                                    log.info("Found existing user (ID: {}) by email '{}'. Associating WebAuthn externalId '{}'.",
                                            existingUserByEmail.getId(), nameFromWebAuthnEntity, externalIdFromWebAuthnEntity);
                                    // Set the externalId for this existing user
                                    existingUserByEmail.setExternalId(externalIdFromWebAuthnEntity); // Requires setExternalId
                                    // Update display name if provided
                                    if (StringUtils.hasText(displayNameFromWebAuthnEntity) && !displayNameFromWebAuthnEntity.equals(existingUserByEmail.getDisplayableFullName())) {
                                        String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                                        existingUserByEmail.updateGivenName(nameParts[0]);
                                        existingUserByEmail.updateMiddleName(nameParts[1]);
                                        existingUserByEmail.updateSurname(nameParts[2]);
                                    }
                                    return existingUserByEmail;
                                })
                                .orElseGet(() -> {
                                    // 3. If still no user found (neither by externalId nor by email), create a brand new user.
                                    log.info("No existing user found by externalId or email. Creating new user for email '{}' with externalId '{}'.",
                                            nameFromWebAuthnEntity, externalIdFromWebAuthnEntity);
                                    User newUser = User.builder()
                                            .email(new EmailAddress(nameFromWebAuthnEntity))
                                            .externalId(externalIdFromWebAuthnEntity)
                                            .givenName(parseFullName(displayNameFromWebAuthnEntity)[0]) // Try to parse from display name
                                            .middleName(parseFullName(displayNameFromWebAuthnEntity)[1])
                                            .surname(parseFullName(displayNameFromWebAuthnEntity)[2])
                                            .build();
                                    newUser.enableAccount();
                                    authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
                                    return newUser;
                                });
                    } else {
                        // nameFromWebAuthnEntity is NOT a valid email (e.g., GitHub ID, or just a username).
                        // This implies the user *should* have been provisioned via OAuth2/OIDC first,
                        // and their externalId should have been set. If we reach here, it's an error.
                        log.error("Cannot create or find user: The 'name' field ('{}') from PublicKeyCredentialUserEntity is not a valid email, " +
                                        "and no existing user found with the WebAuthn-provided externalId '{}'. " +
                                        "This indicates an issue in the WebAuthn flow (e.g., trying to add passkey to non-existent OAuth2/OIDC user) " +
                                        "or a misconfiguration.",
                                nameFromWebAuthnEntity, externalIdFromWebAuthnEntity);
                        throw new IllegalStateException("Failed to process WebAuthn user entity: 'name' field ('" + nameFromWebAuthnEntity +
                                "') is not a valid email, and no existing user found with the WebAuthn-provided externalId '" +
                                externalIdFromWebAuthnEntity + "'.");
                    }
                });
        userRepository.save(userToSave);
        log.info("User entity saved/updated: id={}, externalId={}, email={}",
                userToSave.getId(), userToSave.getExternalId(), userToSave.getEmail().getValue());
    }

    @Override
    @Transactional
    public void delete(Bytes id) {
        String externalId = id.toBase64UrlString();
        log.info("delete (WebAuthn UserHandle): externalId={}", externalId);
        userRepository.findByExternalId(externalId)
                .ifPresentOrElse(
                        user -> {
                            log.debug("delete: Found user with externalId {}. Proceeding with deletion.", externalId);
                            userRepository.delete(user);
                        },
                        () -> log.warn("Attempted to delete non-existent user with externalId: {}", externalId)
                );

    }
}

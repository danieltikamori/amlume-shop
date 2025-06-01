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

        // Construct displayName for WebAuthn from firstName and lastName
        StringBuilder webAuthnDisplayName = new StringBuilder();
        if (StringUtils.hasText(user.getFirstName())) {
            webAuthnDisplayName.append(user.getFirstName());
        }
        if (StringUtils.hasText(user.getLastName())) {
            if (!webAuthnDisplayName.isEmpty()) {
                webAuthnDisplayName.append(" ");
            }
            webAuthnDisplayName.append(user.getLastName());
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
     * Parses a full name string into first name and last name.
     *
     * @param fullName The full name string.
     * @return A String array where [0] is firstName and [1] is lastName (can be null).
     */
    private static String[] parseFullName(String fullName) {
        // Use these 2 logs if encounter frequent parsing issues

        // log.debug("parseFullName: Input fullName='{}'", fullName);


        String[] parts = new String[2]; // [0] = firstName, [1] = lastName
        if (!StringUtils.hasText(fullName)) {
            return parts; // Both null
        }
        String trimmedFullName = fullName.trim();
        int firstSpace = trimmedFullName.indexOf(' ');
        if (firstSpace != -1) {
            parts[0] = trimmedFullName.substring(0, firstSpace);
            parts[1] = trimmedFullName.substring(firstSpace + 1).trim();
            if (!StringUtils.hasText(parts[1])) {
                parts[1] = null;
            }
        } else {
            parts[0] = trimmedFullName;
            // parts[1] remains null
        }

        // log.debug("parseFullName: Output parts=[{}, {}]", parts[0], parts[1]);

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

        // Try to find an existing user by the externalId provided in the webAuthnUserEntity.
        // This externalId is the one generated by WebAuthnRelyingPartyOperations if it created a new user entity.
        User userToSave = userRepository.findByExternalId(externalIdFromWebAuthnEntity)
                .map(existingUser -> {
                    // This user was found by the externalId/userHandle that WebAuthn library is using.
                    log.debug("Updating existing user (ID: {}) for WebAuthn externalId: {}", existingUser.getId(), externalIdFromWebAuthnEntity);

                    // Update display name if provided and different.
                    // The email (existingUser.getEmail()) should NOT be changed by webAuthnUserEntity.getName() here.
                    if (StringUtils.hasText(displayNameFromWebAuthnEntity) && !displayNameFromWebAuthnEntity.equals(existingUser.getDisplayableFullName())) {
                        String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                        if (!Objects.equals(nameParts[0], existingUser.getFirstName())) {
                            existingUser.updateFirstName(nameParts[0]);
                        }
                        if (!Objects.equals(nameParts[1], existingUser.getLastName())) {
                            existingUser.updateLastName(nameParts[1]);
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
                    // This means either:
                    // 1. It's a true passkey-first registration (nameFromWebAuthnEntity should be an email).
                    // 2. It's an "add passkey to existing user" flow, but the WebAuthn library
                    //    is using a *newly generated* externalId and `authentication.getName()` (e.g., GitHub ID)
                    //    as the `nameFromWebAuthnEntity`.

                    log.info("No existing user found with externalId: {}. Attempting to process based on WebAuthn entity name: '{}'",
                            externalIdFromWebAuthnEntity, nameFromWebAuthnEntity);

                    // Check if nameFromWebAuthnEntity is a valid email.
                    boolean isNameValidEmail;
                    try {
                        new EmailAddress(nameFromWebAuthnEntity);
                        isNameValidEmail = true;
                    } catch (IllegalArgumentException e) {
                        isNameValidEmail = false;
                    }

                    if (isNameValidEmail) {
                        // Scenario 1: Passkey-first registration, or nameFromWebAuthnEntity is a valid email.
                        // Attempt to find user by this email. If found, associate this externalId.
                        // If not found, create a new user.
                        log.info("nameFromWebAuthnEntity ('{}') is a valid email format. Checking if user exists with this email.", nameFromWebAuthnEntity);
                        return userRepository.findByEmail_Value(nameFromWebAuthnEntity)
                                .map(userByEmail -> {
                                    // User with this email exists. Update their externalId if it's different or null.
                                    // This links the WebAuthn-generated externalId to the existing email-based user.
                                    if (!externalIdFromWebAuthnEntity.equals(userByEmail.getExternalId())) {
                                        log.warn("User with email '{}' (ID: {}) found, but has different/null externalId ('{}'). " +
                                                        "Updating externalId to '{}' from WebAuthn flow.",
                                                nameFromWebAuthnEntity, userByEmail.getId(), userByEmail.getExternalId(), externalIdFromWebAuthnEntity);
                                        userByEmail.setExternalId(externalIdFromWebAuthnEntity); // Ensure User.setExternalId exists and is appropriate
                                    }
                                    // Update display name if necessary
                                    if (StringUtils.hasText(displayNameFromWebAuthnEntity) && !displayNameFromWebAuthnEntity.equals(userByEmail.getDisplayableFullName())) {
                                        String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                                        userByEmail.updateFirstName(nameParts[0]);
                                        userByEmail.updateLastName(nameParts[1]);
                                    }
                                    return userByEmail;
                                })
                                .orElseGet(() -> {
                                    // No user with this email. Create a new user (passkey-first).
                                    log.info("Creating new user with email: {} (from WebAuthn entity name) and externalId: {}", nameFromWebAuthnEntity, externalIdFromWebAuthnEntity);
                                    String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                                    String newFirstName;
                                    if (StringUtils.hasText(nameParts[0])) {
                                        newFirstName = nameParts[0];
                                    } else {
                                        int atIndex = nameFromWebAuthnEntity.indexOf('@');
                                        newFirstName = (atIndex != -1) ? nameFromWebAuthnEntity.substring(0, atIndex) : nameFromWebAuthnEntity;
                                    }
                                    String newLastName = nameParts[1];

                                    User newUser = User.builder()
                                            .firstName(newFirstName)
                                            .lastName(newLastName)
                                            .email(new EmailAddress(nameFromWebAuthnEntity))
                                            .externalId(externalIdFromWebAuthnEntity)
                                            .build();
                                    newUser.enableAccount();
                                    authorityRepository.findByAuthority("ROLE_USER").ifPresent(newUser::assignAuthority);
                                    return newUser;
                                });
                    } else {
                        // Scenario 2: nameFromWebAuthnEntity is NOT a valid email (e.g., GitHub ID "57825640").
                        // This is the problematic flow for "add passkey to existing OAuth2 user".
                        // We CANNOT create a new user with this name as email.
                        // The user *should* already exist and be identifiable by the *original* externalId
                        // that was passed into PasskeyServiceImpl.beginPasskeyRegistration.
                        // The fact that we are in orElseGet for findByExternalId(externalIdFromWebAuthnEntity)
                        // means the externalId used by WebAuthn library here is NOT the user's actual externalId.
                        log.error("Cannot create or find user: The 'name' field ('{}') from PublicKeyCredentialUserEntity is not a valid email. " +
                                        "This indicates an issue in the WebAuthn flow when adding a passkey to an existing OAuth2/OIDC user. " +
                                        "The WebAuthn library might be using authentication.getName() as the user.name and a new externalId. " +
                                        "The existing user (identified by their actual email/externalId) should have been used directly.",
                                nameFromWebAuthnEntity);
                        throw new IllegalStateException("Failed to process WebAuthn user entity: 'name' field ('" + nameFromWebAuthnEntity +
                                "') is not a valid email, and no existing user found with the WebAuthn-provided externalId '" +
                                externalIdFromWebAuthnEntity + "'. This often happens if the WebAuthn library attempts to create a new user " +
                                "identity using the OAuth2 subject ID as the username during passkey registration for an existing OAuth2 user.");
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

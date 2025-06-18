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
                                                        "Cannot update externalId as it's final. Creating a new user with the WebAuthn externalId.",
                                                nameFromWebAuthnEntity, userByEmail.getId(), userByEmail.getExternalId());

                                        // Create a new user with the same properties but with the new externalId
                                        User newUser = User.builder()
                                                .givenName(userByEmail.getGivenName())
                                                .middleName(userByEmail.getMiddleName())
                                                .surname(userByEmail.getSurname())
                                                .nickname(userByEmail.getNickname())
                                                .email(userByEmail.getEmail())
                                                .recoveryEmail(userByEmail.getRecoveryEmail())
                                                .mobileNumber(userByEmail.getMobileNumber())
                                                .accountStatus(userByEmail.getAccountStatus())
                                                .externalId(externalIdFromWebAuthnEntity)
                                                .build();

                                        // Copy authorities from the original user
                                        userByEmail.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .forEach(authorityName ->
                                                        authorityRepository.findByAuthority(authorityName)
                                                                .ifPresent(newUser::assignAuthority)
                                                );

                                        // Delete the old user
                                        userRepository.delete(userByEmail);

                                        // Return the new user
                                        return newUser;
                                    }
                                    // Update display name if necessary
                                    if (StringUtils.hasText(displayNameFromWebAuthnEntity) && !displayNameFromWebAuthnEntity.equals(userByEmail.getDisplayableFullName())) {
                                        String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                                        userByEmail.updateGivenName(nameParts[0]);
                                        userByEmail.updateMiddleName(nameParts[1]); // Middle name is optional
                                        userByEmail.updateSurname(nameParts[2]);
                                    }
                                    return userByEmail;
                                })
                                .orElseGet(() -> {
                                    // No user with this email. Create a new user (passkey-first).
                                    log.info("Creating new user with email: {} (from WebAuthn entity name) and externalId: {}", nameFromWebAuthnEntity, externalIdFromWebAuthnEntity);
                                    String[] nameParts = parseFullName(displayNameFromWebAuthnEntity);
                                    String newGivenName;
                                    if (StringUtils.hasText(nameParts[0])) {
                                        newGivenName = nameParts[0];
                                    } else {
                                        int atIndex = nameFromWebAuthnEntity.indexOf('@');
                                        newGivenName = (atIndex != -1) ? nameFromWebAuthnEntity.substring(0, atIndex) : nameFromWebAuthnEntity;
                                    }
                                    String newMiddleName = nameParts[1]; // Middle name is optional
                                    String newSurname = nameParts[2];

                                    User newUser = User.builder()
                                            .givenName(newGivenName)
                                            .middleName(newMiddleName) // Include optional middle name
                                            .surname(newSurname)
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

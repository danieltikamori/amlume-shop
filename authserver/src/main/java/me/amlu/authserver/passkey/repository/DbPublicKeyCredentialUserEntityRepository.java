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


@Component
public class DbPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DbPublicKeyCredentialUserEntityRepository.class);
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public DbPublicKeyCredentialUserEntityRepository(UserRepository userRepository, AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
    }

    private static PublicKeyCredentialUserEntity mapUserToPublicKeyCredentialUserEntity(User user) {
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
        log.info("save (WebAuthn UserEntity): webAuthnUsername(email)={}, webAuthnDisplayName(realName)={}, externalId={}",
                webAuthnUserEntity.getName(), webAuthnUserEntity.getDisplayName(), webAuthnUserEntity.getId().toBase64UrlString());

        String externalId = webAuthnUserEntity.getId().toBase64UrlString();
        String emailFromWebAuthn = webAuthnUserEntity.getName(); // This is the email
        String realNameStringFromWebAuthn = webAuthnUserEntity.getDisplayName(); // This is the full real name string

        User userToSave = userRepository.findByExternalId(externalId)
                .map(existingUser -> {
                    log.debug("Updating existing user for WebAuthn: externalId={}", externalId);

                    // Update User.firstName and User.lastName from realNameStringFromWebAuthn
                    if (StringUtils.hasText(realNameStringFromWebAuthn)) {
                        String[] nameParts = parseFullName(realNameStringFromWebAuthn);
                        if (!nameParts[0].equals(existingUser.getFirstName())) {
                            existingUser.updateFirstName(nameParts[0]);
                        }
                        if ((nameParts[1] == null && existingUser.getLastName() != null) ||
                                (nameParts[1] != null && !nameParts[1].equals(existingUser.getLastName()))) {
                            existingUser.updateLastName(nameParts[1]);
                        }
                    } else { // If WebAuthn display name is null/blank, clear existing names
                        if (existingUser.getFirstName() != null)
                            existingUser.updateFirstName(null); // This will fail due to Assert.hasText
                        if (existingUser.getLastName() != null) existingUser.updateLastName(null);
                        // Consider if clearing firstName should be allowed or if an error should be thrown.
                        // For now, if realNameStringFromWebAuthn is blank, we don't update firstName/lastName.
                        // If it must be cleared, User.updateFirstName needs to allow null/blank or a different method is needed.
                        // Let's assume if realNameStringFromWebAuthn is blank, we don't touch existing firstName/lastName.
                        // If it's explicitly null, and User.firstName allows null, then update.
                        // Given User.firstName is non-nullable, we cannot set it to null.
                        // So, if realNameStringFromWebAuthn is null/blank, we effectively don't update firstName.
                        // This might need refinement based on business rules for clearing names.
                        log.warn("WebAuthn display name is blank for existing user {}. First/Last name not updated.", externalId);

                    }


                    if (existingUser.getEmail() != null && !emailFromWebAuthn.equals(existingUser.getEmail().getValue())) {
                        log.warn("WebAuthn save attempt for externalId {} with a different email ({} vs {}). Email not updated via this flow.",
                                externalId, emailFromWebAuthn, existingUser.getEmail().getValue());
                    }

                    // Optional logging
                    log.debug("User object before save (update): id={}, externalId={}, firstName={}, lastName={}, email={}",
                            existingUser.getId(), existingUser.getExternalId(), existingUser.getFirstName(),
                            existingUser.getLastName(), existingUser.getEmail() != null ? existingUser.getEmail().getValue() : "null");

                    return existingUser;
                })
                .orElseGet(() -> {
                    // Create new user for Passkey-first registration
                    log.info("Creating new user via WebAuthn registration for externalId: {}", externalId);

                    if (!StringUtils.hasText(emailFromWebAuthn)) {
                        log.error("Cannot create new user via WebAuthn: email (from webAuthnUserEntity.getName()) is missing for externalId: {}", externalId);
                        throw new IllegalArgumentException("Email (from webAuthnUserEntity.getName()) is required to create a new user via WebAuthn.");
                    }

                    String[] nameParts = parseFullName(realNameStringFromWebAuthn);
                    String newFirstName = nameParts[0];
                    String newLastName = nameParts[1];

                    if (!StringUtils.hasText(newFirstName)) {
                        // If parsing realNameStringFromWebAuthn results in no first name, this is an issue.
                        // Fallback or error. For now, let's use email part if real name is entirely missing.
                        log.warn("Real name (from webAuthnUserEntity.getDisplayName()) is missing or unparsable for new user with externalId: {}. Using email as first name.", externalId);
                        newFirstName = emailFromWebAuthn.split("@")[0]; // Basic fallback
                    }

                    User newUser = User.builder()
                            .firstName(newFirstName)
                            .lastName(newLastName) // Can be null
                            .nickname(null) // Nickname is not provided by default WebAuthn flow
                            .email(new EmailAddress(emailFromWebAuthn))
                            .build();

                    newUser.setExternalId(externalId); // Set the WebAuthn user handle

                    authorityRepository.findByAuthority("ROLE_USER")
                            .ifPresentOrElse(
                                    newUser::assignAuthority,
                                    () -> log.warn("Default authority 'ROLE_USER' not found. New user {}/{} will have no roles.", newUser.getFirstName(), newUser.getEmail().getValue())
                            );
                    log.debug("Built new user for WebAuthn: firstName={}, lastName={}, email={}", newUser.getFirstName(), newUser.getLastName(), newUser.getEmail().getValue());

                    // Optional logging
                    log.debug("User object before save (new): externalId={}, firstName={}, lastName={}, email={}",
                            newUser.getExternalId(), newUser.getFirstName(), newUser.getLastName(),
                            newUser.getEmail() != null ? newUser.getEmail().getValue() : "null");

                    return newUser;
                });

        userRepository.save(userToSave);
        log.info("Saved user: id={}, externalId={}, firstName={}, lastName={}, email={}",
                userToSave.getId(), userToSave.getExternalId(), userToSave.getFirstName(), userToSave.getLastName(),
                userToSave.getEmail() != null ? userToSave.getEmail().getValue() : "null");
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

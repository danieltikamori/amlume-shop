/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.oauth2;

import me.amlu.shop.amlume_shop.user_management.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;
    private final UserService userService; // For updateLastLoginTime and potentially other user ops

    public CustomOidcUserService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Delegate to the standard OIDC user service to get the OidcUser
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        // 2. Extract necessary claims from the OIDC user attributes
        // The 'sub' claim is usually the primary identifier from the IdP.
        // For authserver, 'user_id_numeric' is the stable internal ID.
        Object authServerSubjectIdObject = attributes.get("user_id_numeric");
        if (authServerSubjectIdObject == null) {
            log.error("Claim 'user_id_numeric' not found for OIDC user: {}", oidcUser.getName());
            throw new OAuth2AuthenticationException("Missing 'user_id_numeric' claim.");
        }
        String authServerSubjectId = String.valueOf(authServerSubjectIdObject);
        String emailFromToken = oidcUser.getEmail();

        if (!StringUtils.hasText(emailFromToken)) {
            log.error("Email claim is missing or empty for user with authServerSubjectId: {}. Cannot provision.", authServerSubjectId);
            throw new OAuth2AuthenticationException("Email claim is required for provisioning user.");
        }

        String firstName = oidcUser.getGivenName(); // Standard OIDC claim
        String lastName = oidcUser.getFamilyName(); // Standard OIDC claim
        String nickname = oidcUser.getNickName(); // Standard OIDC claim (or preferred_username)
        // We also have 'full_name' claim, which could be used if given_name/family_name are not standardly populated by all IdPs
        // String fullNameFromToken = (String) attributes.get("full_name");

        // Extract roles - authserver puts roles like "USER", "ADMIN" into the 'roles' claim
        @SuppressWarnings("unchecked")
        Collection<String> rolesFromToken = (Collection<String>) attributes.getOrDefault("roles", new HashSet<String>());

        User shopUser;

        // 4. Try to find user by the definitive authServerSubjectId
        Optional<User> userBySubjectIdOptional = userRepository.findByAuthServerSubjectId(authServerSubjectId);

        if (userBySubjectIdOptional.isPresent()) {
            // User already linked to this authServerSubjectId. Update their details.
            shopUser = userBySubjectIdOptional.get();
            log.debug("Found existing local user '{}' by authServerSubjectId '{}'. Updating details.", shopUser.getUsername(), authServerSubjectId);
            shopUser = updateExistingShopUser(shopUser, emailFromToken, firstName, lastName, nickname, rolesFromToken);
        } else {
            // No user found for this authServerSubjectId. This is a new IdP login for amlume-shop.
            // Now, check if the email from the token is ALREADY IN USE by another local account.
            Optional<User> userByEmailOptional = userRepository.findByContactInfoUserEmailEmail(emailFromToken);

            if (userByEmailOptional.isPresent()) {
                User existingUserWithEmail = userByEmailOptional.get();
                // Email is in use.
                if (existingUserWithEmail.getAuthServerSubjectId() == null || existingUserWithEmail.getAuthServerSubjectId().isEmpty()) {
                    // Scenario: Email exists locally, but NOT linked to any authServerSubjectId.
                    // This local account might have been created before OAuth2, or by an admin.
                    // Action: Link this existing local account to the new authServerSubjectId.
                    log.info("Email {} (from token for new authSubId {}) found in local user (ID: {}) without an authSubId. Linking accounts.",
                            emailFromToken, authServerSubjectId, existingUserWithEmail.getUserId());
                    existingUserWithEmail.updateAuthServerSubjectId(authServerSubjectId); // Ensure User entity has this method
                    shopUser = updateExistingShopUser(existingUserWithEmail, emailFromToken, firstName, lastName, nickname, rolesFromToken); // Update other details
                } else {
                    // Scenario: Email exists locally AND IS ALREADY LINKED to a DIFFERENT authServerSubjectId.
                    // This is a more serious conflict. The email from the token is claimed by another IdP-linked user.
                    log.error("CRITICAL CONFLICT: Email {} (from token for new authSubId {}) is already linked to a different local user (ID: {}, existingAuthSubId: {}).",
                            emailFromToken, authServerSubjectId, existingUserWithEmail.getUserId(), existingUserWithEmail.getAuthServerSubjectId());
                    // Policy: Deny login. This is the safest approach, prevents one IdP user from "taking over" an email linked to another or unintended linking.
                    throw new OAuth2AuthenticationException(
                            String.format("Email '%s' is already associated with another authenticated account. Please contact support.", emailFromToken)
                    );
                }
            } else {
                // Email is not in use locally by any account. Provision a brand new user.
                log.info("User with authServerSubjectId '{}' (email: {}) not found locally. Provisioning new user.", authServerSubjectId, emailFromToken);
                shopUser = createNewShopUser(authServerSubjectId, emailFromToken, firstName, lastName, nickname, rolesFromToken);
            }
        }

        // 5. Update last login time for amlume-shop access
        userService.updateLastLoginTime(shopUser.getUserId());
        log.debug("Updated amlume-shop last access time for user: {}", shopUser.getUsername());

        // 6. Return a custom OidcUser implementation that wraps the amlume-shop User
        return new ShopOidcUser(shopUser, oidcUser);
    }

    private User createNewShopUser(String authServerSubjectId, String email, String firstName,
                                   String lastName, String nickname, Collection<String> rolesFromToken) {
        if (!StringUtils.hasText(email)) {
            log.error("Email claim is missing or empty for new user with authServerSubjectId: {}. Cannot provision.", authServerSubjectId);
            // Decide how to handle this: throw exception, or create user with a placeholder email?
            // Throwing an exception is safer to ensure data integrity.
            throw new OAuth2AuthenticationException("Email claim is required for provisioning new user.");
        }

        // Check if email already exists for a *different* authServerSubjectId (unlikely but good check)
        if (userRepository.existsByContactInfoUserEmailEmail(email)) {
            log.warn("Email {} already exists for a different local user. Potential conflict for authServerSubjectId {}. " +
                    "This might happen if a user re-registers with the IdP after deleting their local shop account, " +
                    "or if email changes are not synced properly.", email, authServerSubjectId);
            // Consider how to handle this:
            // 1. Throw an error: Prevents linking if email is already tied to another authServerSubjectId.
            // 2. Attempt to link: If business logic allows, but risky.
            // 3. Log and proceed: Relies on authServerSubjectId being the ultimate unique link.
            // For now, log and proceed, as findByAuthServerSubjectId is the primary lookup.
        }

        User.UserBuilder<?, ?> builder = User.builder()
                .authServerSubjectId(authServerSubjectId) // Link to authserver
                .contactInfo(ContactInfo.builder()
                        .userEmail(new UserEmail(email))
                        .firstName(firstName)
                        .lastName(lastName)
                        .nickname(nickname) // Assuming ContactInfo builder handles null nickname
                        .emailVerified(true) // Assume email from IdP is verified
                        // phoneNumber will be null by default from builder
                        .build())
                .accountStatus(AccountStatus.builder() // Initial account status
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true) // No local credentials for OAuth2 user
                        .enabled(true)
                        .creationTime(Instant.now())
                        .lastLoginTime(Instant.now()) // Set initial last login
                        .build())
                .deviceFingerprintingInfo(DeviceFingerprintingInfo.builder().deviceFingerprintingEnabled(false).build()) // Default
                .locationInfo(LocationInfo.builder().build()); // Default

        // If AuthenticationInfo is still part of User, set it appropriately (e.g., null password)
        // builder.authenticationInfo(AuthenticationInfo.builder().password(null).build());

        User newUser = builder.build();

        // --- Assign roles ---
        Set<UserRole> shopRoles = mapRoles(rolesFromToken);
        if (shopRoles.isEmpty()) { // Assign a default role if none are provided by token
            shopRoles.add(new UserRole(AppRole.ROLE_CUSTOMER)); // Or ROLE_USER
            log.debug("No roles from token for new user {}, assigning default ROLE_CUSTOMER.", email);
        }
        newUser.createRoleSet(shopRoles); // Use createRoleSet to replace any defaults from builder

        return userRepository.save(newUser);
    }

    private User updateExistingShopUser(User existingUser, String emailFromToken, String firstNameFromToken,
                                        String lastNameFromToken, String nicknameFromToken, Collection<String> rolesFromToken) {
        boolean overallUpdated = false;

        ContactInfo currentContactInfo = existingUser.getContactInfo();
        ContactInfo.ContactInfoBuilder contactInfoBuilder = ContactInfo.builder() // Start with a fresh builder
                .firstName(currentContactInfo.getFirstName())
                .lastName(currentContactInfo.getLastName())
                .nickname(currentContactInfo.getNickname())
                .userEmail(currentContactInfo.getUserEmailObject())
                .emailVerified(currentContactInfo.isEmailVerified())
                .phoneNumber(currentContactInfo.getPhoneNumber());


        // --- Email Update Logic ---
        if (StringUtils.hasText(emailFromToken) && !emailFromToken.equals(currentContactInfo.getEmail())) {
            // Email from token is different. Check if this new email is already used by ANOTHER user.
            Optional<User> otherUserWithNewEmail = userRepository.findByContactInfoUserEmailEmail(emailFromToken);
            if (otherUserWithNewEmail.isPresent() && !Objects.equals(otherUserWithNewEmail.get().getAuthServerSubjectId(), existingUser.getAuthServerSubjectId())) {
                // The new email is taken by a different user. Log and DO NOT update email.
                log.warn("User (authSubId: {}) attempted to update email to '{}', but it's already in use by another user (authSubId: {}). Email update skipped.",
                        existingUser.getAuthServerSubjectId(), emailFromToken, otherUserWithNewEmail.get().getAuthServerSubjectId());
            } else {
                // New email is not taken by another user, or it's the same user (e.g., case change)
                contactInfoBuilder.userEmail(new UserEmail(emailFromToken));
                contactInfoBuilder.emailVerified(true); // Email from IdP is considered verified
                overallUpdated = true;
            }
        }
        // --- End Email Update Logic ---

        if (StringUtils.hasText(firstNameFromToken) && !Objects.equals(firstNameFromToken, currentContactInfo.getFirstName())) {
            contactInfoBuilder.firstName(firstNameFromToken);
            overallUpdated = true;
        }
        if (lastNameFromToken != null && !Objects.equals(lastNameFromToken, currentContactInfo.getLastName())) {
            contactInfoBuilder.lastName(lastNameFromToken);
            overallUpdated = true;
        }
        if (nicknameFromToken != null && !Objects.equals(nicknameFromToken, currentContactInfo.getNickname())) {
            contactInfoBuilder.nickname(nicknameFromToken);
            overallUpdated = true;
        }

        if (overallUpdated) { // Only update if any of the above fields actually changed the builder
            existingUser.updateContactInfo(contactInfoBuilder.build());
        }

        // --- Update Roles --- (logic remains similar)
        Set<UserRole> newShopRoles = mapRoles(rolesFromToken);
        Set<UserRole> currentShopRoles = existingUser.getRoles() != null ? new HashSet<>(existingUser.getRoles()) : new HashSet<>();
        if (!newShopRoles.isEmpty() && !currentShopRoles.equals(newShopRoles)) {
            existingUser.createRoleSet(newShopRoles);
            overallUpdated = true;
        }

        if (overallUpdated) {
            return userRepository.save(existingUser);
        }
        return existingUser;
    }

    private Set<UserRole> mapRoles(Collection<String> roleStringsFromToken) {
        if (roleStringsFromToken == null || roleStringsFromToken.isEmpty()) {
            return new HashSet<>();
        }
        return roleStringsFromToken.stream()
                .map(roleStringFromToken -> {
                    try {
                        // authserver token has roles like "ADMIN", "USER" (no "ROLE_" prefix)
                        // amlume-shop AppRole enum has "ROLE_ADMIN", "ROLE_USER"
                        return AppRole.valueOf("ROLE_" + roleStringFromToken.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown role string '{}' from token. Ignoring.", roleStringFromToken);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(UserRole::new) // Create amlume-shop UserRole
                .collect(Collectors.toSet());
    }
}

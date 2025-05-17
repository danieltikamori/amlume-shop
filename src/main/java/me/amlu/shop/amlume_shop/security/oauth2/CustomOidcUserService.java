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

import com.google.i18n.phonenumbers.Phonenumber;
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
            log.error("Claim 'user_id_numeric' (authserver subject ID) not found in OIDC token attributes for user: {}", oidcUser.getName());
            throw new OAuth2AuthenticationException("Missing 'user_id_numeric' claim in OIDC token.");
        }
        String authServerSubjectId = String.valueOf(authServerSubjectIdObject);

        String email = oidcUser.getEmail(); // Standard OIDC claim (also in the custom claims)
        String firstName = oidcUser.getGivenName(); // Standard OIDC claim
        String lastName = oidcUser.getFamilyName(); // Standard OIDC claim
        String nickname = oidcUser.getNickName(); // Standard OIDC claim (or preferred_username)
        // You also have 'full_name' claim, which could be used if given_name/family_name are not standardly populated by all IdPs
        // String fullNameFromToken = (String) attributes.get("full_name");

        // Extract roles - authserver puts roles like "USER", "ADMIN" into the 'roles' claim
        @SuppressWarnings("unchecked")
        Collection<String> rolesFromToken = (Collection<String>) attributes.getOrDefault("roles", new HashSet<String>());

        // 3. Find or create the local amlume-shop User
        Optional<User> userOptional = userRepository.findByAuthServerSubjectId(authServerSubjectId);
        User shopUser;

        if (userOptional.isEmpty()) {
            // User does not exist locally, provision a new one
            log.info("User with authServerSubjectId '{}' not found locally. Provisioning new user.", authServerSubjectId);
            shopUser = createNewShopUser(authServerSubjectId, email, firstName, lastName, nickname, rolesFromToken);
        } else {
            // User exists, update their details if necessary
            shopUser = userOptional.get();
            log.debug("Found existing local user '{}' for authServerSubjectId '{}'. Updating details.", shopUser.getUsername(), authServerSubjectId);
            shopUser = updateExistingShopUser(shopUser, email, firstName, lastName, nickname, rolesFromToken);
        }

        // 4. Update last login time for amlume-shop access
        userService.updateLastLoginTime(shopUser.getUserId());
        log.debug("Updated amlume-shop last access time for user: {}", shopUser.getUsername());

        // 5. Return a custom OidcUser implementation that wraps the amlume-shop User
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

        // --- Update contact info --- if changed (assuming authserver is the source of truth for these)
        ContactInfo currentContactInfo = existingUser.getContactInfo();
        // Initialize with current values
        String effectiveEmail = (currentContactInfo != null && currentContactInfo.getUserEmailObject() != null) ? currentContactInfo.getUserEmailObject().getEmail() : null;
        String effectiveFirstName = (currentContactInfo != null) ? currentContactInfo.getFirstName() : null;
        String effectiveLastName = (currentContactInfo != null) ? currentContactInfo.getLastName() : null;
        String effectiveNickname = (currentContactInfo != null) ? currentContactInfo.getNickname() : null;
        boolean effectiveEmailVerified = (currentContactInfo != null) && currentContactInfo.isEmailVerified();
        Phonenumber.PhoneNumber effectivePhoneNumber = (currentContactInfo != null) ? currentContactInfo.getPhoneNumber() : null;

        boolean contactInfoChanged = false;

        if (StringUtils.hasText(emailFromToken) && !Objects.equals(emailFromToken, effectiveEmail)) {
            effectiveEmail = emailFromToken;
            contactInfoChanged = true;
            effectiveEmailVerified = true; // Email from IdP is considered verified
        }
        if (StringUtils.hasText(firstNameFromToken) && !Objects.equals(firstNameFromToken, effectiveFirstName)) {
            effectiveFirstName = firstNameFromToken;
            contactInfoChanged = true;
        }
        // For lastName and nickname, an empty string from token might mean "clear", null means "no change"
        if (lastNameFromToken != null && !Objects.equals(lastNameFromToken, effectiveLastName)) {
            effectiveLastName = lastNameFromToken; // Allow setting to "" or a value
            contactInfoChanged = true;
        }
        if (nicknameFromToken != null && !Objects.equals(nicknameFromToken, effectiveNickname)) {
            effectiveNickname = nicknameFromToken;
            contactInfoChanged = true;
        }

        if (contactInfoChanged) {
            ContactInfo updatedContactInfo = ContactInfo.builder()
                    .userEmail(effectiveEmail != null ? new UserEmail(effectiveEmail) : null)
                    .firstName(effectiveFirstName)
                    .lastName(effectiveLastName)
                    .nickname(effectiveNickname)
                    .emailVerified(effectiveEmailVerified) // Use the (potentially updated) verification status
                    .phoneNumber(effectivePhoneNumber) // Preserve existing phone number
                    .build();
            existingUser.updateContactInfo(updatedContactInfo);
            overallUpdated = true;
        }

        // --- Update Roles ---
        Set<UserRole> newShopRoles = mapRoles(rolesFromToken);
        Set<UserRole> currentShopRoles = existingUser.getRoles() != null ? new HashSet<>(existingUser.getRoles()) : new HashSet<>();

        // Only update roles if they have actually changed and new roles are not empty
        // (to avoid removing all roles if token has no roles but user had local ones)
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

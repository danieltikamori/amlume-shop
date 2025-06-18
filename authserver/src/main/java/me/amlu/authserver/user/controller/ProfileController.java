/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.amlu.authserver.user.dto.ChangePasswordRequest;
import me.amlu.authserver.user.dto.GetUserProfileResponse;
import me.amlu.authserver.user.dto.UpdateUserProfileRequest;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.service.UserLookupService;
import me.amlu.authserver.user.service.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/profile") // Consistent base path
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final UserManager userManager;
    private final HttpServletResponse response;
    private final UserLookupService userLookupService;

    public ProfileController(UserManager userManager, HttpServletResponse response, UserLookupService userLookupService) {
        this.userManager = userManager;
        this.response = response;
        this.userLookupService = userLookupService;
    }

    @GetMapping
    public ResponseEntity<GetUserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication); // Resolve
        if (currentUser == null) {
            log.warn("Attempt to get profile without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("User {} fetching their profile.", currentUser.getEmail().getValue());

        GetUserProfileResponse responseDto = new GetUserProfileResponse(
                currentUser.getId(),
                currentUser.getExternalId(),
                currentUser.getGivenName(),
                currentUser.getMiddleName(),
                currentUser.getSurname(),
                currentUser.getNickname(),
                currentUser.getEmail() != null ? currentUser.getEmail().getValue() : null,
                currentUser.getRecoveryEmail() != null ? currentUser.getBackupEmail().getValue() : null,
                currentUser.getMobileNumber() != null ? currentUser.getMobileNumber().e164Value() : null
        );
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping // Maps to PUT /api/profile
    public ResponseEntity<GetUserProfileResponse> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication); // Resolve
        if (currentUser == null) {
            log.warn("Attempt to update profile without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("User {} attempting to update profile. Request: {}", currentUser.getEmail().getValue(), request);

        try {
            // The 'defaultRegion' for phone number parsing is not in UpdateUserProfileRequest.
            // UserManager.updateUserProfile will pass null for defaultRegion to PhoneNumber.ofNullable,
            // which is acceptable if numbers are expected in E.164 or libphonenumber can infer.
            User updatedUser = userManager.updateUserProfile(
                    currentUser.getId(),
                    request.givenName(),
                    request.middleName(),
                    request.surname(),
                    request.nickname(),
                    request.mobileNumber(),
                    request.defaultRegion(),
                    request.recoveryEmail()
            );

            GetUserProfileResponse responseDto = new GetUserProfileResponse(
                    updatedUser.getId(),
                    updatedUser.getExternalId(),
                    updatedUser.getGivenName(),
                    updatedUser.getMiddleName(),
                    updatedUser.getSurname(),
                    updatedUser.getNickname(),
                    updatedUser.getEmail() != null ? updatedUser.getEmail().getValue() : null,
                    updatedUser.getRecoveryEmail() != null ? updatedUser.getRecoveryEmail().getValue() : null,
                    updatedUser.getMobileNumber() != null ? updatedUser.getMobileNumber().e164Value() : null
            );
            log.info("User {} profile updated successfully.", currentUser.getEmail().getValue());
            return ResponseEntity.ok(responseDto);

        } catch (EntityNotFoundException e) {
            // This case should be rare if currentUser is valid, but good to handle.
            log.error("User not found during profile update for authenticated user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.", e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during profile update for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (DataIntegrityViolationException e) { // Catch potential backup email conflict
            log.warn("Data integrity violation during profile update for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error updating profile for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not update profile.", e);
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("#userId == authentication.principal.id or hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication); // Resolve
        if (currentUser == null) {
            log.warn("Attempt to change password without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("User {} attempting to change password.", currentUser.getEmail().getValue());
        // Do NOT log the password itself

        try {
            userManager.changeUserPassword(currentUser.getId(), request.oldPassword(), request.password());
            log.info("User {} password changed successfully.", currentUser.getEmail().getValue());
            return ResponseEntity.ok().build(); // Or ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.error("User not found during password change for authenticated user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.", e);
        } catch (IllegalArgumentException e) { // e.g., if password validation fails within service
            log.warn("Invalid argument during password change for user {}: {}", currentUser.getEmail().getValue(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error changing password for user {}: {}", currentUser.getEmail().getValue(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not change password.", e);
        }
    }

    @DeleteMapping // Maps to DELETE /api/profile
    public ResponseEntity<Void> deleteCurrentUserAccount(
            Authentication authentication,
            HttpServletRequest request) {
        User currentUser = userLookupService.getAppUserFromAuthentication(authentication); // Resolve
        if (currentUser == null) {
            log.warn("Attempt to delete account without resolvable authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Capture details before potential session invalidation or user object becoming detached
        Long userId = currentUser.getId();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail().getValue() : "[unknown_email]";

        log.info("User {} (ID: {}) requesting deletion of their own account.", userEmail, userId);

        try {
            userManager.deleteUserAccount(userId);
            log.info("User {} (ID: {}) account data deleted successfully from backend.", userEmail, userId);

            // Explicitly log out the user from the current HTTP session
            try {
                request.logout();
                log.info("User {} (ID: {}) successfully logged out from current HTTP session after account deletion.", userEmail, userId);
                // SecurityContextLogoutHandler is usually part of Spring Security's LogoutFilter
                // Calling it directly might be redundant if LogoutFilter is already configured and active.
                // However, if you want to be absolutely sure:
                Authentication authInContext = SecurityContextHolder.getContext().getAuthentication();
                if (authInContext != null) { // Check if authentication still exists
                    new SecurityContextLogoutHandler().logout(request, response, authInContext);
                    log.info("User {} (ID: {}) explicitly logged out via SecurityContextLogoutHandler.", userEmail, userId);
                }
            } catch (ServletException e) {
                log.error("Error during HTTP session logout for user {} (ID: {}) after account deletion: {}", userEmail, userId, e.getMessage(), e);
                // The account is deleted, but session logout failed.
                // Subsequent requests will fail authentication anyway.
                // Proceed to return success for the deletion.
            }

            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            log.error("User not found during account deletion for authenticated user {} (ID: {}): {}", userEmail, userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found for deletion.", e);
        } catch (Exception e) {
            log.error("Unexpected error deleting account for user {} (ID: {}): {}", userEmail, userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete account.", e);
        }
    }
}

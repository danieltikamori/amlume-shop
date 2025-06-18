/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.service;

import io.micrometer.core.annotation.Timed;
import me.amlu.authserver.user.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public interface UserServiceInterface {
    // Method called by authentication failure handler
    void handleFailedLogin(String username);

    // Method called by authentication success handler
    void handleSuccessfulLogin(String username);

    User createUser(String givenName, String middleName, String surname, String nickname, String email, String rawPassword, String mobileNumber, String defaultRegion, String recoveryEmail);

    User updateUserProfile(Long userId, String newGivenName, String newMiddleName, String newSurname, String newNickname, String newMobileNumber, String defaultRegion, String newRecoveryEmail);

    void changeUserPassword(Long userId, String oldRawPassword, String newRawPassword);

    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')") // Or based on who can change password
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    void adminChangeUserPassword(Long userId, String newRawPassword);

    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    @Timed(value = "authserver.usermanager.changepassword", description = "Time taken to admin change user password")
    void adminChangeUserPasswordByUsername(String username, String newRawPassword);

    // Method to be called by an admin to manually unlock an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminUnlockUser(Long userId);

    // Method to be called by an admin to manually disable/enable an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminSetUserEnabled(Long userId, boolean enabled);

    @Transactional
        // This method should be transactional
        // Consider if @PreAuthorize is needed here, e.g., only admin or the user themselves.
        // The controller endpoint already checks for @AuthenticationPrincipal.
    void deleteUserAccount(Long userId);
}

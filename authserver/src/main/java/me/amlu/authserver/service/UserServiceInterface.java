/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import me.amlu.authserver.model.User;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserServiceInterface {
    // Method called by authentication failure handler
    void handleFailedLogin(String username);

    // Method called by authentication success handler
    void handleSuccessfulLogin(String username);

    User createUser(String firstName, String lastName, String nickname, String email, String rawPassword, String mobileNumber, String defaultRegion);

    User updateUserProfile(Long userId, String newFirstName, String newLastName, String newNickname, String newMobileNumber, String defaultRegion);

    void changeUserPassword(Long userId, String newRawPassword);

    // Method to be called by an admin to manually unlock an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminUnlockUser(Long userId);

    // Method to be called by an admin to manually disable/enable an account
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_ROOT')")
    void adminSetUserEnabled(Long userId, boolean enabled);
}

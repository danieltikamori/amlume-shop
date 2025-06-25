/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import jakarta.validation.constraints.NotBlank;
import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.user.model.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@NullMarked
public interface RoleService {
    /**
     * Retrieves dynamic roles for a given resource
     *
     * @param resource The resource to check roles for
     * @return Set of role names including the "ROLE_" prefix
     */
    Set<Role> getDynamicRolesForResource(Object resource);

    void clearAllRoles();

    void clearUserRoles(@NotBlank String username, @NonNull Object resource);

    @Transactional
        // Ensure atomicity
    boolean assignRoles(@NonNull User user, @NonNull Set<Role> newRoles);

//    boolean canManageProduct@NonNull User user, @NotNull Product product);
}


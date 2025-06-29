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

import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.security.audit.SecurityAuditService;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import me.amlu.authserver.security.config.RoleHierarchyValidator;
import me.amlu.authserver.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleHierarchyValidator roleHierarchyValidator;
    private final SecurityAuditService securityAuditService;

    public RoleServiceImpl(UserRepository userRepository,
                           RoleHierarchyValidator roleHierarchyValidator,
                           SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.roleHierarchyValidator = roleHierarchyValidator;
        this.securityAuditService = securityAuditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> getDynamicRolesForResource(@NonNull Object resource) {
        Assert.notNull(resource, "Resource cannot be null for role determination.");

        String assignerId = resource.getClass().getName();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }

        String userIdentifier = authentication.getName();
        User user = userRepository.findByExternalIdAndDeletedAtIsNull(userIdentifier)
                .orElseThrow(() -> new UserNotFoundException("User not found with external ID: " + userIdentifier));

        Set<Role> dynamicRoles = new HashSet<>();
        // Add logic here to determine roles based on the resource if needed
        // For now, it can return an empty set or existing roles.

        log.debug("Determined roles for user '{}' on resource '{}': {}", userIdentifier, resource.getClass().getSimpleName(), dynamicRoles);
        securityAuditService.logRoleAssignment(userIdentifier, Collections.singleton(dynamicRoles), assignerId);
        return Collections.unmodifiableSet(dynamicRoles);
    }

    @Override
    public void clearAllRoles() {
        // Implementation for clearing caches if needed
        log.info("Clearing all role-related caches.");
    }

    @Override
    public void clearUserRoles(String authServerSubjectId, @NonNull Object resource) {
        // Implementation for clearing a specific user's cache
        log.info("Clearing role cache for user: {}", authServerSubjectId);
    }

    @Override
    public void assignRoles(@NonNull User user, @NonNull Set<Role> newRoles) {
        Assert.notNull(user, "User cannot be null for role assignment.");
        Assert.notNull(newRoles, "New roles cannot be null for role assignment.");
        Assert.notEmpty(newRoles, "New roles cannot be empty for role assignment.");
        // TODO: finish method implementation
    }
}

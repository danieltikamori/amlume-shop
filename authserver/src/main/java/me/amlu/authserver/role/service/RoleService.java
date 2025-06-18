/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.role.service;

import me.amlu.authserver.role.model.Role;
import me.amlu.authserver.role.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing roles with hierarchical structure.
 */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Find a role by its name.
     *
     * @param name The role name
     * @return Optional containing the role if found
     */
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Find all roles that are descendants of the given role.
     *
     * @param roleName The ancestor role name
     * @return List of descendant roles
     */
    public List<Role> findDescendants(String roleName) {
        Optional<Role> roleOpt = findByName(roleName);
        if (roleOpt.isPresent()) {
            return roleRepository.findDescendants(roleOpt.get().getPath());
        }
        return List.of();
    }

    /**
     * Find all roles that are ancestors of the given role.
     *
     * @param roleName The descendant role name
     * @return List of ancestor roles
     */
    public List<Role> findAncestors(String roleName) {
        Optional<Role> roleOpt = findByName(roleName);
        if (roleOpt.isPresent()) {
            return roleRepository.findAncestors(roleOpt.get().getPath());
        }
        return List.of();
    }

    /**
     * Create a new role.
     *
     * @param role The role to create
     * @return The created role
     */
    @Transactional
    public Role createRole(Role role) {
        // If parent is specified, update the path
        if (role.getParent() != null) {
            Optional<Role> parentOpt = roleRepository.findById(role.getParent().getId());
            if (parentOpt.isPresent()) {
                Role parent = parentOpt.get();
                role.setPath(parent.getPath() + "." + role.getName().toLowerCase().replace(" ", "_"));
            } else {
                role.setPath(role.getName().toLowerCase().replace(" ", "_"));
            }
        } else {
            role.setPath(role.getName().toLowerCase().replace(" ", "_"));
        }

        return roleRepository.save(role);
    }

    /**
     * Add a child role to a parent role.
     *
     * @param parentName The parent role name
     * @param childName  The child role name
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean addChildRole(String parentName, String childName) {
        Optional<Role> parentOpt = findByName(parentName);
        Optional<Role> childOpt = findByName(childName);

        if (parentOpt.isPresent() && childOpt.isPresent()) {
            Role parent = parentOpt.get();
            Role child = childOpt.get();

            parent.addChild(child);
            roleRepository.save(parent);
            return true;
        }

        return false;
    }

    /**
     * Check if a role is a descendant of another role.
     *
     * @param ancestorName   The ancestor role name
     * @param descendantName The descendant role name
     * @return true if descendant, false otherwise
     */
    public boolean isDescendant(String ancestorName, String descendantName) {
        Optional<Role> ancestorOpt = findByName(ancestorName);
        Optional<Role> descendantOpt = findByName(descendantName);

        if (ancestorOpt.isPresent() && descendantOpt.isPresent()) {
            Role ancestor = ancestorOpt.get();
            Role descendant = descendantOpt.get();

            // Check if descendant's path starts with ancestor's path
            return descendant.getPath().startsWith(ancestor.getPath() + ".");
        }

        return false;
    }
}

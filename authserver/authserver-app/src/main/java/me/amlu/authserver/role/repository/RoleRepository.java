/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.role.repository;

import me.amlu.authserver.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Role entities with hierarchical query support using PostgreSQL LTREE.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    /**
     * Find all roles that are descendants of the given path.
     * Uses PostgreSQL LTREE <@ operator (is descendant of).
     *
     * @param path The ancestor path
     * @return List of descendant roles
     */
    @Query(value = "SELECT * FROM roles WHERE path <@ :path", nativeQuery = true)
    List<Role> findDescendants(@Param("path") String path);

    /**
     * Find all roles that are ancestors of the given path.
     * Uses PostgreSQL LTREE @> operator (is ancestor of).
     *
     * @param path The descendant path
     * @return List of ancestor roles
     */
    @Query(value = "SELECT * FROM roles WHERE path @> :path", nativeQuery = true)
    List<Role> findAncestors(@Param("path") String path);

    /**
     * Find all roles at the specified level in the hierarchy.
     *
     * @param level The hierarchy level (0 = root)
     * @return List of roles at the specified level
     */
    @Query(value = "SELECT * FROM roles WHERE nlevel(path) = :level + 1", nativeQuery = true)
    List<Role> findByLevel(@Param("level") int level);

    /**
     * Find all roles matching the given path pattern.
     * Uses PostgreSQL LTREE ~ operator (matches LQUERY).
     *
     * @param pattern The LQUERY pattern (e.g., "*.admin.*")
     * @return List of matching roles
     */
    @Query(value = "SELECT * FROM roles WHERE path ~ :pattern", nativeQuery = true)
    List<Role> findByPattern(@Param("pattern") String pattern);
}

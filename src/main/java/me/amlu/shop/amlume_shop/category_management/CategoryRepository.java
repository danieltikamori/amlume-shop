/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by its name (case-insensitive).
     * Traverses the embedded CategoryName object.
     *
     * @param name The category name string to search for.
     * @return An Optional containing the category if found.
     */
    // Corrected method name for a derived query based on embedded VO field
    Optional<Category> findByCategoryName_NameIgnoreCase(String name);
    // Removed original findByCategoryName(String categoryName)

    /**
     * Finds all categories whose hierarchy path starts with the given pattern.
     * Useful for finding all descendants.
     *
     * @param pathPattern The starting path pattern (e.g., "1.2.").
     * @return A list of descendant categories (including the one matching the pattern if it exists).
     */
    @Query("SELECT c FROM Category c WHERE c.hierarchyLevel.path LIKE :pathPattern%")
    List<Category> findByHierarchyLevelPathStartingWith(@Param("pathPattern") String pathPattern);

    /**
     * Finds all categories at a specific hierarchy level (depth).
     *
     * @param level The hierarchy level (0 for root).
     * @return A list of categories at the specified level.
     */
    @Query("SELECT c FROM Category c WHERE c.hierarchyLevel.level = :level")
    List<Category> findByLevel(@Param("level") int level);
}

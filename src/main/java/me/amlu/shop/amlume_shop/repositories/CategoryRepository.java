/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.repositories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.amlu.shop.amlume_shop.category_management.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCategoryName(@NotBlank @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters") String categoryName);

    @Query("SELECT c FROM Category c WHERE c.hierarchyLevel.path LIKE :pathPattern%")
    List<Category> findByHierarchyLevelPathStartingWith(@Param("pathPattern") String pathPattern);

    @Query("SELECT c FROM Category c WHERE c.hierarchyLevel.level = :level")
    List<Category> findByLevel(@Param("level") int level);
}

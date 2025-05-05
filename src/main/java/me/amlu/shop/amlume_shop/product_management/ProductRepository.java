/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.product_management;

import me.amlu.shop.amlume_shop.category_management.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Checks if a product exists with the given name (case-insensitive),
     * traversing the embedded ProductName object.
     *
     * @param name The product name to check.
     * @return true if a product with the name exists, false otherwise.
     */
    boolean existsByProductName_NameIgnoreCase(String name);

    /**
     * Finds products whose names contain the given keyword (case-insensitive),
     * traversing the embedded ProductName object.
     *
     * @param keyword  The keyword to search for within product names.
     * @param pageable Pagination and sorting information.
     * @return A page of matching products.
     */
    Page<Product> findByProductName_NameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * Finds products belonging to the specified category.
     *
     * @param category The category to search within.
     * @param pageable Pagination and sorting information (renamed from pageDetails for convention).
     * @return A page of products in the given category.
     */
    Page<Product> findByCategory(Category category, Pageable pageable); // Renamed parameter for convention

    // Removed updatePrice method.
    // Updates involving Value Objects (like Money)
    // should be handled in the service layer by fetching the entity,
    // creating and setting the new Value Object instance, and saving the entity.
    // This ensures validation and lifecycle hooks are correctly applied.
    // void updatePrice(String productId, BigDecimal newPrice);
//    Page<Product> findByCategoryOrderByProductPriceAsc(Category category, Pageable pageDetails);
}

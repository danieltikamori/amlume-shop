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

import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    //    @Query(value = "SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM products p WHERE UPPER(p.product_name) = UPPER(:name)", nativeQuery = true)
//@Query(value = "SELECT COUNT(*) > 0 FROM products WHERE UPPER(product_name) = UPPER(:name)", nativeQuery = true)
    boolean existsByProductNameIgnoreCase(@Param("name") String name);

    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByCategory(Category category, Pageable pageDetails);

//    Page<Product> findByCategoryOrderByProductPriceAsc(Category category, Pageable pageDetails);
}

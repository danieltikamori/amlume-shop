/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlume.shop.amlume_shop.controller;

import me.amlume.shop.amlume_shop.model.Category;
import me.amlume.shop.amlume_shop.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {
    private final CategoryService categoryService;


    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/public/categories")
    public CopyOnWriteArrayList<Category> getAllCategories() {
        return  categoryService.getAllCategories();
    }

    @PostMapping("/public/categories")
    public String CreateCategory(@RequestBody Category category) {
        categoryService.createCategory(category);
        return "Category created successfully";
    }

    @DeleteMapping("/admin/categories/{category_id}")
    public String deleteCategory(@PathVariable Long category_id) {
        String status = categoryService.deleteCategory(category_id);
        categoryService.deleteCategory(category_id);
        return status;
    }

}

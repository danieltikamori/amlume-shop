/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.payload.CategoryDTO;
import me.amlu.shop.amlume_shop.payload.CategoryResponse;
import me.amlu.shop.amlume_shop.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
public class CategoryController {
    private final CategoryService categoryService;


    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("v1/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories() {
        CategoryResponse categoryResponse = categoryService.getAllCategories();

        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    @PostMapping("v1/public/categories")
    public ResponseEntity<CategoryDTO> CreateCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);

        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("v1/admin/categories/{category_id}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long category_id) {
        CategoryDTO deletedCategory = categoryService.deleteCategory(category_id);

        return ResponseEntity.status(HttpStatus.OK).body(deletedCategory);
    }

    @PutMapping("v1/admin/categories/{category_id}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @PathVariable Long category_id, @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.updateCategory(category_id, categoryDTO);

        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.OK);
    }

}

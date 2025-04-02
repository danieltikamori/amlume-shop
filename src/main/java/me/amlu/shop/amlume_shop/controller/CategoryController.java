/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.payload.CategoryDTO;
import me.amlu.shop.amlume_shop.payload.CategoryResponse;
import me.amlu.shop.amlume_shop.category_management.CategoryService;
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
    public ResponseEntity<CategoryResponse> getAllCategories(@RequestParam (name = "pageNumber", defaultValue = Constants.PAGE_NUMBER, required = false) int pageNumber,
                                                             @RequestParam(name = "pageSize", defaultValue = Constants.PAGE_SIZE, required = false) int pageSize,
                                                             @RequestParam(name = "sortBy", defaultValue = Constants.SORT_CATEGORIES_BY, required = false) String sortBy,
                                                             @RequestParam(name = "sortDir", defaultValue = Constants.SORT_DIR, required = false) String sortDir
                                                             ) {
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber, pageSize, sortBy, sortDir);

        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    @PostMapping("v1/public/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);

        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("v1/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId) {
        CategoryDTO deletedCategory = categoryService.deleteCategory(categoryId);

        return ResponseEntity.status(HttpStatus.OK).body(deletedCategory);
    }

    @PutMapping("v1/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @PathVariable Long categoryId, @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.updateCategory(categoryId, categoryDTO);

        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.OK);
    }

}

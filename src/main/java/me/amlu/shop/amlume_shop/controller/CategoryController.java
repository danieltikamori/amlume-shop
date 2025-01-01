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
import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/")
public class CategoryController {
    private final CategoryService categoryService;


    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    //    @RequestMapping("/public/categories/{category_id}", method = RequestMethod.GET) // Alternative way to GetMapping
    @GetMapping("v1/public/categories")
    public ResponseEntity<CopyOnWriteArrayList<Category>> getAllCategories() {
        CopyOnWriteArrayList<Category> categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @PostMapping("v1/public/categories")
    public ResponseEntity<String> CreateCategory(@Valid @RequestBody Category category) {
        categoryService.createCategory(category);
        return new ResponseEntity<String>("Category created successfully", HttpStatus.CREATED);
    }

    @DeleteMapping("v1/admin/categories/{category_id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long category_id) {

        String status = categoryService.deleteCategory(category_id);

        return ResponseEntity.status(HttpStatus.OK).body(status);
    }

    @PutMapping("v1/admin/categories/{category_id}")
    public ResponseEntity<String> updateCategory(@Valid @PathVariable Long category_id, @RequestBody Category category) {

        Category savedCategory = categoryService.updateCategory(category_id, category);
        return new ResponseEntity<>("Category with ID " + category_id + " updated successfully", HttpStatus.OK);


    }

}

/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlume.shop.amlume_shop.controller;

import me.amlume.shop.amlume_shop.model.Category;
import me.amlume.shop.amlume_shop.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {
    private final CategoryService categoryService;


    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

//    @RequestMapping("/public/categories/{category_id}", method = RequestMethod.GET) // Alternative way to GetMapping
    @GetMapping("/public/categories")
    public ResponseEntity<CopyOnWriteArrayList<Category>> getAllCategories() {
        CopyOnWriteArrayList<Category> categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    public ResponseEntity<String> CreateCategory(@RequestBody Category category) {
        categoryService.createCategory(category);
        return new ResponseEntity<String>("Category created successfully", HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{category_id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long category_id) {
        try {
            String status = categoryService.deleteCategory(category_id);

//            return new ResponseEntity<>(status, HttpStatus.OK); //Most common
//            return ResponseEntity.ok(status);
            return ResponseEntity.status(HttpStatus.OK).body(status);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }

    @PutMapping("/admin/categories/{category_id}")
    public ResponseEntity<String> updateCategory(@PathVariable Long category_id, @RequestBody Category category) {
        try {
            Category savedCategory = categoryService.updateCategory(category_id, category);
//            String status = categoryService.updateCategory(category_id, category);
            return new ResponseEntity<>("Category with ID " + category_id + " updated successfully", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        }
    }

}

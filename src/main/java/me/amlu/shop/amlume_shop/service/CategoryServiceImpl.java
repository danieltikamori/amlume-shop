/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.repositories.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>();
    private Long nextId = 1L;

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CopyOnWriteArrayList<Category> getAllCategories() {
        return new CopyOnWriteArrayList<>(categoryRepository.findAll());
    }

    @Override
    public void createCategory(Category category) {
        category.setCategory_id(nextId++);
        categoryRepository.save(category);
    }

    @Override
    public String deleteCategory(Long category_id) {
        CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>(categoryRepository.findAll());

        Category category = categories.stream()
                .filter(c -> c.getCategory_id().equals(category_id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
        categoryRepository.delete(category);
        return "Category with ID " + category_id + " deleted successfully";
    }

    @Override
    public Category updateCategory(Long categoryId, Category category) {
//        Optional<Category> existingCategory = Optional.ofNullable(categories.stream()
//                .filter(c -> c.getCategory_id().equals(categoryId))
//                .findFirst()
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found.")));
        CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>(categoryRepository.findAll());

        Optional<Category> optionalCategory = categories.stream()
                .filter(c -> c.getCategory_id().equals(categoryId))
                .findFirst();

        if (optionalCategory.isPresent()) {
            Category existingCategory = optionalCategory.get();
            existingCategory.setCategoryName(category.getCategoryName());
            Category savedCategory = categoryRepository.save(existingCategory);
            return existingCategory;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found.");
        }

    }
}

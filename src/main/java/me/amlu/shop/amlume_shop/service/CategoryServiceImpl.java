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

import me.amlu.shop.amlume_shop.exceptions.APIException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import me.amlu.shop.amlume_shop.exceptions.NotFoundException;
import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.repositories.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>();

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CopyOnWriteArrayList<Category> getAllCategories() {
        if (categoryRepository.findAll().isEmpty()) {
            throw new NotFoundException("No categories created yet");
        }
        return new CopyOnWriteArrayList<>(categoryRepository.findAll());
    }

    @Override
    public void createCategory(Category category) {
        Optional<Category> savedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (savedCategory.isPresent()) {
            throw new APIException("Category with name " + category.getCategoryName() + " already exists");
        }
        categoryRepository.save(category);
        return CompletableFuture.supplyAsync(() -> {
        }, executorService).join();
    }

    @Override
    public String deleteCategory(Long category_id) {
        Category category = categoryRepository.findById(category_id).orElseThrow(() -> new ResourceNotFoundException("Category", "category_id", category_id));

        categoryRepository.delete(category);
        return "Category with ID " + category_id + " deleted successfully";
        return CompletableFuture.supplyAsync(() -> {
        }, executorService).join();
    }

    @Override
    public Category updateCategory(Long categoryId, Category category) {
        categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category", "category_id", categoryId));
        Category savedCategory;
        return CompletableFuture.supplyAsync(() -> {

        category.setCategory_id(categoryId);
        savedCategory = categoryRepository.save(category);
        return savedCategory;

        }, executorService).join();
    }
}

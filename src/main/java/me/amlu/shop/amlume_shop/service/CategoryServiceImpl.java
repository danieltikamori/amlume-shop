/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.exceptions.APIException;
import me.amlu.shop.amlume_shop.exceptions.NotFoundException;
import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.payload.CategoryDTO;
import me.amlu.shop.amlume_shop.payload.CategoryResponse;
import me.amlu.shop.amlume_shop.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final ExecutorService executorService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ModelMapper modelMapper, ExecutorService executorService) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
        this.executorService = executorService;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getAllCategories(int pageNumber, int pageSize, String sortBy, String sortDir) {

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories = categoryPage.getContent();
        if (categories.isEmpty())
            throw new NotFoundException("No category found");

        List<CategoryDTO> categoryDTOs = categories.parallelStream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .collect(Collectors.toList());

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setCategories(categoryDTOs);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());

        return categoryResponse;
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Category category = modelMapper.map(categoryDTO, Category.class);
            Optional<Category> categoryFromDB = categoryRepository.findByCategoryName(categoryDTO.getCategoryName());
            if (categoryFromDB.isPresent()) {
                throw new APIException("Category with name " + categoryDTO.getCategoryName() + " already exists");
            }
            Category savedCategory = categoryRepository.save(category);
            return modelMapper.map(savedCategory, CategoryDTO.class);
        }, executorService).join();
    }

    @Override
    @Transactional
    public CategoryDTO deleteCategory(Long categoryId) {
        return CompletableFuture.supplyAsync(() -> {
            Category existingCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

            categoryRepository.delete(existingCategory);
            return modelMapper.map(existingCategory, CategoryDTO.class);
        }, executorService).join();
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Category savedCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

            Category category = modelMapper.map(categoryDTO, Category.class);
            category.setCategoryId(categoryId);

            savedCategory = categoryRepository.save(category);
            return modelMapper.map(savedCategory, CategoryDTO.class);
        }, executorService).join();
    }
}

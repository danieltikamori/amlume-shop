/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management;

import me.amlu.shop.amlume_shop.exceptions.APIException;
import me.amlu.shop.amlume_shop.exceptions.CategoryDataValidationException;
import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.payload.CreateCategoryRequest;
import me.amlu.shop.amlume_shop.payload.GetCategoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    // Constructor
    public CategoryServiceImpl(CategoryRepository categoryRepository /*, ModelMapper modelMapper, ExecutorService executorService */) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Cacheable(value = "category", key = "#pageNumber + '_' + #pageSize + '_' + #sortBy + '_' + #sortDir")
    @Transactional(readOnly = true)
    public GetCategoryResponse getAllCategories(int pageNumber, int pageSize, String sortBy, String sortDir) {
        log.debug("Fetching categories page: {}, size: {}, sort: {}, dir: {}", pageNumber, pageSize, sortBy, sortDir);

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories = categoryPage.getContent();

        if (categories.isEmpty()) {
            log.info("No categories found for the given pagination.");
            // Returning empty response instead of throwing NotFoundException for an empty list
            return createCategoryResponse(categoryPage, List.of());
            // throw new NotFoundException("No category found"); // Original behaviour
        }

        // Use manual mapping, consider removing parallelStream unless proven necessary
        List<CreateCategoryRequest> createCategoryRequests = categories.stream()
                .map(this::mapEntityToDto) // Use manual mapping
                .collect(Collectors.toList());

        return createCategoryResponse(categoryPage, createCategoryRequests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllSubcategories(Category category) {
        Assert.notNull(category, "Category cannot be null");
        Assert.notNull(category.getHierarchyLevel(), "Category hierarchy level cannot be null"); // Need hierarchy for a path
        log.debug("Finding all subcategories for category ID: {}", category.getCategoryId());
        // Use the correct path from the fixed HierarchyLevel
        String pathPattern = category.getHierarchyLevel().getPath() + "."; // Append dot for direct children and descendants
        return categoryRepository.findByHierarchyLevelPathStartingWith(pathPattern);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAncestors(Category category) {
        Assert.notNull(category, "Category cannot be null");
        log.debug("Finding ancestors for category ID: {}", category.getCategoryId());
        List<Category> ancestors = new ArrayList<>();
        Category current = category.getParentCategory(); // Fetch parent lazily if needed
        while (current != null) {
            ancestors.add(current);
            current = current.getParentCategory(); // Fetch next parent
        }
        return ancestors; // Order is child -> parent -> grandparent...
    }

    @Override
    public boolean isDescendantOf(Category possibleDescendant, Category possibleAncestor) {
        Assert.notNull(possibleDescendant, "Possible descendant cannot be null");
        Assert.notNull(possibleAncestor, "Possible ancestor cannot be null");
        Assert.notNull(possibleDescendant.getHierarchyLevel(), "Descendant hierarchy level cannot be null");
        Assert.notNull(possibleAncestor.getHierarchyLevel(), "Ancestor hierarchy level cannot be null");

        // Use the correct path from the fixed HierarchyLevel
        String descendantPath = possibleDescendant.getHierarchyLevel().getPath();
        String ancestorPath = possibleAncestor.getHierarchyLevel().getPath();

        // Check if descendant path starts with ancestor path followed by a dot, and paths are different
        return descendantPath.startsWith(ancestorPath + ".") && !descendantPath.equals(ancestorPath);
    }

    @Override
    public int getDepth(Category category) {
        Assert.notNull(category, "Category cannot be null");
        return category.getDepth(); // Delegate to entity method
    }

    @Override
    public void validateHierarchy(Category category, Category newParent) {
        Assert.notNull(category, "Category cannot be null");
        // newParent can be null if moving to root

        if (category.equals(newParent)) { // Relies on correct equals (ID based)
            throw new CategoryDataValidationException("Category cannot be its own parent");
        }
        // Check only if newParent is not null and both have hierarchy info
        if (newParent != null && category.getHierarchyLevel() != null && newParent.getHierarchyLevel() != null && isDescendantOf(newParent, category)) {
            throw new CategoryDataValidationException("Cannot move category under one of its own descendants (circular hierarchy)");
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "category", allEntries = true) // Evict cache on create
    public CreateCategoryRequest createCategory(CreateCategoryRequest createCategoryRequest) {
        Assert.notNull(createCategoryRequest, "CategoryDTO cannot be null");
        log.debug("Attempting to create category with name: {}", createCategoryRequest.categoryName()); // Use record accessor

        // --- Manual Mapping & Validation ---
        CategoryName categoryName;
        Description description;
        Category parentCategory = null; // Assume root unless parentId is provided

        try {
            categoryName = new CategoryName(createCategoryRequest.categoryName()); // Use record accessor
            // Description might be optional in DTO? Handle null.
            description = (createCategoryRequest.description() != null) ? new Description(createCategoryRequest.description()) : null; // Use record accessor

            // --- Check Existence ---
            // Use corrected repository method
            if (categoryRepository.findByCategoryName_NameIgnoreCase(categoryName.getName()).isPresent()) {
                log.warn("Category already exists with name: {}", categoryName.getName());
                throw new APIException("Category with name '" + categoryName.getName() + "' already exists");
            }

            // --- Handle Parent ---
            if (createCategoryRequest.parentId() != null) { // Assuming CategoryDTO has parentId
                parentCategory = categoryRepository.findById(createCategoryRequest.parentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "parentId", createCategoryRequest.parentId()));
            }

        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Validation error creating category VOs: {}", e.getMessage());
            throw new CategoryDataValidationException("Invalid category data: " + e.getMessage(), e);
        }

        // --- Create Entity ---
        Category category;
        if (parentCategory != null) {
            // Use constructor for subcategory (will call setParentCategory internally)
            category = new Category(categoryName, parentCategory);
            category.updateDescription(description); // Set description if provided
            // Note: setParentCategory calculates initial hierarchy based on parent
        } else {
            // Use constructor for root category
            category = new Category(categoryName, description);
            // Hierarchy is initially set for root in constructor
        }
        // Set other fields if available in DTO (e.g., manager, status)
        // category.setCategoryManager(...)
        // category.setStatus(...) // Default is ACTIVE in constructor

        try {
            // --- Save ---
            Category savedCategory = categoryRepository.save(category);

            // --- IMPORTANT: Update Hierarchy Path After Save (if ID is needed) ---
            // If the path relies on the ID, we need to update it *after* the first save
            // when the ID is generated.
            if (savedCategory.getHierarchyLevel().getPath().contains("?")) {
                log.debug("Updating hierarchy path for new category ID: {}", savedCategory.getCategoryId());
                // Re-trigger hierarchy calculation by setting the parent again (or null for root)
                savedCategory.setParentCategory(savedCategory.getParentCategory());
                savedCategory = categoryRepository.save(savedCategory); // Save again with updated path
            }

            log.info("Successfully created category ID: {}", savedCategory.getCategoryId());
            return mapEntityToDto(savedCategory); // Use manual mapping

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving category: {}", e.getMessage());
            throw new APIException("Error saving category due to data integrity issue: " + e.getMessage(), e);
        }
        // Removed CompletableFuture wrapper for simplicity
    }

    @Override
    @Transactional
    @CacheEvict(value = "category", allEntries = true) // Evict cache on delete
    public CreateCategoryRequest deleteCategory(Long categoryId) {
        Assert.notNull(categoryId, "Category ID cannot be null");
        log.debug("Attempting to delete category ID: {}", categoryId);

        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        // Optional: Add check if category has products or subcategories before deleting?
        // if (!existingCategory.getProducts().isEmpty()) { ... }
        // if (!existingCategory.getSubCategories().isEmpty()) { ... }

        CreateCategoryRequest deletedDto = mapEntityToDto(existingCategory); // Map BEFORE deleting

        categoryRepository.delete(existingCategory); // Assumes soft delete via BaseEntity
        log.info("Successfully deleted category ID: {}", categoryId);

        return deletedDto;
    }

    @Override
    @Transactional
    @CacheEvict(value = "category", allEntries = true) // Evict cache on update
    public CreateCategoryRequest updateCategory(Long categoryId, CreateCategoryRequest createCategoryRequest) {
        Assert.notNull(categoryId, "Category ID cannot be null");
        Assert.notNull(createCategoryRequest, "CategoryDTO cannot be null");
        log.debug("Attempting to update category ID: {}", categoryId);

        // --- Fetch Existing Entity ---
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        // --- Manual Mapping & Validation ---
        try {
            // Create new VOs from DTO data
            CategoryName newName = new CategoryName(createCategoryRequest.categoryName()); // Use record accessor
            Description newDescription = (createCategoryRequest.description() != null) ? new Description(createCategoryRequest.description()) : null; // Use record accessor
            // Handle status update if present in DTO
            CategoryStatus newStatus = createCategoryRequest.status() != null ? new CategoryStatus(createCategoryRequest.status(), createCategoryRequest.active(), createCategoryRequest.reason()) : existingCategory.getStatus(); // Assuming DTO fields

            // --- Check Name Conflict (if name changed) ---
            if (!existingCategory.getCategoryName().equals(newName)) {
                Optional<Category> conflictingCategory = categoryRepository.findByCategoryName_NameIgnoreCase(newName.getName());
                if (conflictingCategory.isPresent() && !conflictingCategory.get().getCategoryId().equals(categoryId)) {
                    log.warn("Update failed: New category name '{}' already exists.", newName.getName());
                    throw new APIException("Category with name '" + newName.getName() + "' already exists");
                }
                existingCategory.updateName(newName); // Update name if no conflict
            }

            // --- Update Simple Fields/VOs ---
            existingCategory.updateDescription(newDescription);
            existingCategory.setStatus(newStatus);
            // Update manager if provided in DTO
            // if (categoryDTO.managerId() != null) { ... fetch user and set ... }

            // --- Handle Parent Change ---
            Long newParentId = createCategoryRequest.parentId(); // Assuming DTO has parentId
            Long currentParentId = existingCategory.getParentCategory() != null ? existingCategory.getParentCategory().getCategoryId() : null;

            if (!Objects.equals(newParentId, currentParentId)) {
                log.debug("Parent category change detected for category ID {}. New parent ID: {}", categoryId, newParentId);
                Category newParent = null;
                if (newParentId != null) {
                    newParent = categoryRepository.findById(newParentId)
                            .orElseThrow(() -> new ResourceNotFoundException("New Parent Category", "parentId", newParentId));
                }
                // Validate hierarchy before setting
                validateHierarchy(existingCategory, newParent);
                existingCategory.setParentCategory(newParent); // This updates hierarchy level and bidirectional links
            }

            // --- Save ---
            Category updatedCategory = categoryRepository.save(existingCategory);
            log.info("Successfully updated category ID: {}", updatedCategory.getCategoryId());
            return mapEntityToDto(updatedCategory); // Use manual mapping

        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Validation error updating category VOs for ID {}: {}", categoryId, e.getMessage());
            throw new CategoryDataValidationException("Invalid category data for update: " + e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating category ID {}: {}", categoryId, e.getMessage());
            throw new APIException("Error updating category due to data integrity issue: " + e.getMessage(), e);
        }
    }

    // --- Helper Methods ---

    /**
     * Manually maps a Category entity to a CategoryDTO.
     * Handles extraction of values from Value Objects.
     * Assumes CategoryDTO is a record or has appropriate fields/constructor.
     */
    private CreateCategoryRequest mapEntityToDto(Category category) {
        if (category == null) {
            return null;
        }

        Long categoryId = category.getCategoryId();
        String categoryName = (category.getCategoryName() != null) ? category.getCategoryName().getName() : null;
        String description = (category.getDescription() != null) ? category.getDescription().getDescriptionData() : null;
        Long parentId = (category.getParentCategory() != null) ? category.getParentCategory().getCategoryId() : null;
        Integer level = (category.getHierarchyLevel() != null) ? category.getHierarchyLevel().getLevel() : null;
        String path = (category.getHierarchyLevel() != null) ? category.getHierarchyLevel().getPath() : null;
        String status = (category.getStatus() != null) ? category.getStatus().getStatus() : null;
        Boolean active = (category.getStatus() != null) ? category.getStatus().isActive() : null;
        String reason = (category.getStatus() != null) ? category.getStatus().getReason() : null;
        Long managerId = (category.getCategoryManager() != null) ? category.getCategoryManager().getUserId() : null;
        // Add other fields as needed in DTO

        // Assuming CategoryDTO is a record with matching fields
        return new CreateCategoryRequest(
                categoryId,
                categoryName,
                description,
                parentId,
                level, // Assuming DTO has level
                path,  // Assuming DTO has path
                status, // Assuming DTO has status string
                active, // Assuming DTO has active boolean
                reason, // Assuming DTO has reason string
                managerId // Assuming DTO has managerId
                // Add other fields
        );
    }

    /**
     * Helper to create CategoryResponse.
     * Assumes CategoryResponse is a record or has appropriate fields/constructor.
     */
    private GetCategoryResponse createCategoryResponse(Page<Category> page, List<CreateCategoryRequest> dtoList) {
        // Assuming CategoryResponse is a record
        return new GetCategoryResponse(
                dtoList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

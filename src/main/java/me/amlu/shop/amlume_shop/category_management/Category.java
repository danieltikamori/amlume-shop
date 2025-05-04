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

import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.user_management.User;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "categories", indexes = @Index(name = "idx_category_name", columnList = "category_name"))
// Assuming ValueObjects maps to category_name
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Category extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false, updatable = false)
    private Long categoryId;

    @AttributeOverrides({
            @AttributeOverride(name = "categoryName.name", column = @Column(name = "category_name"))
    })
    @Embedded
    @Column(name = "category_name") // Explicit column name for index clarity
    private CategoryName categoryName;

    @AttributeOverrides({
            @AttributeOverride(name = "description.descriptionData", column = @Column(name = "description"))
    })
    @Embedded
    private Description description;

    @AttributeOverrides({
            @AttributeOverride(name = "hierarchyLevel.level", column = @Column(name = "hierarchy_level")),
            @AttributeOverride(name = "hierarchyLevel.path", column = @Column(name = "hierarchy_path"))
    })
    @Embedded
    private HierarchyLevel hierarchyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    // Cascade ALL for subcategories might be okay if deleting parent deletes children
    @BatchSize(size = 20)
    private Set<Category> subCategories = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY) // Removed CascadeType.ALL - deleting category shouldn't delete user
    @JoinColumn(name = "category_manager_id")
    private User categoryManager;

    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<Product> products = new HashSet<>();

    @AttributeOverrides({
            @AttributeOverride(name = "categoryStatus.status", column = @Column(name = "category_status")),
            @AttributeOverride(name = "categoryStatus.active", column = @Column(name = "is_active")),
            @AttributeOverride(name = "categoryStatus.reason", column = @Column(name = "status_reason"))
    })
    @Embedded
    private CategoryStatus status;

    // --- JPA Required No-Arg Constructor ---
    protected Category() {
        // Protected to discourage direct use, but satisfies JPA
    }

    // --- Custom Constructors ---

    /**
     * Constructor for creating a Category, often used for root categories initially.
     * Hierarchy level might need adjustment post-persistence or via setParentCategory.
     *
     * @param name        The name of the category.
     * @param description The description of the category.
     */
    public Category(CategoryName name, Description description) {
        this.categoryName = Objects.requireNonNull(name, "Category name cannot be null");
        this.description = description;
        // Initial hierarchy level for a potential root category.
        // The Path might be incomplete until ID is assigned.
        // Consider setting a path later.
        this.hierarchyLevel = new HierarchyLevel(0, "?"); // Placeholder path initially
        this.status = CategoryStatus.ACTIVE; // Default status example
    }

    /**
     * Constructor for creating a subcategory linked to a parent.
     *
     * @param name   The name of the subcategory.
     * @param parent The parent category.
     */
    public Category(CategoryName name, Category parent) {
        this.categoryName = Objects.requireNonNull(name, "Category name cannot be null");
        this.status = CategoryStatus.ACTIVE; // Default status example
        // setParentCategory handles hierarchy level and bidirectional link
        this.setParentCategory(Objects.requireNonNull(parent, "Parent category cannot be null for subcategory constructor"));
    }


    // --- Getters ---

    public Long getCategoryId() {
        return categoryId;
    }

    public CategoryName getCategoryName() {
        return categoryName;
    }

    public Description getDescription() {
        return description;
    }

    public HierarchyLevel getHierarchyLevel() {
        return hierarchyLevel;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    // Return an unmodifiable set to prevent direct modification bypassing add/remove methods
    public Set<Category> getSubCategories() {
        return Collections.unmodifiableSet(subCategories);
    }

    public User getCategoryManager() {
        return categoryManager;
    }

    // Return an unmodifiable set
    public Set<Product> getProducts() {
        return Collections.unmodifiableSet(products);
    }

    public CategoryStatus getStatus() {
        return status;
    }

    // --- Setters & Mutators ---

    // No setter for categoryId (generated, final)
    // No setter for hierarchyLevel (managed internally by setParentCategory)
    // No setter for subCategories (managed by setParentCategory/addSubCategory)
    // No setter for products (managed by addProduct/removeProduct)

    /**
     * Updates the category name.
     *
     * @param newName the new category name, not null.
     */
    public void updateName(CategoryName newName) {
        this.categoryName = Objects.requireNonNull(newName, "Category name cannot be null");
    }

    /**
     * Updates the category description.
     *
     * @param newDescription the new category description.
     */
    public void updateDescription(Description newDescription) {
        this.description = newDescription; // Allow null description? If not, add Objects.requireNonNull
    }

    /**
     * Sets the parent category and updates the hierarchy level and bidirectional links.
     * Should be called *after* the child category has been persisted and has an ID
     * if the hierarchy path relies on the ID.
     *
     * @param parent The parent category. If null, this category becomes a root category.
     */
    public void setParentCategory(Category parent) {
        // Remove from old parent's subcategories if exists
        if (this.parentCategory != null) {
            this.parentCategory.internalRemoveSubCategory(this);
        }

        this.parentCategory = parent;

        if (parent == null) {
            // Became a root category
            String path = (this.categoryId != null) ? this.categoryId.toString() : "?"; // Use ID if available
            this.hierarchyLevel = new HierarchyLevel(0, path);
        } else {
            // Became a subcategory
            String parentPath = parent.getHierarchyLevel() != null ? parent.getHierarchyLevel().getPath() : "?";
            String childIdPart = (this.categoryId != null) ? this.categoryId.toString() : "?"; // Use ID if available
            String newPath = parentPath + "." + childIdPart;
            int newLevel = (parent.getHierarchyLevel() != null ? parent.getHierarchyLevel().getLevel() : -1) + 1;

            this.hierarchyLevel = new HierarchyLevel(newLevel, newPath);
            // Add to new parent's subcategories
            parent.internalAddSubCategory(this);
        }
    }

    // Internal helper to manage a bidirectional link without exposing direct subcategory modification
    private void internalAddSubCategory(Category child) {
        if (this.subCategories == null) {
            this.subCategories = new HashSet<>();
        }
        this.subCategories.add(child);
    }

    // Internal helper to manage a bidirectional link
    private void internalRemoveSubCategory(Category child) {
        if (this.subCategories != null) {
            this.subCategories.remove(child);
        }
    }


    /**
     * Assigns a user as the manager for this category.
     *
     * @param categoryManager The user to assign as manager.
     */
    public void setCategoryManager(User categoryManager) {
        this.categoryManager = categoryManager;
    }

    /**
     * Sets the status of the category.
     *
     * @param status The new status, not null.
     */
    public void setStatus(CategoryStatus status) {
        this.status = Objects.requireNonNull(status, "Category status cannot be null");
    }

    /**
     * Adds a product to the category and sets the bidirectional relationship.
     *
     * @param product the product to add, not null.
     */
    public void addProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        if (this.products == null) {
            this.products = new HashSet<>();
        }
        this.products.add(product);
        product.setCategory(this); // Maintain a bidirectional link
    }

    /**
     * Removes a product from the category and clears the bidirectional relationship.
     *
     * @param product the product to remove, not null.
     */
    public void removeProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        if (this.products != null && this.products.remove(product)) {
            product.setCategory(null); // Maintain a bidirectional link
        }
    }

    // --- Business Logic Methods ---

    /**
     * Checks if the category has special restrictions based on its name.
     * Assumes CategoryName has a method like getName() or getName().
     * Adjust if needed.
     *
     * @return true if the category name contains "restricted" (case-insensitive).
     */
    public boolean hasSpecialRestrictions() {
        // Assuming CategoryName has getName() - adjust if it's getName() or other
        return categoryName != null && StringUtils.containsIgnoreCase(categoryName.getName(), "restricted");
    }

    /**
     * Checks if this is the main category based on its name.
     * Assumes CategoryName has a method like getName() or getName().
     * Adjust if needed.
     *
     * @return true if the category name is "main" (case-insensitive).
     */
    public boolean isMainCategory() {
        // Assuming CategoryName has getName() - adjust if it's getName() or other
        return categoryName != null && StringUtils.equalsIgnoreCase(categoryName.getName(), "main");
    }

    public boolean isRoot() {
        return parentCategory == null;
    }

    public boolean isLeaf() {
        return subCategories == null || subCategories.isEmpty();
    }

    public int getDepth() {
        return hierarchyLevel != null ? hierarchyLevel.getLevel() : -1; // Handle null hierarchyLevel
    }

    public String getCategoryPath() {
        return hierarchyLevel != null ? hierarchyLevel.getPath() : "?"; // Handle null hierarchyLevel
    }

    public List<Category> getPathFromRoot() {
        List<Category> path = new ArrayList<>();
        Category current = this;
        while (current != null) {
            path.addFirst(current); // Use addFirst for correct order
            current = current.getParentCategory();
        }
        return path;
    }

    public String getFormattedPath() {
        return getPathFromRoot().stream()
                // Assuming CategoryName has getName() - adjust if needed
                .map(category -> category.getCategoryName() != null ? category.getCategoryName().getName() : "?")
                .collect(Collectors.joining(" > "));
    }

    // --- BaseEntity Implementation ---

    @Override
    @Transient // Exclude from persistence mapping
    public Long getAuditableId() {
        return this.categoryId;
    }

    @Override
    @Transient // Exclude from persistence mapping
    public Long getId() {
        return this.categoryId;
    }

    // --- equals() and hashCode() ---

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // Handle Hibernate proxies correctly
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        // Cast is safe now
        Category category = (Category) o;
        // Compare by ID, ensuring ID is not null for persisted entities
        return getCategoryId() != null && Objects.equals(getCategoryId(), category.getCategoryId());
    }

    @Override
    public final int hashCode() {
        // Correct hashCode implementation based on the primary key (ID)
        // Consistent with equals logic.
        // Handles null ID for transient state.
        return Objects.hash(categoryId);
        // Alternative for transient entities (less common for DB entities):
        // return categoryId == null ? System.identityHashCode(this) : Objects.hash(categoryId);
    }

    // --- Manual toString() ---

    @Override
    public String toString() {
        // Basic toString, excluding lazy/potentially large collections/relationships
        // to prevent accidental loading and infinite loops.
        return new StringJoiner(", ", Category.class.getSimpleName() + "[", "]")
                .add("categoryId=" + categoryId)
                .add("categoryName=" + categoryName) // Relies on CategoryName.toString()
                .add("description=" + description) // Relies on Description.toString()
                .add("status=" + status) // Relies on CategoryStatus.toString()
                .add("hierarchyLevel=" + hierarchyLevel) // Relies on HierarchyLevel.toString()
                // Excluded: parentCategory,
                // subCategories, categoryManager, products
                .toString();
    }
}
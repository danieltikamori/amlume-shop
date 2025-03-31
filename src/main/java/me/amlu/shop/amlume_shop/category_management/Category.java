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
import lombok.*;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.model.Product;
import me.amlu.shop.amlume_shop.user_management.User;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "categories")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "categories", indexes = @Index(name = "idx_category_name", columnList = "category_name"))
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Embedded
    private CategoryName categoryName;

    @Embedded
    private Description description;

    @Embedded
    private HierarchyLevel hierarchyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory")
    @ToString.Exclude
    private Set<Category> subCategories = new HashSet<>();

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_manager_id")
    @ToString.Exclude
    private User categoryManager;

    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @ToString.Exclude
    @BatchSize(size = 20)
    private Set<Product> products = new HashSet<>();

    @Embedded
    private CategoryStatus status;

    public Category(CategoryName name, Description description) {
        this.categoryName = Objects.requireNonNull(name, "Category name cannot be null");
        this.description = description;
    }

    /**
     * Checks if the category has special restrictions
     *
     * @return true if the category name contains "restricted"
     */
    public boolean hasSpecialRestrictions() {
        return StringUtils.containsIgnoreCase((CharSequence) categoryName, "restricted");
    }

    /**
     * Checks if this is the main category
     *
     * @return true if the category name is "main" (case-insensitive)
     */
    public boolean isMainCategory() {
        return StringUtils.equalsIgnoreCase((CharSequence) categoryName, "main");
    }


    /**
     * Updates the category name
     *
     * @param newName the new category name
     */
    public void updateName(CategoryName newName) {
        this.categoryName = Objects.requireNonNull(newName, "Category name cannot be null");
    }

    /**
     * Updates the category description
     *
     * @param newDescription the new category description
     */
    public void updateDescription(Description newDescription) {
        this.description = newDescription;
    }

    // Constructor for root category
    public Category(CategoryName name) {
        this.categoryName = Objects.requireNonNull(name);
        this.hierarchyLevel = new HierarchyLevel(0, categoryId.toString());
    }

    // Constructor for subcategory
    public Category(CategoryName name, Category parent) {
        this.categoryName = Objects.requireNonNull(name);
        this.setParentCategory(parent);
    }

    public void setParentCategory(Category parent) {
        if (parent == null) {
            this.hierarchyLevel = new HierarchyLevel(0, categoryId.toString());
        } else {
            this.parentCategory = parent;
            String newPath = parent.getHierarchyLevel().getPath() + "." + categoryId;
            this.hierarchyLevel = new HierarchyLevel(
                    parent.getHierarchyLevel().getLevel() + 1,
                    newPath
            );
            parent.addSubCategory(this);
        }
    }

    private void addSubCategory(Category child) {
        subCategories.add(child);
    }

    public boolean isRoot() {
        return parentCategory == null;
    }

    public boolean isLeaf() {
        return subCategories.isEmpty();
    }

    public int getDepth() {
        return hierarchyLevel.getLevel();
    }

    // Method to get full category path
    public String getCategoryPath() {
        return hierarchyLevel.getPath();
    }

    // Get root to leaf path as a list of categories
    public List<Category> getPathFromRoot() {
        List<Category> path = new ArrayList<>();
        Category current = this;
        while (current != null) {
            path.addFirst(current);
            current = current.getParentCategory();
        }
        return path;
    }

    // Get formatted path string
    public String getFormattedPath() {
        return getPathFromRoot().stream()
                .map(category -> category.getCategoryName().getValue())
                .collect(Collectors.joining(" > "));
    }

    /**
     * Adds a product to the category
     *
     * @param product the product to add
     */
    public void addProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        products.add(product);
        product.setCategory(this);
    }

    /**
     * Removes a product from the category
     *
     * @param product the product to remove
     */
    public void removeProduct(Product product) {
        Objects.requireNonNull(product, "Product cannot be null");
        products.remove(product);
        product.setCategory(null);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof Category category)) return false;
        return getCategoryId() != null && Objects.equals(getCategoryId(), category.getCategoryId());
    }

    @Override
    public final int hashCode() {
        if (this instanceof HibernateProxy hibernateProxy) {
            return hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode();
        } else {
            return getClass().hashCode();
        }
    }

}

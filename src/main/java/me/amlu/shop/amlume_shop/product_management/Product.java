/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.product_management;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.order_management.Order;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@Table(name = "products")
// Add unique constraint at DB level for product name (optional but recommended)
// @Table(name = "products", uniqueConstraints = {
//    @UniqueConstraint(name = "uk_product_name", columnNames = {"productName_name"}) // Adjust column name if needed
// })
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false, updatable = false, unique = true)
    private Long productId;

    @AttributeOverrides({
            @AttributeOverride(name = "productName.name", column = @Column(name = "product_name", nullable = false, unique = true)),
    })
    @Column(name = "product_name", nullable = false)
    @Embedded
    @Valid
    // Optional: Override column name if Hibernate doesn't generate 'productName_name' as desired
    // @AttributeOverride(name = "name", column = @Column(name = "product_name_value", nullable = false, unique = true))
    private ProductName productName;

    @Column(name = "product_image") // This will store the GENERATED (UUID) filename
    private String productImage;

    @Column(name = "original_image_filename") // For reference
    private String originalImageFilename;     // Stores the original uploaded filename

    @AttributeOverrides({
            @AttributeOverride(name = "productDescription.description", column = @Column(name = "product_description", length = 2000)),
    })
    @Column(name = "product_description", length = 2000)
    @Embedded
    @Valid
    // Optional: Override column name if needed
    // @AttributeOverride(name = "description", column = @Column(name = "product_description_value", length = 2000))
    private ProductDescription productDescription;

    @Column(name = "product_quantity", nullable = false)
    @ColumnDefault("0")
    @Min(value = 0, message = "Product quantity must be greater than or equal to 0")
    private Integer productQuantity;

    @Embedded
    @Valid
    @AttributeOverride(name = "money.amount", column = @Column(name = "product_price", nullable = false, precision = 12, scale = 2))
    private Money productPrice;

    @Embedded
    @Valid
    @AttributeOverride(name = "discountPercentage.percentage", column = @Column(name = "product_discount_percentage", nullable = false, precision = 5, scale = 2))
    private DiscountPercentage productDiscountPercentage;

    @Embedded
    @Valid
    // Mark as not directly updatable via standard setters if calculation is always enforced
    @AttributeOverride(name = "money.amount", column = @Column(name = "product_special_price", nullable = false, precision = 12, scale = 2 /*, updatable = false ??? */))
    private Money productSpecialPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "order_id")
     private Order order;

    // --- JPA Required No-Arg Constructor ---
    public Product() {
        // JPA requires a no-arg constructor
    }

    // --- Getters ---

    public Long getProductId() {
        return productId;
    }

    public ProductName getProductName() {
        return productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public String getOriginalImageFilename() {
        return originalImageFilename;
    }

    public ProductDescription getProductDescription() {
        return productDescription;
    }

    public Integer getProductQuantity() {
        return productQuantity;
    }

    public Money getProductPrice() {
        return productPrice;
    }

    public DiscountPercentage getProductDiscountPercentage() {
        return productDiscountPercentage;
    }

    public Money getProductSpecialPrice() {
        return productSpecialPrice;
    }

    public Category getCategory() {
        return category;
    }

    public User getSeller() {
        return seller;
    }

    // --- Setters ---

    // No setter for productId as it's generated and updatable=false

    public void setProductName(ProductName productName) {
        this.productName = productName;
    }

    public void setProductImage(String productImage) { // Sets the GENERATED filename
        this.productImage = productImage;
    }

    public void setOriginalImageFilename(String originalImageFilename) { // For reference
        this.originalImageFilename = originalImageFilename;
    }

    public void setProductDescription(ProductDescription productDescription) {
        this.productDescription = productDescription;
    }

    public void setProductQuantity(Integer productQuantity) {
        this.productQuantity = productQuantity;
    }

    public void setProductPrice(Money productPrice) {
        this.productPrice = productPrice;
    }

    public void setProductDiscountPercentage(DiscountPercentage productDiscountPercentage) {
        this.productDiscountPercentage = productDiscountPercentage;
    }

    public void setProductSpecialPrice(Money productSpecialPrice) {
        this.productSpecialPrice = productSpecialPrice;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    // --- Business Logic Methods ---

    public User getCategoryManager() {
        return category != null ? category.getCategoryManager() : null;
    }

    public boolean isHighValue() {
        return productPrice != null && productPrice.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
    }

    public boolean isLowValue() {
        return productPrice != null && productPrice.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0;
    }

    public boolean isRestricted() {
        return category != null && category.hasSpecialRestrictions();
    }

    /**
     * Recalculates and sets the special price based on the current price and discount.
     * Should be called whenever price or discount percentage changes.
     */
    public void recalculateSpecialPrice() {
        if (this.productPrice != null && this.productDiscountPercentage != null) {
            // Use the logic from DiscountPercentage for consistency
            this.productSpecialPrice = this.productDiscountPercentage.applyToPrice(this.productPrice);
        } else if (this.productPrice != null) {
            // If no discount, special price is the same as regular price
            this.productSpecialPrice = this.productPrice;
        } else {
            // Handle case where price is null (shouldn't happen with nullable=false)
            this.productSpecialPrice = null; // Or maybe new Money(BigDecimal.ZERO)? Depends on requirements.
        }
    }

    // --- BaseEntity Implementation ---

    @Override
    @Transient // Exclude from persistence mapping
    public Long getAuditableId() {
        return this.productId;
    }

    // Optional: Keep getId() for convenience
    @Transient // Exclude from persistence mapping if only for convenience
    public Long getId() {
        return this.productId;
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
        // Cast is safe now after class check
        Product product = (Product) o;
        // Compare by ID, handle null ID for transient entities
        return getProductId() != null && Objects.equals(getProductId(), product.getProductId());
    }

    @Override
    public final int hashCode() {
        // Base hash code on the primary key
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        // Basic toString implementation, excluding lazy relationships by default
        // to avoid accidental loading. Add more fields if needed for debugging.
        return new StringJoiner(", ", Product.class.getSimpleName() + "[", "]")
                .add("productId=" + productId)
                .add("productName=" + productName)
                .add("productImage='" + productImage + "'") // Generated name
                .add("originalImageFilename='" + originalImageFilename + "'") // Original name
                .add("productDescription=" + productDescription)
                .add("productQuantity=" + productQuantity)
                .add("productPrice=" + productPrice)
                .add("productDiscountPercentage=" + productDiscountPercentage)
                .add("productSpecialPrice=" + productSpecialPrice)
                // Excluded: category, seller (as per original @ToString.exclude)
                // Add inherited fields from BaseEntity if desired, e.g.,
                // .add("version=" + getVersion())
                // .add("deleted=" + isDeleted())
                .toString();
    }
}
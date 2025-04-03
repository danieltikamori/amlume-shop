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
import jakarta.validation.constraints.*;
import lombok.*;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.model.Order;
import me.amlu.shop.amlume_shop.user_management.User;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@ToString(exclude = {"category", "seller", "order"}) // Exclude relationships from toString
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    //    @Tsid
//    @GeneratedValue(generator = "tsid_generator")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false, updatable = false, unique = true)
    private Long productId;

    @NotBlank(message = "Product title is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image")
    private String productImage;

    @NotBlank(message = "Product description is required")
    @Size(min = 2, max = 2000, message = "Product description must be between 2 and 2000 characters")
    @Column(name = "product_description", nullable = false, length = 2000) // Specify length for varchar
    private String productDescription;

    @Column(name = "product_quantity", nullable = false)
    @ColumnDefault("0")
    @Min(value = 0, message = "Product quantity must be greater than or equal to 0")
    private Integer productQuantity;

    @Column(name = "product_price", nullable = false, precision = 12, scale = 2) // Specify precision and scale
    @ColumnDefault("0.0")
    @Digits(integer = 10, fraction = 2, message = "Product price must be a valid number with up to 10 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than 0")
    private BigDecimal productPrice;

    @Column(name = "product_discount_percentage", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Product discount percentage must be greater than or equal to 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Product discount percentage must be less than or equal to 100")
    private BigDecimal productDiscountPercentage;

    @Column(name = "product_special_price", nullable = false, precision = 12, scale = 2) // Specify precision and scale
    @DecimalMin(value = "0.0", inclusive = false, message = "Product special price must be greater than 0")
    private BigDecimal productSpecialPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id") // Changed name to category_id
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private transient Order order;

    @Override
    @PrePersist
    public void prePersist() {
        if (productDiscountPercentage == null) {
            productDiscountPercentage = BigDecimal.ZERO;
        }
        if (productSpecialPrice == null) {
            productSpecialPrice = productPrice;
        }
    }

    public User getCategoryManager() {
        return category != null ? category.getCategoryManager() : null; // Handle null category
    }

    public boolean isHighValue() {
        return productPrice.compareTo(BigDecimal.valueOf(1000)) > 0;
    }

    public boolean isLowValue() {
        return productPrice.compareTo(BigDecimal.valueOf(1000)) < 0;
    }

    public boolean isRestricted() {
        return category != null && category.hasSpecialRestrictions(); // Handle null category
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
        if (!(o instanceof Product product)) return false;
        return getProductId() != null && Objects.equals(getProductId(), product.getProductId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}

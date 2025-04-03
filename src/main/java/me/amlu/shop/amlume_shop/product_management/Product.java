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
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    //    @Tsid
//    @GeneratedValue(generator = "tsid_generator")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, unique = true, name = "product_id")
    private Long productId;

    @NotBlank(message = "Product title is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
//    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Product name must only contain alphanumeric and symbol characters")
    @Column(nullable = false, name = "product_name")
    private String productName;

    private String productImage;

    @NotBlank(message = "Product description is required")
    @Size(min = 2, max = 2000, message = "Product description must be between 2 and 2000 characters")
//    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Product description must only contain alphanumeric and symbol characters")
    @Column(nullable = false, name = "product_description")
    private String productDescription;

    @Column(nullable = false, name = "product_quantity")
    @ColumnDefault("0")
    @Min(value = 0, message = "Product quantity must be greater than or equal to 0")
    private Integer productQuantity;

    @Column(nullable = false, name = "product_price")
    @ColumnDefault("0.0")
    @Digits(integer = 10, fraction = 2, message = "Product price must be a valid number with up to 10 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than or equal to 0")
    private BigDecimal productPrice;

    @Column(nullable = false, name = "product_discount_percentage")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product discount percentage must be greater than or equal to 0")
    @DecimalMax(value = "100.0", inclusive = false, message = "Product discount percentage must be less than or equal to 100")
    private BigDecimal productDiscountPercentage;

    @Column(nullable = false, name = "product_special_price")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product special price must be greater than or equal to 0")
    private BigDecimal productSpecialPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId")
    @ToString.Exclude
    private Category category;

//    @Column(nullable = false, name = "product_department")
//    private String department;
//
//    @Column(nullable = false, name = "product_region")
//    private String productRegion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
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
//        if (productRegion == null) {
//            throw new IllegalStateException("Product region cannot be null");
//        }
    }

    public User getCategoryManager() {
        return category.getCategoryManager();
    }

    public boolean isHighValue() {
        return productPrice.compareTo(BigDecimal.valueOf(1000)) > 0;
    }

    public boolean isLowValue() {
        return productPrice.compareTo(BigDecimal.valueOf(1000)) < 0;
    }

    public boolean isRestricted() {
        return category.hasSpecialRestrictions();
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

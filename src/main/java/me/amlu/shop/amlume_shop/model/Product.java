/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
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
public class Product {

//    @Tsid
//    @GeneratedValue(generator = "tsid_generator")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false,  unique = true, name = "product_id")
    private Long productId;

    @NotBlank(message = "Product title is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
//    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Product name must only contain alphanumeric and symbol characters")
    @Column(nullable = false,  name = "product_name")
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

    @Column(nullable = false,  name = "product_price")
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

    @PrePersist
    public void prePersist() {
        if (productDiscountPercentage == null) {
            productDiscountPercentage = BigDecimal.ZERO;
        }
        if (productSpecialPrice == null) {
            productSpecialPrice = productPrice;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Product product = (Product) o;
        return getProductId() != null && Objects.equals(getProductId(), product.getProductId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

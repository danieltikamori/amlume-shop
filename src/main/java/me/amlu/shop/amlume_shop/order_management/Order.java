/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.order_management;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.product_management.Product;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "order_status"),
        @Index(name = "idx_order_date", columnList = "order_date")
})
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", updatable = false, unique = true)
    private Long orderId;

    @NotBlank
    @Column(name = "order_number", updatable = false, unique = true)
    private String orderNumber;

    @NotBlank
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private List<Product> products;

    //    @NotBlank
//    @Column(name = "order_status")
//    private String orderStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @NotBlank
    @Column(name = "order_date")
    private Instant orderDate;

//    private String department;
//
//    private String region;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        if (!(o instanceof Order order)) return false;
        return getOrderId() != null && Objects.equals(getOrderId(), order.getOrderId());
    }

    @Override
    public final int hashCode() {
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        return thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public Long getAuditableId() {
        return this.orderId;
    }

    @Override
    public Long getId() {
        return this.orderId;
    }
}

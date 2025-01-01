/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity(name = "categories")
public class Category {

    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO) - Default
    // @GeneratedValue(strategy = GenerationType.SEQUENCE) - For supported databases
    // @GeneratedValue(strategy = GenerationType.TABLE) - For sequence unsupported databases. Creates a new table with sequence for each entity. Less efficient.
    // @GeneratedValue(strategy = GenerationType.IDENTITY) - Most used for MySQL and PostgreSQL
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long category_id;
    private String categoryName;

    public Category(Long category_id, String categoryName) {
        this.category_id = category_id;
        this.categoryName = categoryName;
    }

    // Default constructor is encouraged for JPA entities
    public Category() {
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategory_id() {
        return category_id;
    }

    public void setCategory_id(Long category_id) {
        this.category_id = category_id;
    }
}

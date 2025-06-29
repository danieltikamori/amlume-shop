/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.checkerframework.checker.units.qual.min;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

@Embeddable
public class CategoryName implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 200, message = "Category name must be between 2 and 200 characters")
    @Column(name = "category_name", nullable = false)
    private final String name;

    // --- Constructor ---

    public CategoryName(String name) {
        Objects.requireNonNull(name, "Category name cannot be null");
        String trimmedName = name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 200) {
            throw new IllegalArgumentException("Category name must be between 2 and 200 characters after trimming");
        }
        this.name = trimmedName;
    }

    // JPA constructor
    protected CategoryName() {
        this.name = null;
    }

    // --- Getter ---
    public String getName() {
        return name;
    }

    // equals/hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryName that = (CategoryName) o;
        // Case-insensitive comparison is good here
        return name != null && name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? Objects.hash(name.toLowerCase()) : 0;
    }

    // --- toString ---
    @Override
    public String toString() {
        return new StringJoiner(", ", CategoryName.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }
}

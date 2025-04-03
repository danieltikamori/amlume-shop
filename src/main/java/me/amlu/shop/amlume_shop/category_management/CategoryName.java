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

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Embeddable
@ToString
public class CategoryName implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 200, message = "Category name must be between 2 and 200 characters")
    private final String value;

    public CategoryName(String value) {
        Objects.requireNonNull(value, "Category name cannot be null");
        if (value.trim().length() < 2 || value.trim().length() > 200) {
            throw new IllegalArgumentException("Category name must be between 2 and 200 characters");
        }
        this.value = value.trim();
    }

    // Required for JPA
    protected CategoryName() {
        this.value = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryName that = (CategoryName) o;
        return value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.toLowerCase());
    }
}
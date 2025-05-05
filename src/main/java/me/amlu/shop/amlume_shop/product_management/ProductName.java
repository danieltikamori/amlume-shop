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

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial; // Import Serial
import java.io.Serializable; // Import Serializable
import java.util.Objects;
import java.util.StringJoiner;

@Embeddable
public class ProductName implements Serializable { // Add implements Serializable

    @Serial // Add serialVersionUID
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private final String name; // Keep final for immutability

    /**
     * JPA/Hibernate requires a no-arg constructor for embeddables, even if private/protected.
     * Made protected to discourage direct use while satisfying framework requirements.
     * The 'name' field will be null if this constructor is used directly without reflection.
     */
    protected ProductName() {
        this.name = null;
    }

    /**
     * Public constructor for creating ProductName instances.
     * Enforces validation rules (length, not blank) and trims the input.
     * <p>
     * - Trim the input before checking the length,
     * ensuring the stored value is always trimmed and meets the length criteria.
     *
     * @param name The product name string.
     * @throws NullPointerException     if name is null.
     * @throws IllegalArgumentException if name length is invalid after trimming.
     */
    public ProductName(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        String trimmedName = name.trim(); // Trim first
        if (trimmedName.length() < 2 || trimmedName.length() > 200) {
            throw new IllegalArgumentException("Product name must be between 2 and 200 characters after trimming");
        }
        this.name = trimmedName; // Store the trimmed version
    }

    // --- Getter ---
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductName that = (ProductName) o;
        // Use Objects.equals for null safety, although name should be non-null via public constructor.
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for null safety.
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        // Simple representation of the name.
        return new StringJoiner(", ", ProductName.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }
}
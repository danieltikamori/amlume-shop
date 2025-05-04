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
public class ProductDescription implements Serializable { // Add implements Serializable

    @Serial // Add serialVersionUID
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Product description is required")
    @Size(min = 2, max = 2000, message = "Product description must be between 2 and 2000 characters")
    private final String description; // Keep final for immutability

    /**
     * JPA/Hibernate requires a no-arg constructor for embeddables, even if private/protected.
     * Made protected to discourage direct use while satisfying framework requirements.
     * The 'description' field will be null if this constructor is used directly without reflection.
     */
    protected ProductDescription() {
        this.description = null;
    }

    /**
     * Public constructor for creating ProductDescription instances.
     * Enforces validation rules (length, not blank) and trims the input.
     * <p>
     * - Trim the input before checking the length,
     * ensuring the stored value is always trimmed and meets the length criteria.
     *
     * @param description The product description string.
     * @throws NullPointerException     if description is null.
     * @throws IllegalArgumentException if description length is invalid after trimming.
     */
    public ProductDescription(String description) {
        Objects.requireNonNull(description, "Description cannot be null");
        String trimmedDescription = description.trim(); // Trim first
        if (trimmedDescription.length() < 2 || trimmedDescription.length() > 2000) {
            throw new IllegalArgumentException("Product description must be between 2 and 2000 characters after trimming");
        }
        this.description = trimmedDescription; // Store the trimmed version
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductDescription that = (ProductDescription) o;
        // Use Objects.equals for null safety, although description should be non-null via public constructor.
        return Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for null safety.
        return Objects.hash(description);
    }

    @Override
    public String toString() {
        // Simple representation of the description.
        return new StringJoiner(", ", ProductDescription.class.getSimpleName() + "[", "]")
                .add("description='" + description + "'")
                .toString();
    }
}
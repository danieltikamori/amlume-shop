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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.io.Serial; // Import Serial
import java.io.Serializable; // Import Serializable
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class DiscountPercentage implements Serializable { // Add implements Serializable

    @Serial // Add serialVersionUID
    private static final long serialVersionUID = 1L;

    @Digits(integer = 3, fraction = 2, message = "Discount percentage must be a valid number with up to 3 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percentage must be greater than or equal to 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount percentage must be less than or equal to 100")
    private final BigDecimal percentage; // Keep final for immutability

    /**
     * JPA/Hibernate requires a no-arg constructor for embeddables, even if private/protected.
     * Made protected to discourage direct use while satisfying framework requirements.
     * The 'percentage' field will be null if this constructor is used directly without reflection.
     */
    protected DiscountPercentage() {
        this.percentage = null; // Or BigDecimal.ZERO if a default is desired and constraints allow
    }

    /**
     * Public constructor for creating DiscountPercentage instances.
     * Enforces validation rules.
     *
     * @param percentage The percentage value (must be between 0 and 100).
     * @throws NullPointerException     if percentage is null.
     * @throws IllegalArgumentException if percentage is outside the valid range.
     */
    public DiscountPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage cannot be null");
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        // Ensure scale is set correctly upon creation
        this.percentage = percentage.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    // --- Business Logic ---

    /**
     * Applies this discount percentage to the given price.
     *
     * @param price The original price (Money object).
     * @return A new Money object representing the price after the discount.
     */
    public Money applyToPrice(Money price) {
        Objects.requireNonNull(price, "Price cannot be null for applying discount");
        // Calculate discount factor (e.g., 15% -> 0.15)
        BigDecimal discountFactor = percentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        // Calculate the amount to subtract
        BigDecimal discountAmount = price.getAmount().multiply(discountFactor);
        // Calculate final price and return as a new Money object
        // Ensure the result is still positive (Money constructor handles this)
        return new Money(price.getAmount().subtract(discountAmount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountPercentage that = (DiscountPercentage) o;
        // BigDecimal.equals considers scale, compareTo ignores scale.
        // Since we enforce scale in the constructor, either works,
        // but compareTo is often preferred for monetary values.
        // Using Objects.equals is safest
        // if the protected constructor could somehow be used to create an instance with null.
        return Objects.equals(percentage, that.percentage);
        // Alternative using compareTo (if null percentage is impossible via public constructor):
        // return percentage != null && that.percentage != null && percentage.compareTo(that.percentage) == 0;
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for null safety, although percentage should be non-null via public constructor.
        return Objects.hash(percentage);
    }

    @Override
    public String toString() {
        // Provide a clear string representation, including the % sign.
        return (percentage != null ? percentage.toPlainString() : "null") + "%";
    }
}
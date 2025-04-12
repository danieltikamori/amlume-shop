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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.StringJoiner;

@Embeddable
public class Money {

    @Digits(integer = 10, fraction = 2, message = "Price must be a valid number with up to 10 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private final BigDecimal amount; // Keep final for immutability

    /**
     * JPA/Hibernate requires a no-arg constructor for embeddables, even if private/protected.
     * Made protected to discourage direct use while satisfying framework requirements.
     * The 'amount' field will be null if this constructor is used directly without reflection.
     */
    protected Money() {
        this.amount = null; // Or BigDecimal.ZERO if a default is desired and constraints allow
    }

    /**
     * Public constructor for creating Money instances.
     * Enforces validation rules (must be > 0) and sets scale.
     *
     * @param amount The monetary value (must be greater than 0).
     * @throws NullPointerException if amount is null.
     * @throws IllegalArgumentException if amount is not greater than 0.
     */
    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        // Ensure scale is set correctly upon creation
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    // --- Calculation Methods ---

    /**
     * Adds another Money value to this one.
     * @param other The Money value to add.
     * @return A new Money instance representing the sum.
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "Cannot add null Money value");
        return new Money(this.amount.add(other.getAmount()));
    }

    /**
     * Subtracts another Money value from this one.
     * @param other The Money value to subtract.
     * @return A new Money instance representing the difference.
     * @throws IllegalArgumentException if the result would be zero or negative.
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Cannot subtract null Money value");
        // The constructor will validate if the result is > 0
        return new Money(this.amount.subtract(other.getAmount()));
    }

    /**
     * Multiplies this Money value by a BigDecimal multiplier.
     * @param multiplier The value to multiply by.
     * @return A new Money instance representing the product.
     * @throws IllegalArgumentException if the result would be zero or negative.
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier cannot be null");
        // The constructor will validate if the result is > 0
        return new Money(this.amount.multiply(multiplier));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        // BigDecimal.equals considers scale, compareTo ignores scale.
        // Since we enforce scale in the constructor, either works, but compareTo is often preferred for monetary values.
        // Using Objects.equals is safest if the protected constructor could somehow be used to create an instance with null.
        return Objects.equals(amount, money.amount);
        // Alternative using compareTo (if null amount is impossible via public constructor):
        // return amount != null && money.amount != null && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for null safety, although amount should be non-null via public constructor.
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        // Simple representation of the amount. Could add currency symbol later if needed.
        return new StringJoiner(", ", Money.class.getSimpleName() + "[", "]")
                .add("amount=" + (amount != null ? amount.toPlainString() : "null"))
                .toString();
    }
}
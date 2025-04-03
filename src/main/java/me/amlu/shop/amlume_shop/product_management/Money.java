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

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class Money {

    @Digits(integer = 10, fraction = 2, message = "Price must be a valid number with up to 10 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    // Add methods for calculations if needed (e.g., add, subtract, multiply)
    public Money add(Money other) {
        return new Money(this.amount.add(other.getAmount()));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.getAmount()));
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }
}
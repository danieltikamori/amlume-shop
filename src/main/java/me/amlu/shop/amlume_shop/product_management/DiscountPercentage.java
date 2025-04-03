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
public class DiscountPercentage {

    @Digits(integer = 3, fraction = 2, message = "Discount percentage must be a valid number with up to 3 digits and 2 decimal places")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percentage must be greater than or equal to 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount percentage must be less than or equal to 100")
    private final BigDecimal percentage;

    public DiscountPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage cannot be null");
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        this.percentage = percentage.setScale(2, RoundingMode.HALF_UP);
    }

    public Money applyToPrice(Money price) {
        BigDecimal discountAmount = price.getAmount().multiply(percentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return new Money(price.getAmount().subtract(discountAmount));
    }
}
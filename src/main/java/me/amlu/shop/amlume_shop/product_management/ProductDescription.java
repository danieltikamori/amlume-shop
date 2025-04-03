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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class ProductDescription {

    @NotBlank(message = "Product description is required")
    @Size(min = 2, max = 2000, message = "Product description must be between 2 and 2000 characters")
    private final String description;

    public ProductDescription(String description) {
        Objects.requireNonNull(description, "Description cannot be null");
        if (description.trim().length() < 2 || description.trim().length() > 2000) {
            throw new IllegalArgumentException("Product description must be between 2 and 2000 characters");
        }
        this.description = description.trim();
    }
}
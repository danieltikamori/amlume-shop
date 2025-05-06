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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

@Embeddable
public class Description implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Description is required")
    @Size(min = 2, max = 2000, message = "Description must be between 2 and 2000 characters")
    private final String descriptionData;

    // Constructor
    public Description(String descriptionData) {
        Objects.requireNonNull(descriptionData, "Description cannot be null");
        String trimmedValue = descriptionData.trim();
        if (trimmedValue.length() < 2 || trimmedValue.length() > 2000) {
            throw new IllegalArgumentException("Description must be between 2 and 2000 characters after trimming");
        }
        this.descriptionData = trimmedValue;
    }

    // JPA constructor
    protected Description() {
        this.descriptionData = null;
    }

    // --- Getter ---
    public String getDescriptionData() {
        return descriptionData;
    }

    // equals/hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Description that = (Description) o;
        return Objects.equals(descriptionData, that.descriptionData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptionData);
    }

    // --- toString ---
    @Override
    public String toString() {
        return new StringJoiner(", ", Description.class.getSimpleName() + "[", "]")
                .add("descriptionData='" + descriptionData + "'")
                .toString();
    }
}
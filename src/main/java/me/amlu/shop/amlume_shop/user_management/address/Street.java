/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management.address;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Street implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 1, max = 250, message = "Street must be between 1 and 250 characters")
    @Column(name = "street_name")
    private String streetName;

    protected Street() {
    } // for JPA

    public Street(String streetName) {
        // It's better to perform validation using Bean Validation annotations and let the framework handle it,
        // but constructor validation is also an option. Ensure the streetName is not null or empty before trim.
        if (streetName == null || streetName.trim().isEmpty() || streetName.trim().length() > 250) {
            // Using a custom exception related to domain validation might be better than IllegalArgumentException
            throw new IllegalArgumentException("Street name must be between 1 and 250 characters");
        }
        this.streetName = streetName.trim(); // Trim whitespace
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use getClass() for strict streetName object equality
        if (o == null || getClass() != o.getClass()) return false;
        Street street = (Street) o;
        // Compare the core streetName field using Objects.equals for null safety
        return Objects.equals(streetName, street.streetName);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for concise and null-safe hashCode generation
        return Objects.hash(streetName);
    }

    @Override
    public String toString() {
        // This toString is reasonable for a simple streetName object
        return streetName;
    }

    public @NotBlank @Size(min = 1, max = 250, message = "Street must be between 1 and 250 characters") String getStreetName() {
        return this.streetName;
    }
}
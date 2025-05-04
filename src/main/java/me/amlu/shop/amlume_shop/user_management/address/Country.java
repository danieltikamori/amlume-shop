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
public class Country implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 2, max = 127, message = "Country must be between 2 and 127 characters")
    @Column(name = "country_code")
    private String countryCode;

    protected Country() {
    } // for JPA

    public Country(String countryCode) {
        // It's better to perform validation using Bean Validation annotations and let the framework handle it,
        // but constructor validation is also an option. Ensure the countryCode is not null or empty before trim.
        if (countryCode == null || countryCode.trim().length() < 2 || countryCode.trim().length() > 127) {
            // Using a custom exception related to domain validation might be better than IllegalArgumentException
            throw new IllegalArgumentException("Country code must be between 2 and 127 characters");
        }
        this.countryCode = countryCode.trim(); // Trim whitespace
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use getClass() for strict countryCode object equality
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        // Compare the core countryCode field using Objects.equals for null safety
        return Objects.equals(countryCode, country.countryCode);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for concise and null-safe hashCode generation
        return Objects.hash(countryCode);
    }

    @Override
    public String toString() {
        // This toString is reasonable for a simple countryCode object
        return countryCode;
    }

    public @NotBlank @Size(min = 2, max = 127, message = "Country code must be between 2 and 127 characters") String getCountryCode() {
        return this.countryCode;
    }
}

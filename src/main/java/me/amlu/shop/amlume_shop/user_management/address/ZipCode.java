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
import me.amlu.shop.amlume_shop.security.config.ValidPostalCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ZipCode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ValidPostalCode
    @NotBlank
    @Size(min = 2, max = 15, message = "Zip code must be between 2 and 15 characters")
    @Column(name = "zip_code")
    private String zipCode;

    protected ZipCode() {
    } // for JPA

    public ZipCode(String zipCode) {
        // It's better to perform validation using Bean Validation annotations and let the framework handle it,
        // but constructor validation is also an option. Ensure the zipCode is not null or empty before trim.
        if (zipCode == null || zipCode.trim().length() < 2 || zipCode.trim().length() > 250) {
            // Using a custom exception related to domain validation might be better than IllegalArgumentException
            throw new IllegalArgumentException("Zipcode must be between 2 and 250 characters");
        }
        this.zipCode = zipCode.trim(); // Trim whitespace
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use getClass() for strict zipCode object equality
        if (o == null || getClass() != o.getClass()) return false;
        ZipCode zipCode = (ZipCode) o;
        // Compare the core zipCode field using Objects.equals for null safety
        return Objects.equals(this.zipCode, zipCode.zipCode);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for concise and null-safe hashCode generation
        return Objects.hash(zipCode);
    }

    @Override
    public String toString() {
        // This toString is reasonable for a simple zipCode object
        return zipCode;
    }

    public @NotBlank @Size(min = 2, max = 15, message = "Zip code must be between 2 and 15 characters") String getZipCode() {
        return this.zipCode;
    }
}

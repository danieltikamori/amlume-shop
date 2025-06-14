/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static me.amlu.authserver.common.Constants.PASSWORD_FIELD_LENGTH;
import static me.amlu.authserver.common.SecurityConstants.*;

/**
 * Includes validation
 *
 * @author Daniel Itiro Tikamori
 */
@Embeddable
public class HashedPassword implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // The column definition will be in the User entity via @AttributeOverride
    // or if the column name in User matches this field name.
    // For clarity, we'll use the @AttributeOverride in User.
    @Column(name = "password", length = PASSWORD_FIELD_LENGTH)
    // If not overridden, but User will override to "password"
    private String value;

    public HashedPassword(String hashedPasswordValue) {
        Assert.hasText(hashedPasswordValue, "Hashed password value cannot be empty");
        // Optionally, add a check for a common prefix if your hashes always start with one (e.g., "{bcrypt}")
        // Assert.isTrue(hashedPasswordValue.startsWith("{bcrypt}"), "Hashed password must be in a recognized format");
        this.value = hashedPasswordValue;
    }

    public boolean passwordMatches(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty() || value == null) {
            return false; // Consider this case as not matching for security
        }

        try {
            // Create encoders for supported formats
            Map<String, PasswordEncoder> encoders = new HashMap<>();
            encoders.put("bcrypt", new BCryptPasswordEncoder());
            encoders.put("argon2", new Argon2PasswordEncoder(
                    ARGON2_SALT_LENGTH,
                    ARGON2_HASH_LENGTH,
                    ARGON2_PARALLELISM,
                    ARGON2_MEMORY,
                    ARGON2_ITERATIONS
            ));

            // Default encoder for new passwords
            String defaultEncoderId = "argon2";

            // Handle Spring Security's {id}hash format
            if (value.startsWith("{")) {
                DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder(defaultEncoderId, encoders);
                return delegatingEncoder.matches(rawPassword, value);
            }
            // Handle raw format without {id} prefix
            else if (value.startsWith("$")) {
                // BCrypt format
                if (value.startsWith("$2")) {
                    return encoders.get("bcrypt").matches(rawPassword, value);
                }
                // Argon2 format
                else if (value.startsWith("$argon2")) {
                    return encoders.get("argon2").matches(rawPassword, value);
                }
            }

            // If we get here, try with the default encoder as a fallback
            return encoders.get(defaultEncoderId).matches(rawPassword, value);

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error during password matching: " + e.getMessage());
            return false;
        }
    }

    public HashedPassword() {
    }

    @Override
    public String toString() {
        return "****"; // Avoid logging the actual hash
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HashedPassword other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        return Objects.equals(this$value, other$value);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HashedPassword;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }
}

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
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

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
    @Column(name = "password_value", length = 128) // Example, if not overridden, but User will override to "password"
    private String value;

    public HashedPassword(String hashedPasswordValue) {
        Assert.hasText(hashedPasswordValue, "Hashed password value cannot be empty");
        // Optionally, add a check for a common prefix if your hashes always start with one (e.g., "{bcrypt}")
        // Assert.isTrue(hashedPasswordValue.startsWith("{bcrypt}"), "Hashed password must be in a recognized format");
        this.value = hashedPasswordValue;
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

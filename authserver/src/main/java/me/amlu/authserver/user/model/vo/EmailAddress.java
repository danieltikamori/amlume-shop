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
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;


/**
 * Validation made by Apache Commons EmailValidator
 *
 * @author Daniel Itiro Tikamori
 */
@Embeddable
public class EmailAddress implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // Get a singleton instance of EmailValidator
    // By default, it does not allow local addresses (e.g., user@localhost)
    // To allow local addresses, use EmailValidator.getInstance(true)
    private static final EmailValidator validator = EmailValidator.getInstance();

    @Column(name = "email", nullable = false, unique = true, length = 254)
    // Renamed column for clarity if not overridden
    private String value;
    private transient String localPart; // Marked transient as it's derived and not persisted directly
    private transient String domain;    // Marked transient

    public EmailAddress(String address) {
        Assert.hasText(address, "Email address cannot be null or empty.");

        // Normalize to lowercase for consistent comparisons and storage.
//        Explanation: toLowerCase() without a Locale uses the system's default locale,
//        which can lead to unexpected behavior if the default locale has unusual case-mapping rules
//        (e.g., Turkish 'i').
//        For programmatic case conversion where consistency is key (like normalizing email addresses),
//        Locale.ROOT (locale-insensitive) or Locale.US (common for ASCII-dominant data) is preferred.
//        Ascii.toLowerCase from Guava is also an option if you only expect ASCII.
        String normalizedAddress = address.trim().toLowerCase(Locale.ROOT);

        // Use Apache Commons EmailValidator
        if (!validator.isValid(normalizedAddress)) {
            throw new IllegalArgumentException("Invalid email address format: " + address);
        }

        this.value = normalizedAddress;

        // Split into local part and domain (can still be useful)
        // The validator ensures there's an '@' symbol.
        int atIndex = this.value.lastIndexOf('@');
        if (atIndex > 0 && atIndex < this.value.length() - 1) { // Basic check after validation
            this.localPart = this.value.substring(0, atIndex);
            this.domain = this.value.substring(atIndex + 1);
        } else {
            // This case should ideally not be reached if validator.isValid() passed
            // and the email format is standard. Handle defensively.
            this.localPart = ""; // Or throw an internal error
            this.domain = "";    // Or throw an internal error
        }
    }

    public EmailAddress() {
    }

    // Static factory method (optional but good practice)
    public static EmailAddress of(String address) {
        return new EmailAddress(address);
    }

    // Getter for localPart (optional, but can be useful)
    public String getLocalPart() {
        if (this.localPart == null && this.value != null) { // Lazy initialization if needed
            int atIndex = this.value.lastIndexOf('@');
            if (atIndex > 0 && atIndex < this.value.length() - 1) {
                this.localPart = this.value.substring(0, atIndex);
            } else {
                this.localPart = "";
            }
        }
        return localPart;
    }

    // Getter for domain (optional)
    public String getDomain() {
        if (this.domain == null && this.value != null) { // Lazy initialization
            int atIndex = this.value.lastIndexOf('@');
            if (atIndex > 0 && atIndex < this.value.length() - 1) {
                this.domain = this.value.substring(atIndex + 1);
            } else {
                this.domain = "";
            }
        }
        return domain;
    }

    /**
     * Email validation using Apache Commons EmailValidator
     *
     * @param emailValue email address to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String emailValue) {
        if (emailValue == null) {
            return false;
        }
        // return true if emailValue matches your validation pattern, false otherwise
        return validator.isValid(emailValue);
    }


    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return this.value;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EmailAddress other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        return Objects.equals(this$value, other$value);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof EmailAddress;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }
}

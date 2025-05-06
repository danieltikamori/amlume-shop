/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.model.vo;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * PhoneNumber validated with a proper library
 *
 * @author Daniel Itiro Tikamori
 */

@Embeddable
public class PhoneNumber implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    // Store the phone number in E.164 format for consistency
    // The column definition will be in the User entity via @AttributeOverride
    @Column(name = "phone_number_value", length = 20)
    // Example if not overridden, User will override to "mobile_number"
    private String e164Value;

    /**
     * Constructs a PhoneNumber object.
     *
     * @param rawNumber     The raw phone number string.
     * @param defaultRegion The ISO 3166-1 alpha-2 country code to use for parsing if the number is not in international format.
     *                      Can be null if numbers are always expected in international format (e.g., +14155552671).
     * @throws IllegalArgumentException if the phone number is invalid.
     */
    public PhoneNumber(String rawNumber, String defaultRegion) {
        if (!StringUtils.hasText(rawNumber)) {
            // Allow null/empty rawNumber to represent no phone number, resulting in null e164Value
            this.e164Value = null;
            return;
        }
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(rawNumber, defaultRegion);
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                throw new IllegalArgumentException("Invalid phone number: " + rawNumber);
            }
            this.e164Value = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Could not parse phone number: " + rawNumber + ". " + e.getMessage(), e);
        }
    }

    /**
     * Convenience constructor assuming numbers might not always have a default region
     * or are expected to be in international format.
     *
     * @param rawNumber The raw phone number string.
     */
    public PhoneNumber(String rawNumber) {
        this(rawNumber, null); // Pass null for defaultRegion, relying on international format or util's best guess
    }

    public PhoneNumber() {
    }


    public String getFormattedNumber(PhoneNumberUtil.PhoneNumberFormat format) {
        if (this.e164Value == null) {
            return null;
        }
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(this.e164Value, null); // Region is not needed for E.164
            return phoneNumberUtil.format(parsedNumber, format);
        } catch (NumberParseException e) {
            // This should ideally not happen if e164Value was correctly formed
            return this.e164Value; // Fallback
        }
    }

    @Override
    public String toString() {
        return e164Value != null ? e164Value : "";
    }

    // Static factory method for nullable phone numbers
    public static PhoneNumber ofNullable(String rawNumber, String defaultRegion) {
        if (!StringUtils.hasText(rawNumber)) {
            return null; // Or return a PhoneNumber instance with null e164Value if preferred by JPA for @Embedded
        }
        return new PhoneNumber(rawNumber, defaultRegion);
    }

    public static PhoneNumber ofNullable(String rawNumber) {
        return ofNullable(rawNumber, null);
    }

    public String getE164Value() {
        return this.e164Value;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PhoneNumber other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$e164Value = this.getE164Value();
        final Object other$e164Value = other.getE164Value();
        return Objects.equals(this$e164Value, other$e164Value);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PhoneNumber;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $e164Value = this.getE164Value();
        result = result * PRIME + ($e164Value == null ? 43 : $e164Value.hashCode());
        return result;
    }
}
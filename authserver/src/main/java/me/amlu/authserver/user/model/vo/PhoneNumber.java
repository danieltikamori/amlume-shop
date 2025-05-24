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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a validated phone number, stored in E.164 format.
 * This class is an immutable value object implemented as a record.
 * <p>
 * The {@code @AttributeOverride(name = "e164Value", column = @Column(name = "mobile_number", ...))}
 * in the {@code User} entity maps the {@code e164Value} field of this embeddable
 * to the {@code mobile_number} column in the {@code users} table.
 *
 * @author Daniel Itiro Tikamori
 */
@Embeddable
public record PhoneNumber(
        // This 'e164Value' is the property name referenced in User's @AttributeOverride.
        // The @Column annotation here defines default column properties if PhoneNumber
        //  is embedded without an override, but User's @AttributeOverride takes precedence.
        // It's good practice to keep it for clarity, even if overridden.
        @Column(name = "e164_value_default", length = 20) // Default name if is not overridden by User entity
        String e164Value
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PhoneNumber.class);
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    /**
     * Compact constructor for the record.
     * This constructor is called after the canonical constructor.
     * It's a good place for validation of the record's components.
     * However, for PhoneNumber, we want to allow a null e164Value if no phone number is provided.
     * The primary validation and parsing happen in the factory method `ofNullable`.
     * If an instance is created directly with a non-null e164Value, we assume it's already validated and formatted.
     *
     * @param e164Value The E.164 formatted phone number string. Can be null.
     */
    public PhoneNumber {
        // If e164Value is not null, it should ideally be a valid E.164 string.
        // For direct instantiation, we trust the input or rely on factory methods for validation.
        // Objects.requireNonNull(e164Value, "e164Value cannot be null when creating a PhoneNumber instance.");
        // The above line would prevent storing a "null" phone number.
        // If a PhoneNumber object *must* always represent a number, then make e164Value non-null.
        // But since User.mobileNumber can be null, this PhoneNumber VO should also support a null state.
        // A null e164Value here means "no phone number".
    }

    /**
     * Static factory method to create a {@code PhoneNumber} instance from a raw phone number string.
     * If the provided {@code rawNumber} is null or blank, this method returns {@code null},
     * indicating no phone number.
     * Otherwise, it attempts to parse and validate the number using the specified {@code defaultRegion}.
     *
     * @param rawNumber     The raw phone number string to parse.
     * @param defaultRegion The ISO 3166-1 alpha-2 country code (e.g., "US", "GB") to use as a hint
     *                      if the number is not in international E.164 format. Can be null if numbers
     *                      are expected to be in E.164 format already.
     * @return A {@code PhoneNumber} instance containing the number in E.164 format if valid,
     * or {@code null} if {@code rawNumber} is null/blank (representing no phone number).
     * @throws IllegalArgumentException if {@code rawNumber} is provided but is unparseable or invalid.
     */
    public static PhoneNumber ofNullable(String rawNumber, String defaultRegion) {
        if (!StringUtils.hasText(rawNumber)) {
            return null; // Represents no phone number / clearing the phone number
        }

        try {
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber parsedProto = phoneUtil.parse(rawNumber, defaultRegion);
            if (phoneUtil.isValidNumber(parsedProto)) {
                String e164FormattedString = phoneUtil.format(parsedProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                return new PhoneNumber(e164FormattedString);
            } else {
                log.warn("Provided phone number string '{}' (defaultRegion: '{}') is not a valid number.", rawNumber, defaultRegion);
                throw new IllegalArgumentException("Invalid phone number format or value: " + rawNumber);
            }
        } catch (NumberParseException e) {
            log.warn("Failed to parse phone number string '{}' (defaultRegion: '{}'). Error: {}", rawNumber, defaultRegion, e.getMessage());
            throw new IllegalArgumentException("Unparseable phone number: " + rawNumber, e);
        }
    }

    /**
     * Convenience factory method assuming numbers might not always have a default region
     * or are expected to be in international format.
     *
     * @param rawNumber The raw phone number string.
     * @return A {@code PhoneNumber} instance or {@code null}.
     */
    public static PhoneNumber ofNullable(String rawNumber) {
        return ofNullable(rawNumber, null);
    }

    /**
     * Returns the E.164 formatted phone number string.
     * This is the accessor method automatically provided by the record for the `e164Value` component.
     * No need to explicitly define `public String getE164Value()`.
     */
    // public String e164Value() { return this.e164Value; } // Implicitly provided by record

    /**
     * Gets a formatted version of the phone number.
     *
     * @param format The desired {@link PhoneNumberUtil.PhoneNumberFormat}.
     * @return The formatted phone number string, or null if this PhoneNumber has no value.
     */
    public String getFormattedNumber(PhoneNumberUtil.PhoneNumberFormat format) {
        if (this.e164Value == null) {
            return null;
        }
        try {
            // Region hint is not strictly needed for parsing a valid E.164 string.
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber parsedProto = phoneUtil.parse(this.e164Value, null);
            return phoneUtil.format(parsedProto, format);
        } catch (NumberParseException e) {
            // This should ideally not happen if e164Value was correctly formed and stored.
            log.error("Could not re-parse stored E.164 phone number '{}' for formatting: {}", this.e164Value, e.getMessage());
            return this.e164Value; // Fallback to the stored E.164 string.
        }
    }

    /**
     * Overrides the default {@code toString()} to return the E.164 formatted phone number,
     * or an empty string if the phone number is not set.
     *
     * @return The E.164 string representation of the phone number, or empty string.
     */
    @Override
    public String toString() {
        return this.e164Value != null ? this.e164Value : "";
    }

    // equals() and hashCode() are automatically generated by the record based on e164Value.

    // --- Optional: Convenience methods to extract parts of the number ---

    public int getCountryCode() {
        if (this.e164Value == null) return 0; // Or throw exception
        try {
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber parsedProto = phoneUtil.parse(this.e164Value, null);
            return parsedProto.getCountryCode();
        } catch (NumberParseException e) {
            log.error("Could not re-parse stored E.164 phone number '{}' to get country code: {}", this.e164Value, e.getMessage());
            return -1; // Or throw a runtime exception
        }
    }

    public String getNationalNumber() {
        if (this.e164Value == null) return ""; // Or throw exception
        try {
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber parsedProto = phoneUtil.parse(this.e164Value, null);
            return String.valueOf(parsedProto.getNationalNumber());
        } catch (NumberParseException e) {
            log.error("Could not re-parse stored E.164 phone number '{}' to get national number: {}", this.e164Value, e.getMessage());
            return ""; // Or throw a runtime exception
        }
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber; // Import the correct PhoneNumber type
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import me.amlu.shop.amlume_shop.security.config.Phone;
import me.amlu.shop.amlume_shop.service.PhoneNumberConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContactInfo implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ContactInfo.class);
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();


    @Column(name = "given_name")
    @Size(min = 1, max = 127, message = "Given name must be between 1 and 127 characters")
    private String givenName;

    @Column(name = "surname")
    @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters")
    private String surname;

    @Column(name = "nickname", unique = true)
    @Size(min = 1, max = 127, message = "Nickname must be between 1 and 127 characters")
    private String nickname;

    @Embedded
    @Email
    private UserEmail userEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Phone
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Convert(converter = PhoneNumberConverter.class)
    // @Size is not applicable to Phonenumber.PhoneNumber type directly.
    // Length/format validation is inherent to the Phonenumber object or handled by @Phone.
    @Column(name = "phone_number", length = 50) // Database column still stores a String (e.g., E.164)
    private Phonenumber.PhoneNumber phoneNumber;

    protected ContactInfo() {
    } // Required by JPA

    // Constructor updated to accept Phonenumber.PhoneNumber
    public ContactInfo(String givenName, String surname, String nickname, UserEmail userEmail, boolean emailVerified, Phonenumber.PhoneNumber phoneNumber) {
        this.givenName = givenName;
        this.surname = surname;
        this.nickname = nickname;
        this.userEmail = userEmail;
        this.emailVerified = emailVerified;
        this.phoneNumber = phoneNumber;
    }


    public ContactInfo(UserEmail userEmail, Phonenumber.PhoneNumber phoneNumber) {
        this.userEmail = userEmail;
        this.phoneNumber = phoneNumber;
    }

    private static boolean $default$emailVerified() {
        return false;
    }

    public static ContactInfoBuilder builder() {
        return new ContactInfoBuilder();
    }

    public String getEmail() {
        return userEmail != null ? userEmail.getEmail() : null;
    }

    // Renamed for clarity, as UserEmail is the embedded object
    public UserEmail getUserEmailObject() {
        return userEmail;
    }

    public ContactInfo withGivenName(String givenName) {
        return new ContactInfo(givenName, this.surname, this.nickname, this.userEmail, this.emailVerified, this.phoneNumber);
    }

    public ContactInfo withSurname(String surname) {
        return new ContactInfo(this.givenName, surname, this.nickname, this.userEmail, this.emailVerified, this.phoneNumber);
    }

    public @Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters") String getGivenName() {
        return this.givenName;
    }

    public @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String getSurname() {
        return this.surname;
    }

    public @Size(min = 1, max = 127, message = "Nickname must be between 1 and 127 characters") String getNickname() {
        return this.nickname;
    }


    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    // Getter now returns the Phonenumber.PhoneNumber object
    public Phonenumber.PhoneNumber getPhoneNumber() {
        return this.phoneNumber;
    }

    // This method is no longer needed as getPhoneNumber() returns the object.
    // public Phonenumber.PhoneNumber getPhoneNumberObject() { ... }

    /**
     * Provides the phone number as a string, typically in E.164 format.
     * Useful for DTOs or when a string representation is needed.
     * This method relies on the PhoneNumberConverter's logic or can use PhoneNumberUtil directly.
     *
     * @return Phone number as string, or null if not set.
     */
    @JsonIgnore // Avoid duplicate serialization if phoneNumber object is also serialized
    public String getPhoneNumberString() {
        if (this.phoneNumber == null) {
            return null;
        }
        // Option 1: Use the converter (if accessible and appropriate)
        // PhoneNumberConverter converter = new PhoneNumberConverter();
        // return converter.convertToDatabaseColumn(this.phoneNumber);

        // Option 2: Use PhoneNumberUtil directly (more common for this purpose)
        return phoneUtil.format(this.phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }


    // --- Modifying Methods ---

    // Improve validation logic
    @Email
    public void updateEmailAddress(String newEmailAddress) {
        if (newEmailAddress != null) {
            this.userEmail = new UserEmail(newEmailAddress);
            // Consider if emailVerified should be reset upon userEmail change
            // this.emailVerified = false;
        }
    }

    /**
     * Updates the phone number from a string.
     * The string will be parsed into a Phonenumber.PhoneNumber object.
     *
     * @param newPhoneNumberString The new phone number as a string.
     * @throws IllegalArgumentException if the string is not a valid phone number.
     */
    public void updatePhoneNumber(String newPhoneNumberString) {
        if (newPhoneNumberString != null && !newPhoneNumberString.trim().isEmpty()) {
            try {
                // Attempt to parse. A default region might be needed if numbers are not always international.
                // For simplicity, assuming international format or a default region configured elsewhere if necessary.
                this.phoneNumber = phoneUtil.parse(newPhoneNumberString, null /* Default region, e.g., "US" */);
            } catch (NumberParseException e) {
                log.warn("Failed to parse phone number string '{}': {}", newPhoneNumberString, e.getMessage());
                throw new IllegalArgumentException("Invalid phone number format: " + newPhoneNumberString, e);
            }
        } else {
            this.phoneNumber = null; // Clear the phone number
        }
    }

    /**
     * Updates the phone number using a Phonenumber.PhoneNumber object.
     * This is now the primary way to set the phoneNumber field with a rich type.
     *
     * @param newPhoneNumber The new Phonenumber.PhoneNumber object.
     */
    public void updatePhoneNumber(Phonenumber.PhoneNumber newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    // --- End Modifying Methods ---

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ContactInfo other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$givenName = this.getGivenName();
        final Object other$givenName = other.getGivenName();
        if (!Objects.equals(this$givenName, other$givenName)) return false;
        final Object this$surname = this.getSurname();
        final Object other$surname = other.getSurname();
        if (!Objects.equals(this$surname, other$surname)) return false;
        final Object this$nickname = this.getNickname();
        final Object other$nickname = other.getNickname();
        if (!Objects.equals(this$nickname, other$nickname)) return false;
        final Object this$userEmail = this.userEmail; // Compare the UserEmail object
        final Object other$userEmail = other.userEmail;
        if (!Objects.equals(this$userEmail, other$userEmail)) return false;
        if (this.isEmailVerified() != other.isEmailVerified()) return false;
        final Object this$phoneNumber = this.getPhoneNumber(); // Compare Phonenumber.PhoneNumber objects
        final Object other$phoneNumber = other.getPhoneNumber();
        return Objects.equals(this$phoneNumber, other$phoneNumber);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ContactInfo;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $firstName = this.getGivenName();
        result = result * PRIME + ($firstName == null ? 43 : $firstName.hashCode());
        final Object $surname = this.getSurname();
        result = result * PRIME + ($surname == null ? 43 : $surname.hashCode());
        final Object $nickname = this.getNickname();
        result = result * PRIME + ($nickname == null ? 43 : $nickname.hashCode());
        final Object $userEmail = this.userEmail;
        result = result * PRIME + ($userEmail == null ? 43 : $userEmail.hashCode());
        result = result * PRIME + (this.isEmailVerified() ? 79 : 97);
        final Object $phoneNumber = this.getPhoneNumber();
        result = result * PRIME + ($phoneNumber == null ? 43 : $phoneNumber.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ContactInfo(" +
                "givenName=" + this.getGivenName() +
                ", surname=" + this.getSurname() +
                ", nickname=" + this.getNickname() +
                ", userEmail=" + this.userEmail + // Relies on UserEmail's toString
                ", emailVerified=" + this.isEmailVerified() +
                ", phoneNumber=" + (this.phoneNumber != null ? phoneUtil.format(this.phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164) : "null") +
               ")";
    }

    public static class ContactInfoBuilder {
        private @Size(min = 1, max = 127, message = "Given name must be between 1 and 127 characters") String givenName;
        private @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String surname;
        private @Size(min = 1, max = 127, message = "Nickname must be between 1 and 127 characters") String nickname;
        private UserEmail userEmail;
        private boolean emailVerified$value;
        private boolean emailVerified$set;
        private Phonenumber.PhoneNumber phoneNumber;

        ContactInfoBuilder() {
        }

        public ContactInfoBuilder givenName(@Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters") String givenName) {
            this.givenName = givenName;
            return this;
        }

        public ContactInfoBuilder surname(@Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String surname) {
            this.surname = surname;
            return this;
        }

        public ContactInfoBuilder nickname(@Size(min = 1, max = 127, message = "Nickname must be between 1 and 127 characters") String nickname) {
            this.nickname = nickname;
            return this;
        }

        public ContactInfoBuilder userEmail(UserEmail userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public ContactInfoBuilder emailVerified(boolean emailVerified) {
            this.emailVerified$value = emailVerified;
            this.emailVerified$set = true;
            return this;
        }

        // Builder method now accepts Phonenumber.PhoneNumber
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public ContactInfoBuilder phoneNumber(Phonenumber.PhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        // Optional: Keep a method to build from string, which involves parsing
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public ContactInfoBuilder phoneNumberString(String phoneNumberString) {
            if (phoneNumberString != null && !phoneNumberString.trim().isEmpty()) {
                try {
                    this.phoneNumber = phoneUtil.parse(phoneNumberString, null /* Default region */);
                } catch (NumberParseException e) {
                    log.warn("Builder: Failed to parse phone number string '{}': {}", phoneNumberString, e.getMessage());
                    throw new IllegalArgumentException("Invalid phone number string for builder: " + phoneNumberString, e);
                }
            } else {
                this.phoneNumber = null;
            }
            return this;
        }


        public ContactInfo build() {
            boolean emailVerifiedValue = this.emailVerified$set ? this.emailVerified$value : ContactInfo.$default$emailVerified();
            return new ContactInfo(this.givenName, this.surname, this.nickname, this.userEmail, emailVerifiedValue, this.phoneNumber);
        }

        @Override
        public String toString() {
            return "ContactInfo.ContactInfoBuilder(givenName=" + this.givenName +
                    ", surname=" + this.surname +
                    ", nickname=" + this.nickname +
                    ", userEmail=" + this.userEmail +
                    ", emailVerified$value=" + this.emailVerified$value +
                    ", phoneNumber=" + (this.phoneNumber != null ? phoneUtil.format(this.phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164) : "null") +
                    ")";
        }
    }
}

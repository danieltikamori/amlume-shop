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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import me.amlu.shop.amlume_shop.config.Phone;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContactInfo implements Serializable {

    @Column(name = "first_name")
    @Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters")
    private String firstName;

    @Column(name = "last_name")
    @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters")
    private String lastName;

    @Embedded
    @Email
    private UserEmail userEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Phone
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters")
    @Column(name = "phone_number")
    private Phonenumber.PhoneNumber phoneNumber;

    protected ContactInfo() {
    } // Required by JPA

    public ContactInfo(String firstName, String lastName, UserEmail userEmail, boolean emailVerified, Phonenumber.PhoneNumber phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userEmail = userEmail;
        this.emailVerified = emailVerified;
        this.phoneNumber = phoneNumber;
    }

    private static boolean $default$emailVerified() {
        return false;
    }

    public static ContactInfoBuilder builder() {
        return new ContactInfoBuilder();
    }

    public String getEmail() {
        return userEmail.getEmail();
    }

    protected String getUserEmail() {
        return userEmail.getEmail();
    }

    public ContactInfo withFirstName(String firstName) {

        return new ContactInfo(firstName, lastName, userEmail, emailVerified, phoneNumber);
    }

    public ContactInfo withLastName(String lastName) {
        return new ContactInfo(firstName, lastName, userEmail, emailVerified, phoneNumber);
    }

    public @Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters") String getFirstName() {
        return this.firstName;
    }

    public @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String getLastName() {
        return this.lastName;
    }

    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    public Phonenumber.@Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters") PhoneNumber getPhoneNumber() {
        return this.phoneNumber;
    }

    // --- Modifying Methods ---

    // Improve validation logic
    @Email
    public void updateEmailAddress(String newEmailAddress) {
        if (newEmailAddress != null) {
            this.userEmail = new UserEmail(newEmailAddress);
        }
    }

    // --- End Modifying Methods ---

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ContactInfo other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$firstName = this.getFirstName();
        final Object other$firstName = other.getFirstName();
        if (!Objects.equals(this$firstName, other$firstName)) return false;
        final Object this$lastName = this.getLastName();
        final Object other$lastName = other.getLastName();
        if (!Objects.equals(this$lastName, other$lastName)) return false;
        final Object this$userEmail = this.getUserEmail();
        final Object other$userEmail = other.getUserEmail();
        if (!Objects.equals(this$userEmail, other$userEmail)) return false;
        if (this.isEmailVerified() != other.isEmailVerified()) return false;
        final Object this$phoneNumber = this.getPhoneNumber();
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
        final Object $firstName = this.getFirstName();
        result = result * PRIME + ($firstName == null ? 43 : $firstName.hashCode());
        final Object $lastName = this.getLastName();
        result = result * PRIME + ($lastName == null ? 43 : $lastName.hashCode());
        final Object $userEmail = this.getUserEmail();
        result = result * PRIME + ($userEmail == null ? 43 : $userEmail.hashCode());
        result = result * PRIME + (this.isEmailVerified() ? 79 : 97);
        final Object $phoneNumber = this.getPhoneNumber();
        result = result * PRIME + ($phoneNumber == null ? 43 : $phoneNumber.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ContactInfo(firstName=" + this.getFirstName() + ", lastName=" + this.getLastName() + ", userEmail=" + this.getUserEmail() + ", emailVerified=" + this.isEmailVerified() + ", phoneNumber=" + this.getPhoneNumber() + ")";
    }

    public static class ContactInfoBuilder {
        private @Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters") String firstName;
        private @Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String lastName;
        private UserEmail userEmail;
        private boolean emailVerified$value;
        private boolean emailVerified$set;
        private Phonenumber.@Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters") PhoneNumber phoneNumber;

        ContactInfoBuilder() {
        }

        public ContactInfoBuilder firstName(@Size(min = 1, max = 127, message = "First name must be between 1 and 127 characters") String firstName) {
            this.firstName = firstName;
            return this;
        }

        public ContactInfoBuilder lastName(@Size(min = 1, max = 127, message = "Last name must be between 1 and 127 characters") String lastName) {
            this.lastName = lastName;
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

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public ContactInfoBuilder phoneNumber(@Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters") Phonenumber.PhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public ContactInfo build() {
            boolean emailVerified$value = this.emailVerified$value;
            if (!this.emailVerified$set) {
                emailVerified$value = ContactInfo.$default$emailVerified();
            }
            return new ContactInfo(this.firstName, this.lastName, this.userEmail, emailVerified$value, this.phoneNumber);
        }

        @Override
        public String toString() {
            return "ContactInfo.ContactInfoBuilder(firstName=" + this.firstName + ", lastName=" + this.lastName + ", userEmail=" + this.userEmail + ", emailVerified$value=" + this.emailVerified$value + ", phoneNumber=" + this.phoneNumber + ")";
        }
    }
}
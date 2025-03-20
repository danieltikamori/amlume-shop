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
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.amlu.shop.amlume_shop.config.Phone;

import java.io.Serializable;

@Builder
@Embeddable
@Getter
@EqualsAndHashCode
public class ContactInfo implements Serializable {

    @Embedded
    private UserEmail userEmail;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Phone
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 5, max = 50, message = "Phone number must be between 5 and 50 characters")
    @Column(name = "phone_number")
    private Phonenumber.PhoneNumber phoneNumber;

    protected ContactInfo() {
    } // Required by JPA

    public ContactInfo(UserEmail userEmail, boolean emailVerified, Phonenumber.PhoneNumber phoneNumber) {
        this.userEmail = userEmail;
        this.emailVerified = emailVerified;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return userEmail.getEmail();
    }

    String getUserEmail() {
        return userEmail.getEmail();
    }

}
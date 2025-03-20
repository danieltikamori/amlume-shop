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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Builder
@Embeddable
public class UserEmail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 5, max = 50)
    @Email
    @Column(nullable = false, unique = true, name = "email")
    private String email;

    public UserEmail(String email) {
        if (email == null || !email.matches("^[a-zA-Z0-9_+&*-]+(?:\\\\.[a-zA-Z0-9_+&*-])++@[a-zA-Z0-9-]+(?:\\\\.[a-zA-Z0-9-])++[a-zA-Z]{2,7}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email;
    }

    protected UserEmail() { // Required by JPA
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserEmail userEmail)) return false;
        return Objects.equals(email, userEmail.email);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email);
    }

    @Override
    public String toString() {
        return email;
    }
}
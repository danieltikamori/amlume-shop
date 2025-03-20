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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Builder
@Embeddable
public class Username implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 3, max = 20)
    @Column(nullable = false, unique = true, name = "username")
    String valueOfUsername;

    protected Username() {
    } // for JPA

    public Username(String value) {
        if (value == null || value.trim().length() < 3 || value.trim().length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }
        this.valueOfUsername = value;
    }

    public String getUsername() {
        return valueOfUsername;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Username username = (Username) o;
        return Objects.equals(valueOfUsername, username.valueOfUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueOfUsername);
    }


    @Override
    public String toString() {
        return valueOfUsername;
    }

}
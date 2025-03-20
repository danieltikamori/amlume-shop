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
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.amlu.shop.amlume_shop.config.ValidPassword;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
public class UserPassword implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ValidPassword
    @NotBlank
    @Size(min = 12, max = 255, message = "Password must be between 12 and 255 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, name = "password")
    private String password;  // This will store the Argon2id or other hash

    protected UserPassword() { // Required by JPA
    }

    public UserPassword(String password) {
//        if (password.isBlank()) {
//            throw new IllegalArgumentException("Password cannot be null or empty");
//        }
//        if (password.length() < 12 || password.length() > 255) {
//            throw new IllegalArgumentException("Password must be between 12 and 255 characters");
//        }
        this.password = password;
    }

String getPassword() {
    return password;
}


}
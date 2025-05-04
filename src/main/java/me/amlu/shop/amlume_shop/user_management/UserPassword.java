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
import me.amlu.shop.amlume_shop.security.config.ValidPassword;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserPassword implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ValidPassword
    @NotBlank
    @Size(min = 12, max = 255, message = "Password must be between 12 and 255 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, name = "userpassword")
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

    public static UserPasswordBuilder builder() {
        return new UserPasswordBuilder();
    }

    String getPassword() {
        return password;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserPassword other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        return Objects.equals(this$password, other$password);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserPassword;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "UserPassword(password=" + this.getPassword() + ")";
    }

    public static class UserPasswordBuilder {
        private @NotBlank
        @Size(min = 12, max = 255, message = "Password must be between 12 and 255 characters") String password;

        UserPasswordBuilder() {
        }

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public UserPasswordBuilder password(@NotBlank @Size(min = 12, max = 255, message = "Password must be between 12 and 255 characters") String password) {
            this.password = password;
            return this;
        }

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public UserPassword build() {
            return new UserPassword(this.password);
        }

        @Override
        public String toString() {
            return "UserPassword.UserPasswordBuilder(password=" + this.password + ")";
        }
    }
}
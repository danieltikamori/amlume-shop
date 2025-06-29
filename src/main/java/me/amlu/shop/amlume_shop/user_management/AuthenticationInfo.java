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

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AuthenticationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Embedded
    private Username username;

    @Embedded
    private UserPassword password;

    public AuthenticationInfo(@NotBlank @Size(min = 3, max = 20) Username username, String encode) {
    }

    public AuthenticationInfo(Username username, UserPassword password) {
        this.username = username;
        this.password = password;
    }

    protected AuthenticationInfo() {
    }

    public AuthenticationInfo(String username, UserPassword userPassword) {
        this.username = new Username(username);
        this.password = new UserPassword(userPassword.getPassword());
    }

    public static AuthenticationInfoBuilder builder() {
        return new AuthenticationInfoBuilder();
    }

    // --- Getters ---

    public String getUsername() {
        return username.getUsername();
    }

    public String getPassword() {
        return password.getPassword();
    }

    // --- End Getters ---

    // --- Modifier methods ---

    public void updatePassword(String encodedPassword) {
        this.password = new UserPassword(encodedPassword);
    }

    // --- End Modifier methods ---

    // --- equals() and hashCode() ---

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AuthenticationInfo other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (!Objects.equals(this$username, other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        return Objects.equals(this$password, other$password);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof AuthenticationInfo;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public static class AuthenticationInfoBuilder {
        private Username username;
        private UserPassword password;

        AuthenticationInfoBuilder() {
        }

        public AuthenticationInfoBuilder username(Username username) {
            this.username = username;
            return this;
        }

        public AuthenticationInfoBuilder password(UserPassword password) {
            this.password = password;
            return this;
        }

        public AuthenticationInfo build() {
            return new AuthenticationInfo(this.username, this.password);
        }

        @Override
        public String toString() {
            return "AuthenticationInfo.AuthenticationInfoBuilder(username=" + this.username + ", password=" + this.password + ")";
        }
    }
}

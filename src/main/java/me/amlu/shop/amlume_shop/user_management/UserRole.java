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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    private AppRole roleName;

    protected UserRole() { // Required by JPA
    }

    public UserRole(AppRole roleName) {
        this.roleName = roleName;
    }

//    public UserRole() {
//        this.roleName = null;
//    }

    public static UserRoleBuilder builder() {
        return new UserRoleBuilder();
    }

    public AppRole getRoleName() {
        return this.roleName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserRole other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$roleName = this.getRoleName();
        final Object other$roleName = other.getRoleName();
        return Objects.equals(this$roleName, other$roleName);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserRole;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "UserRole(roleName=" + this.getRoleName() + ")";
    }

    public static class UserRoleBuilder {
        private AppRole roleName;

        UserRoleBuilder() {
        }

        public UserRoleBuilder roleName(AppRole roleName) {
            this.roleName = roleName;
            return this;
        }

        public UserRole build() {
            return new UserRole(this.roleName);
        }

        @Override
        public String toString() {
            return "UserRole.UserRoleBuilder(roleName=" + this.roleName + ")";
        }
    }
}
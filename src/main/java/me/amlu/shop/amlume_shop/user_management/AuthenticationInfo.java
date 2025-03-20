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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public final class AuthenticationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Embedded
    Username username;

    @Embedded
    UserPassword password;

    public AuthenticationInfo(@NotBlank @Size(min = 3, max = 20) String username, String encode) {
    }

    public String getUsername() {
        return username.getUsername();
    }

    public String getPassword() {
        return password.getPassword();
    }

}
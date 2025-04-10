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
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Builder
@Embeddable
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor(force = true)
public class AccountStatus implements Serializable {

    @Column(name = "user_locked", nullable = false)
    @Builder.Default
    private final boolean userLocked = false;

    @Column(name = "lock_time")
    private final Instant lockTime;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private final int failedLoginAttempts = 0;

    @Column(name = "account_non_locked", nullable = false)
    @Builder.Default
    private final boolean accountNonLocked = true; // For account locking (e.g. for failed login attempts). Non locked = true

    @Column(name = "last_login_time")
    private final Instant lastLoginTime;

    @Column(name = "account_non_expired", nullable = false)
    @Builder.Default
    private final boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired")
    private final boolean credentialsNonExpired;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private final boolean enabled = true;

    @Column(name = "creation_time", nullable = false)
    private final Instant creationTime;

}
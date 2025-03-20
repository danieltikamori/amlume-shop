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
import jakarta.persistence.Embedded;
import lombok.*;

import java.io.Serializable;

@Builder
@Embeddable
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor(force = true)
public final class MfaInfo implements Serializable {

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private final boolean mfaEnabled = false;

    @Column(name = "mfa_enforced")
    private final boolean mfaEnforced;

    @Embedded
    private MfaQrCodeUrl mfaQrCodeUrl;

    @Embedded
    private MfaSecret mfaSecret;

    String getMfaQrCodeUrl() {
        return mfaQrCodeUrl.getMfaQrCodeUrlValue();
    }

    String getMfaSecret() {
        return mfaSecret.getMfaSecretValue();
    }

}
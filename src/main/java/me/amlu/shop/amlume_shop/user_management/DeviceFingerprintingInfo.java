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

import java.io.Serial;
import java.io.Serializable;

@Builder
@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor(force = true)
public class DeviceFingerprintingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "device_fingerprinting_enabled")
    private final boolean deviceFingerprintingEnabled;

    @Embedded
    private final UserDeviceFingerprints deviceFingerprints;

    protected DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = null;
    }

    protected DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled, UserDeviceFingerprints deviceFingerprints) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = deviceFingerprints;
    }

}
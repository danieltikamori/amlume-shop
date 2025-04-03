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
import java.util.Objects;

@Builder
@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class DeviceFingerprintingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "device_fingerprinting_enabled")
    private final boolean deviceFingerprintingEnabled;

    @Embedded
    private final UserDeviceFingerprints deviceFingerprints;

    public DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled, UserDeviceFingerprints deviceFingerprints) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = deviceFingerprints;
    }

    public DeviceFingerprintingInfo(boolean deviceFingerprintingEnabled) {
        this.deviceFingerprintingEnabled = deviceFingerprintingEnabled;
        this.deviceFingerprints = null;
    }

    // Required for JPA
    protected DeviceFingerprintingInfo() {
        this.deviceFingerprintingEnabled = false;
        this.deviceFingerprints = null;
    }

    public DeviceFingerprintingInfo enableFingerprinting() {
//        Objects.requireNonNull(deviceFingerprints, "deviceFingerprints cannot be null");
        return new DeviceFingerprintingInfo(true, deviceFingerprints);
    }

    public DeviceFingerprintingInfo disableFingerprinting() {
        return new DeviceFingerprintingInfo(false, null);
    }
}
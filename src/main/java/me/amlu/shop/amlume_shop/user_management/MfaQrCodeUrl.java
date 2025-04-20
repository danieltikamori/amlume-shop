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
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MfaQrCodeUrl implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "mfa_qr_code_url", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String mfaQrCodeUrlValue;

    protected MfaQrCodeUrl() { // Required by JPA
    }

    public MfaQrCodeUrl(String mfaQrCodeUrl) {
        if (mfaQrCodeUrl == null || mfaQrCodeUrl.trim().length() < 5 || mfaQrCodeUrl.trim().length() > 255) {
            throw new IllegalArgumentException("MfaQrCodeUrl must be between 5 and 255 characters");
        }
        this.mfaQrCodeUrlValue = mfaQrCodeUrl;
    }

    public String getMfaQrCodeUrlValue() {
        return Objects.requireNonNullElse(mfaQrCodeUrlValue, "Please try again");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MfaQrCodeUrl that)) return false;
        return Objects.equals(mfaQrCodeUrlValue, that.mfaQrCodeUrlValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mfaQrCodeUrlValue);
    }

    @Override
    public String toString() {
        return getMfaQrCodeUrlValue();
    }
}
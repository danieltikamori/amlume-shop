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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MfaSecret implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "mfa_secret", nullable = true)
    private String mfaSecretValue;

    protected MfaSecret() { // Required by JPA
    }

    public MfaSecret(String mfaSecretValue) {
        this.mfaSecretValue = mfaSecretValue;
    }

    public String getMfaSecretValue() {
        return mfaSecretValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MfaSecret mfaSecret)) return false;
        return Objects.equals(mfaSecretValue, mfaSecret.mfaSecretValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mfaSecretValue);
    }

    @Override
    public String toString() {
        return mfaSecretValue;
    }
}
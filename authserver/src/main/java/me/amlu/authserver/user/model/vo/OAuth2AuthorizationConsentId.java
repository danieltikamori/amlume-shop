/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OAuth2AuthorizationConsentId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "registered_client_id", length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", length = 200)
    private String principalName;

    public OAuth2AuthorizationConsentId(String registeredClientId, String principalName) {
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
    }

    public OAuth2AuthorizationConsentId() {
    }

    public String getRegisteredClientId() {
        return this.registeredClientId;
    }

    public void setRegisteredClientId(String registeredClientId) {
        this.registeredClientId = registeredClientId;
    }

    public String getPrincipalName() {
        return this.principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof OAuth2AuthorizationConsentId other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$registeredClientId = this.getRegisteredClientId();
        final Object other$registeredClientId = other.getRegisteredClientId();
        if (!Objects.equals(this$registeredClientId, other$registeredClientId))
            return false;
        final Object this$principalName = this.getPrincipalName();
        final Object other$principalName = other.getPrincipalName();
        return Objects.equals(this$principalName, other$principalName);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof OAuth2AuthorizationConsentId;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $registeredClientId = this.getRegisteredClientId();
        result = result * PRIME + ($registeredClientId == null ? 43 : $registeredClientId.hashCode());
        final Object $principalName = this.getPrincipalName();
        result = result * PRIME + ($principalName == null ? 43 : $principalName.hashCode());
        return result;
    }
}

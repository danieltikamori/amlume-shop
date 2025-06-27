/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.model;

import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class GeoLocationEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String location;
    private Instant timestamp;

    public GeoLocationEntry(String location, Instant timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public GeoLocationEntry() {
    }

    public String getLocation() {
        return this.location;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeoLocationEntry other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$location = this.getLocation();
        final Object other$location = other.getLocation();
        if (!Objects.equals(this$location, other$location)) return false;
        final Object this$timestamp = this.getTimestamp();
        final Object other$timestamp = other.getTimestamp();
        return Objects.equals(this$timestamp, other$timestamp);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GeoLocationEntry;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        return result;
    }

    public String toString() {
        return "IpMetadataEntity.GeoLocationEntry(location=" + this.getLocation() + ", timestamp=" + this.getTimestamp() + ")";
    }
}

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

import java.time.Instant;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class GeoLocationHistory {
    private final Queue<LocationEntry> locations;
    private static final int MAX_ENTRIES = 10;

    public GeoLocationHistory() {
        this.locations = new LinkedList<>();
    }

    public void addLocation(GeoLocation location) {
        if (location != null) {
            locations.add(new LocationEntry(location, Instant.now()));
            if (locations.size() > MAX_ENTRIES) {
                locations.poll();
            }
        }
    }

    public boolean hasRecentLocation() {
        return !locations.isEmpty();
    }

    public GeoLocation getLastLocation() {
        LocationEntry lastEntry = locations.peek();
        return lastEntry != null ? lastEntry.location() : null;
    }

    public Instant getLastTimestamp() {
        LocationEntry lastEntry = locations.peek();
        return lastEntry != null ? lastEntry.timestamp() : null;
    }

    public Queue<LocationEntry> getLocations() {
        return this.locations;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeoLocationHistory other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$locations = this.getLocations();
        final Object other$locations = other.getLocations();
        return Objects.equals(this$locations, other$locations);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GeoLocationHistory;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $locations = this.getLocations();
        result = result * PRIME + ($locations == null ? 43 : $locations.hashCode());
        return result;
    }

    public String toString() {
        return "GeoLocationHistory(locations=" + this.getLocations() + ")";
    }
}

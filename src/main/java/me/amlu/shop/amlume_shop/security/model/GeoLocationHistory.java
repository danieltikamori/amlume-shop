/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import lombok.Data;
import me.amlu.shop.amlume_shop.model.LocationEntry;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

@Data
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
        return lastEntry != null ? lastEntry.getLocation() : null;
    }

    public Instant getLastTimestamp() {
        LocationEntry lastEntry = locations.peek();
        return lastEntry != null ? lastEntry.getTimestamp() : null;
    }
}

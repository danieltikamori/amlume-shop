/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management;

import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

@Embeddable
public class CategoryStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Define common statuses as constants
    public static final CategoryStatus ACTIVE = new CategoryStatus("ACTIVE", true, null);
    public static final CategoryStatus INACTIVE = new CategoryStatus("INACTIVE", false, "Deactivated");

    private final String status;
    private final boolean active;
    private final String reason;

    // Constructor
    public CategoryStatus(String status, boolean active, String reason) {
        // Add validation if needed (e.g., status not blank)
        this.status = Objects.requireNonNull(status, "Status string cannot be null");
        this.active = active;
        this.reason = reason; // Reason can be null
    }

    // JPA constructor
    protected CategoryStatus() {
        this.status = null;
        this.reason = null;
        this.active = false;
    }

    // --- Getters ---
    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public String getReason() {
        return reason;
    }

    // --- equals/hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryStatus that = (CategoryStatus) o;
        return active == that.active && Objects.equals(status, that.status) && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, active, reason);
    }

    // --- toString ---
    @Override
    public String toString() {
        return new StringJoiner(", ", CategoryStatus.class.getSimpleName() + "[", "]")
                .add("status='" + status + "'")
                .add("active=" + active)
                .add("reason='" + reason + "'")
                .toString();
    }
}
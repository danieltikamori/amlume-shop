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
public class HierarchyLevel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int level; // Keep final
    private final String path; // Keep final

    // Constructor
    public HierarchyLevel(int level, String path) {
        Objects.requireNonNull(path, "Path cannot be null");
        if (level < 0) {
            throw new IllegalArgumentException("Level must be greater than or equal to 0");
        }
        this.level = level;
        this.path = path;
    }

    // JPA constructor
    protected HierarchyLevel() {
        this.level = 0; // Default level
        this.path = "?"; // Default path placeholder
    }

    // --- Getters ---
    public int getLevel() {
        return level;
    }

    // Return the actual path field
    public String getPath() {
        return path;
    }

    // --- equals/hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HierarchyLevel that = (HierarchyLevel) o;
        return level == that.level && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, path);
    }

    // --- toString ---
    @Override
    public String toString() {
        return new StringJoiner(", ", HierarchyLevel.class.getSimpleName() + "[", "]")
                .add("level=" + level)
                .add("path='" + path + "'")
                .toString();
    }
}
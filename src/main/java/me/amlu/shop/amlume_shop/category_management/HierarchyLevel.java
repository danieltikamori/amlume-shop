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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Embeddable
@ToString
@EqualsAndHashCode
public class HierarchyLevel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final int level;
    private final String path;

    public HierarchyLevel(int level, String path) {
        Objects.requireNonNull(path, "Path cannot be null");
        if (level < 0) {
            throw new IllegalArgumentException("Level must be greater than or equal to 0");
        }
        this.level = level;
        this.path = path;
    }

    // Required for JPA
    protected HierarchyLevel() {
        this.level = 0;
        this.path = null;
    }

    public String getPath() {
        return "0".repeat(Math.max(0, level));
    }

}
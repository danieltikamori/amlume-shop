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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class HierarchyLevel {
    @Column(name = "level")
    private final int level;

    public HierarchyLevel(int level, String string) {
        if (level < 0) {
            throw new IllegalArgumentException("Hierarchy level cannot be negative");
        }
        if (string == null || string.trim().isEmpty()) {
            throw new IllegalArgumentException("Hierarchy level path cannot be null or empty");
        }
        if (!string.matches("0+")) {
            throw new IllegalArgumentException("Hierarchy level path must consist of zeros only");
        }
//        if (string.length() != level) {
//            throw new IllegalArgumentException("Hierarchy level path length must match the level");
//        }
        this.level = level;
    }

//    public int getValue() {
//        return level;
//    }

    public String getPath() {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < level; i++) {
            path.append("0");
        }
        return path.toString();
    }

    public int getLevel() {
        return level;
    }
}
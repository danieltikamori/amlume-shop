/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.enums;

import java.util.Arrays;

/**
 * Enumerates supported file types with their common extensions.
 */
public enum FileTypeEnum {

    CSV(".csv"),
    EXCEL(".xlsx"),
    WORD(".docx"),
    ZIP(".zip");

    private final String value;

    FileTypeEnum(String value) {
        this.value = value;
    }

    /**
     * Returns the file extension associated with this file type (e.g., ".csv").
     *
     * @return The file extension string.
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Finds a FileTypeEnum by its extension value.
     *
     * @param value The extension string (e.g., ".csv").
     * @return The matching FileTypeEnum.
     * @throws IllegalArgumentException if no matching enum constant is found.
     */
    public static FileTypeEnum fromValue(String value) {
        return Arrays.stream(FileTypeEnum.values())
                .filter(enumVal -> enumVal.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown file type value: " + value));
    }
}
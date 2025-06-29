/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostalCodeUtils {
    private static final Map<String, String> POSTAL_CODE_REGEX_MAP = new HashMap<>();

    static {
        POSTAL_CODE_REGEX_MAP.put("JP", "^[0-9]{7}$"); // Japan
        POSTAL_CODE_REGEX_MAP.put("KR", "^[0-9]{5}$"); // Korea
        POSTAL_CODE_REGEX_MAP.put("IN", "^[1-9][0-9]{5}$"); // India
        POSTAL_CODE_REGEX_MAP.put("PH", "^[0-9]{4}$"); // Philippines
        POSTAL_CODE_REGEX_MAP.put("SG", "^[0-9]{6}$"); // Singapore
        POSTAL_CODE_REGEX_MAP.put("AU", "^[0-9]{4}$"); // Australia
        POSTAL_CODE_REGEX_MAP.put("NZ", "^[0-9]{4}$"); // New Zealand
        POSTAL_CODE_REGEX_MAP.put("DE", "^[0-9]{5}$"); // Germany
        POSTAL_CODE_REGEX_MAP.put("FR", "^[0-9]{5}$"); // France
        POSTAL_CODE_REGEX_MAP.put("IT", "^[0-9]{5}$"); // Italy
        POSTAL_CODE_REGEX_MAP.put("ES", "^[0-9]{5}$"); // Spain
        POSTAL_CODE_REGEX_MAP.put("UK", "^[A-Za-z]{1,2}[0-9][A-Za-z0-9]? [0-9][A-Za-z]{2}$"); // UK
        POSTAL_CODE_REGEX_MAP.put("CA", "^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$"); // Canada
        POSTAL_CODE_REGEX_MAP.put("US", "^\\d{5}(-\\d{4})?$"); // US ZIP code
        POSTAL_CODE_REGEX_MAP.put("MX", "^[0-9]{5}$"); // Mexico
        POSTAL_CODE_REGEX_MAP.put("BR", "^[0-9]{5}-[0-9]{3}$"); // Brazil

        // ... add more countries and their regex patterns ...
    }

    public static String getPostalCodeRegex(String countryCode) {
        return POSTAL_CODE_REGEX_MAP.getOrDefault(countryCode.toUpperCase(Locale.ROOT), ".+"); // Default to allow any if not found
    }

    private PostalCodeUtils() {
        // Private constructor to prevent instantiation
    }
}

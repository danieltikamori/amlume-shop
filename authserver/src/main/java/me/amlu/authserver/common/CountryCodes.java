/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

import java.util.HashMap;
import java.util.Map;

/**
 * ISO 3166-1 alpha-2 country codes used throughout the application.
 */
public final class CountryCodes {
    // Common country codes
    public static final String US = "US"; // United States
    public static final String CA = "CA"; // Canada
    public static final String GB = "GB"; // United Kingdom
    public static final String AU = "AU"; // Australia
    public static final String DE = "DE"; // Germany
    public static final String FR = "FR"; // France
    public static final String JP = "JP"; // Japan
    public static final String CN = "CN"; // China
    public static final String BR = "BR"; // Brazil
    public static final String IN = "IN"; // India

    // Map of country codes to names
    private static final Map<String, String> COUNTRY_NAMES = new HashMap<>();

    static {
        // Initialize map with common countries
        COUNTRY_NAMES.put(US, "United States");
        COUNTRY_NAMES.put(CA, "Canada");
        COUNTRY_NAMES.put(GB, "United Kingdom");
        COUNTRY_NAMES.put(AU, "Australia");
        COUNTRY_NAMES.put(DE, "Germany");
        COUNTRY_NAMES.put(FR, "France");
        COUNTRY_NAMES.put(JP, "Japan");
        COUNTRY_NAMES.put(CN, "China");
        COUNTRY_NAMES.put(BR, "Brazil");
        COUNTRY_NAMES.put(IN, "India");
        // Add more as needed
    }

    private CountryCodes() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the country name for a given country code.
     *
     * @param countryCode The ISO 3166-1 alpha-2 country code
     * @return The country name, or the code itself if not found
     */
    public static String getCountryName(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return COUNTRY_NAMES.getOrDefault(countryCode.toUpperCase(), countryCode);
    }

    /**
     * Checks if a country code is valid.
     *
     * @param countryCode The country code to validate
     * @return true if the country code is valid
     */
    public static boolean isValidCountryCode(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return false;
        }
        return COUNTRY_NAMES.containsKey(countryCode.toUpperCase());
    }
}

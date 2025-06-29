/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.commons;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for enum operations.
 */
public final class EnumUtils {

    private EnumUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Safely converts a string to an enum value.
     *
     * @param <T>          The enum type
     * @param enumClass    The enum class
     * @param value        The string value
     * @param defaultValue The default value if conversion fails
     * @return The enum value or default
     */
    public static <T extends Enum<T>> T getEnumFromString(Class<T> enumClass, String value, T defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Safely converts a string to an enum value.
     *
     * @param <T>       The enum type
     * @param enumClass The enum class
     * @param value     The string value
     * @return Optional containing the enum value if conversion succeeds
     */
    public static <T extends Enum<T>> Optional<T> getEnumFromString(Class<T> enumClass, String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds an enum value by a predicate.
     *
     * @param <T>       The enum type
     * @param enumClass The enum class
     * @param predicate The predicate to match
     * @return Optional containing the first matching enum value
     */
    public static <T extends Enum<T>> Optional<T> findByPredicate(Class<T> enumClass, Predicate<T> predicate) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(predicate)
                .findFirst();
    }

    /**
     * Finds an enum value by a property.
     *
     * @param <T>               The enum type
     * @param <V>               The property type
     * @param enumClass         The enum class
     * @param propertyExtractor Function to extract the property
     * @param value             The property value to match
     * @return Optional containing the first matching enum value
     */
    public static <T extends Enum<T>, V> Optional<T> findByProperty(
            Class<T> enumClass, Function<T, V> propertyExtractor, V value) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> value.equals(propertyExtractor.apply(e)))
                .findFirst();
    }

    /**
     * Checks if a string is a valid enum value.
     *
     * @param <T>       The enum type
     * @param enumClass The enum class
     * @param value     The string value
     * @return true if the string is a valid enum value
     */
    public static <T extends Enum<T>> boolean isValidEnum(Class<T> enumClass, String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        try {
            // Assign the result to a variable, even if it's not used further.
            // This satisfies the Error Prone check.
            @SuppressWarnings("unused") // Suppress "unused" warning for this specific variable
            T enumConstant = Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

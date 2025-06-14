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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Utility methods for JSON operations.
 */
public final class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = createObjectMapper();

    private JsonUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Creates and configures an ObjectMapper.
     *
     * @return Configured ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Converts an object to a JSON string.
     *
     * @param object The object to convert
     * @return JSON string representation of the object
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return "{}";
        }
    }

    /**
     * Converts a JSON string to an object.
     *
     * @param json  The JSON string
     * @param clazz The class to convert to
     * @param <T>   The type of the class
     * @return The converted object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("Error converting JSON to object", e);
            return null;
        }
    }

    /**
     * Converts a JSON string to a Map.
     *
     * @param json The JSON string
     * @return Map representation of the JSON
     */
    public static Map<String, Object> jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            log.error("Error converting JSON to Map", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Converts a JSON string to a JsonNode.
     *
     * @param json The JSON string
     * @return JsonNode representation of the JSON
     */
    public static JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            log.error("Error parsing JSON", e);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Checks if a string is valid JSON.
     *
     * @param json The string to check
     * @return true if the string is valid JSON
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the ObjectMapper instance.
     *
     * @return The ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

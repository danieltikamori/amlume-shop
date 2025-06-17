/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.session.data.mongo.JacksonMongoSessionConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LoggingJacksonMongoSessionConverter extends JacksonMongoSessionConverter {
    private static final Logger log = LoggerFactory.getLogger(LoggingJacksonMongoSessionConverter.class);
    private final ObjectMapper objectMapper;

    public LoggingJacksonMongoSessionConverter(ObjectMapper objectMapper) {
        super(objectMapper); // Pass to super
        this.objectMapper = objectMapper;
        log.info("LoggingJacksonMongoSessionConverter initialized with ObjectMapper hash: {}", System.identityHashCode(objectMapper));
    }

    //        @Override
    @NonNull
    protected Map<String, Object> deserializeAttributes(Document attributes) {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        // Log the entire attributes document as JSON
        log.debug("MONGO_SESSION_DESERIALIZE: Attempting to deserialize session attributes from MongoDB Document: {}", attributes.toJson());

        Map<String, Object> result = new HashMap<>(attributes.size());
        attributes.forEach((key, value) -> {
            Object deserializedValue = null;
            try {
                String valueToLog;
                String valueType;

                if (value instanceof Document) {
                    valueToLog = ((Document) value).toJson();
                    valueType = "org.bson.Document";
                } else {
                    valueToLog = String.valueOf(value);
                    valueType = (value != null ? value.getClass().getName() : "null");
                }
                log.debug("MONGO_SESSION_DESERIALIZE: Attribute key='{}', BSON value='{}', BSON type='{}'", key, valueToLog, valueType);

                deserializedValue = this.objectMapper.convertValue(value, Object.class);

                if (deserializedValue != null) {
                    log.debug("MONGO_SESSION_DESERIALIZE: Successfully deserialized attribute key='{}' to Java type: {}", key, deserializedValue.getClass().getName());
                    if ("SPRING_SECURITY_CONTEXT".equals(key)) {
                        // Log more details for the critical attribute
                        String deserializedValueString = deserializedValue.toString();
                        log.warn("MONGO_SESSION_DESERIALIZE: Deserialized SPRING_SECURITY_CONTEXT attribute is of Java type: {}. Value (truncated): {}",
                                deserializedValue.getClass().getName(),
                                deserializedValueString.substring(0, Math.min(500, deserializedValueString.length())));
                    }
                } else {
                    log.warn("MONGO_SESSION_DESERIALIZE: Deserialized attribute key='{}' resulted in null.", key);
                }
            } catch (Exception e) {
                log.error("MONGO_SESSION_DESERIALIZE: Error deserializing attribute key='{}', BSON value='{}': {}", key, value, e.getMessage(), e);
                // Decide how to handle - skip attribute, throw, etc.
                // For now, let's put null or skip, to see if other attributes work.
                // Or rethrow to see the exact failure point.
                // throw new RuntimeException("Failed to deserialize attribute " + key, e);
            }
            result.put(key, deserializedValue);
        });
        log.debug("MONGO_SESSION_DESERIALIZE: Finished deserializing attributes. Result map size: {}", result.size());
        return result;
    }
}

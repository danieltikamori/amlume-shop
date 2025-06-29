/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.amlu.authserver.webauthn.CustomAuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Deserializer for AuthenticationExtensionsClientInputs.
 */
public class AuthenticationExtensionsClientInputsDeserializer extends JsonDeserializer<AuthenticationExtensionsClientInputs> {

    @Override
    public AuthenticationExtensionsClientInputs deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Map<String, Object> extensions = new HashMap<>();

        if (node != null && !node.isNull()) {
            // Refactored: Use properties() instead of fields()
            // Option 1: Using a traditional iterator loop (similar to original logic)
            // Iterator<Map.Entry<String, JsonNode>> properties = node.properties(); // properties() returns Iterator
            // while (properties.hasNext()) {
            //     Map.Entry<String, JsonNode> entry = properties.next();
            //     JsonNode value = entry.getValue();
            //     if (value.isBoolean()) {
            //         extensions.put(entry.getKey(), value.asBoolean());
            //     } else if (value.isNumber()) {
            //         extensions.put(entry.getKey(), value.asLong());
            //     } else if (value.isTextual()) {
            //         extensions.put(entry.getKey(), value.asText());
            //     }
            // }

            // Option 2: Using Java 8 Streams (more modern and often preferred)
            // properties() returns an Iterator, which can be converted to a Stream using StreamSupport.stream
            node.properties().stream()
                    .forEach(entry -> {
                        JsonNode value = entry.getValue();
                        if (value.isBoolean()) {
                            extensions.put(entry.getKey(), value.asBoolean());
                        } else if (value.isNumber()) {
                            extensions.put(entry.getKey(), value.asLong());
                        } else if (value.isTextual()) {
                            extensions.put(entry.getKey(), value.asText());
                        }
                    });
        }

        final CustomAuthenticationExtensionsClientInputs customInputs =
                new CustomAuthenticationExtensionsClientInputs(extensions);

        // Use dynamic proxy to implement the interface
        return (AuthenticationExtensionsClientInputs) Proxy.newProxyInstance(
                AuthenticationExtensionsClientInputs.class.getClassLoader(),
                new Class<?>[]{AuthenticationExtensionsClientInputs.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("getExtensions".equals(methodName)) {
                        return customInputs.getExtensions();
                    } else if ("getInputs".equals(methodName)) {
                        return customInputs.getExtensions();
                    } else {
                        return method.invoke(customInputs, args);
                    }
                }
        );
    }
}

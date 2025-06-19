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
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientOutputs;
import me.amlu.authserver.passkey.model.SimpleAuthenticationExtensionsClientOutputs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom deserializer for AuthenticationExtensionsClientOutputs interface.
 */
public class AuthenticationExtensionsClientOutputsDeserializer extends JsonDeserializer<AuthenticationExtensionsClientOutputs> {

    @Override
    public AuthenticationExtensionsClientOutputs deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Map<String, Object> extensions = new HashMap<>();

        // Convert JsonNode to Map
        if (node != null && !node.isNull()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();

                if (value.isBoolean()) {
                    extensions.put(entry.getKey(), value.asBoolean());
                } else if (value.isNumber()) {
                    extensions.put(entry.getKey(), value.asLong());
                } else if (value.isTextual()) {
                    extensions.put(entry.getKey(), value.asText());
                }
            }
        }

        // Use the existing SimpleAuthenticationExtensionsClientOutputs class
        return new SimpleAuthenticationExtensionsClientOutputs(extensions);
    }
}

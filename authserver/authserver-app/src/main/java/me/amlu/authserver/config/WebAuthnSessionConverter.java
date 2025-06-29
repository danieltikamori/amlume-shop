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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.stereotype.Component;

/**
 * Converter for WebAuthn objects to handle serialization/deserialization in Redis sessions.
 * This component provides methods to convert WebAuthn objects to/from JSON strings
 * using a specialized ObjectMapper configured for WebAuthn objects.
 */
@Component
public class WebAuthnSessionConverter {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnSessionConverter.class);
    private final ObjectMapper webAuthnObjectMapper;

    public WebAuthnSessionConverter(@Qualifier("webAuthnSessionObjectMapper") ObjectMapper webAuthnObjectMapper) {
        this.webAuthnObjectMapper = webAuthnObjectMapper;
        log.info("WebAuthnSessionConverter initialized with specialized WebAuthn ObjectMapper");
    }

    /**
     * Converts PublicKeyCredentialCreationOptions to a JSON string.
     *
     * @param options The WebAuthn credential creation options
     * @return JSON string representation
     */
    public String convertCreationOptionsToJson(PublicKeyCredentialCreationOptions options) {
        try {
            return webAuthnObjectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PublicKeyCredentialCreationOptions", e);
            throw new RuntimeException("Failed to serialize WebAuthn creation options", e);
        }
    }

    /**
     * Converts a JSON string to PublicKeyCredentialCreationOptions.
     *
     * @param json JSON string representation
     * @return The WebAuthn credential creation options
     */
    public PublicKeyCredentialCreationOptions convertJsonToCreationOptions(String json) {
        try {
            return webAuthnObjectMapper.readValue(json, PublicKeyCredentialCreationOptions.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PublicKeyCredentialCreationOptions from JSON: {}", json, e);
            throw new RuntimeException("Failed to deserialize WebAuthn creation options", e);
        }
    }

    /**
     * Converts PublicKeyCredentialRequestOptions to a JSON string.
     *
     * @param options The WebAuthn credential request options
     * @return JSON string representation
     */
    public String convertRequestOptionsToJson(PublicKeyCredentialRequestOptions options) {
        try {
            return webAuthnObjectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PublicKeyCredentialRequestOptions", e);
            throw new RuntimeException("Failed to serialize WebAuthn request options", e);
        }
    }

    /**
     * Converts a JSON string to PublicKeyCredentialRequestOptions.
     *
     * @param json JSON string representation
     * @return The WebAuthn credential request options
     */
    public PublicKeyCredentialRequestOptions convertJsonToRequestOptions(String json) {
        try {
            return webAuthnObjectMapper.readValue(json, PublicKeyCredentialRequestOptions.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PublicKeyCredentialRequestOptions from JSON: {}", json, e);
            throw new RuntimeException("Failed to deserialize WebAuthn request options", e);
        }
    }
}

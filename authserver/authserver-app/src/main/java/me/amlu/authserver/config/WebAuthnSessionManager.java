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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

/**
 * Manager for handling WebAuthn objects in the HTTP session.
 * This component provides methods to store and retrieve WebAuthn objects
 * in the session using string-based serialization to avoid Redis serialization issues.
 */
@Component
public class WebAuthnSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnSessionManager.class);
    private static final String CREATION_OPTIONS_KEY = "WEBAUTHN_CREATION_OPTIONS";
    private static final String REQUEST_OPTIONS_KEY = "WEBAUTHN_REQUEST_OPTIONS";

    private final WebAuthnSessionConverter converter;

    public WebAuthnSessionManager(WebAuthnSessionConverter converter) {
        this.converter = converter;
        log.info("WebAuthnSessionManager initialized");
    }

    /**
     * Stores PublicKeyCredentialCreationOptions in the session as a JSON string.
     *
     * @param options The WebAuthn credential creation options
     */
    public void storeCreationOptions(PublicKeyCredentialCreationOptions options) {
        HttpSession session = getSession();
        String json = converter.convertCreationOptionsToJson(options);
        session.setAttribute(CREATION_OPTIONS_KEY, json);
        log.debug("Stored PublicKeyCredentialCreationOptions in session");
    }

    /**
     * Retrieves PublicKeyCredentialCreationOptions from the session.
     *
     * @return The WebAuthn credential creation options, or null if not found
     */
    public PublicKeyCredentialCreationOptions getCreationOptions() {
        HttpSession session = getSession();
        String json = (String) session.getAttribute(CREATION_OPTIONS_KEY);
        if (json == null) {
            log.debug("No PublicKeyCredentialCreationOptions found in session");
            return null;
        }
        return converter.convertJsonToCreationOptions(json);
    }

    /**
     * Stores PublicKeyCredentialRequestOptions in the session as a JSON string.
     *
     * @param options The WebAuthn credential request options
     */
    public void storeRequestOptions(PublicKeyCredentialRequestOptions options) {
        HttpSession session = getSession();
        String json = converter.convertRequestOptionsToJson(options);
        session.setAttribute(REQUEST_OPTIONS_KEY, json);
        log.debug("Stored PublicKeyCredentialRequestOptions in session");
    }

    /**
     * Retrieves PublicKeyCredentialRequestOptions from the session.
     *
     * @return The WebAuthn credential request options, or null if not found
     */
    public PublicKeyCredentialRequestOptions getRequestOptions() {
        HttpSession session = getSession();
        String json = (String) session.getAttribute(REQUEST_OPTIONS_KEY);
        if (json == null) {
            log.debug("No PublicKeyCredentialRequestOptions found in session");
            return null;
        }
        return converter.convertJsonToRequestOptions(json);
    }

    /**
     * Removes WebAuthn options from the session.
     */
    public void clearOptions() {
        HttpSession session = getSession();
        session.removeAttribute(CREATION_OPTIONS_KEY);
        session.removeAttribute(REQUEST_OPTIONS_KEY);
        log.debug("Cleared WebAuthn options from session");
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(true);
    }
}

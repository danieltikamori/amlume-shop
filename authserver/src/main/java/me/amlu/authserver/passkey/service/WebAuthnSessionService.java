/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.passkey.service;

import me.amlu.authserver.config.WebAuthnSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.stereotype.Service;

/**
 * Service for managing WebAuthn session data.
 * This service provides methods to store and retrieve WebAuthn objects
 * in the session using the WebAuthnSessionManager.
 */
@Service
public class WebAuthnSessionService {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnSessionService.class);
    private final WebAuthnSessionManager sessionManager;

    public WebAuthnSessionService(WebAuthnSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        log.info("WebAuthnSessionService initialized");
    }

    /**
     * Stores WebAuthn credential creation options in the session.
     *
     * @param options The WebAuthn credential creation options
     */
    public void storeCreationOptions(PublicKeyCredentialCreationOptions options) {
        sessionManager.storeCreationOptions(options);
        log.debug("Stored WebAuthn creation options in session");
    }

    /**
     * Retrieves WebAuthn credential creation options from the session.
     *
     * @return The WebAuthn credential creation options, or null if not found
     */
    public PublicKeyCredentialCreationOptions getCreationOptions() {
        PublicKeyCredentialCreationOptions options = sessionManager.getCreationOptions();
        if (options != null) {
            log.debug("Retrieved WebAuthn creation options from session");
        }
        return options;
    }

    /**
     * Stores WebAuthn credential request options in the session.
     *
     * @param options The WebAuthn credential request options
     */
    public void storeRequestOptions(PublicKeyCredentialRequestOptions options) {
        sessionManager.storeRequestOptions(options);
        log.debug("Stored WebAuthn request options in session");
    }

    /**
     * Retrieves WebAuthn credential request options from the session.
     *
     * @return The WebAuthn credential request options, or null if not found
     */
    public PublicKeyCredentialRequestOptions getRequestOptions() {
        PublicKeyCredentialRequestOptions options = sessionManager.getRequestOptions();
        if (options != null) {
            log.debug("Retrieved WebAuthn request options from session");
        }
        return options;
    }

    /**
     * Clears WebAuthn options from the session.
     */
    public void clearOptions() {
        sessionManager.clearOptions();
        log.debug("Cleared WebAuthn options from session");
    }
}

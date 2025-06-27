/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Controller advice for handling WebAuthn/Passkey related exceptions.
 * This class provides global exception handlers for WebAuthn operations.
 */
@ControllerAdvice
public class PasskeyControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(PasskeyControllerAdvice.class);

    /**
     * Handles JSON processing exceptions that may occur during WebAuthn object serialization/deserialization.
     *
     * @param ex The JsonProcessingException
     * @return A ResponseEntity with an error message
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(JsonProcessingException ex) {
        log.error("JSON processing error in WebAuthn operation", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing WebAuthn data: " + ex.getMessage());
    }

    /**
     * Handles WebAuthn-specific exceptions.
     *
     * @param ex The WebAuthn-related Exception
     * @return A ResponseEntity with an error message
     */
    @ExceptionHandler(com.webauthn4j.util.exception.WebAuthnException.class)
    public ResponseEntity<String> handleWebAuthnException(Exception ex) {
        log.error("Error in WebAuthn operation", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("WebAuthn operation failed: " + ex.getMessage());
    }
}

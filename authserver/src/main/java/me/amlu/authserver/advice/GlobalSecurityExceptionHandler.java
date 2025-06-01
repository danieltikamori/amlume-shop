/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.advice;

import me.amlu.authserver.exceptions.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for security-related exceptions within the application.
 * This class uses Spring's {@link ControllerAdvice} to intercept exceptions thrown
 * by controllers and provide a centralized way to handle them, returning consistent
 * and informative error responses to the client while logging detailed information
 * internally.
 *
 * <p>It is particularly focused on handling exceptions related to security operations
 * like encryption and decryption, ensuring that sensitive details are not exposed
 * to the client in error responses.</p>
 */
@ControllerAdvice
public class GlobalSecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalSecurityExceptionHandler.class);

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<Object> handleEncryptionException(
            EncryptionException ex, WebRequest request) {

        log.error("Encryption/Decryption error occurred: {}. Request: {}", ex.getMessage(), request.getDescription(false), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        // Provide a generic message to the client. Do NOT expose internal details.
        body.put("message", "A security processing error occurred. Please try again later or contact support if the issue persists.");
        body.put("path", request.getDescription(false).replace("uri=", "")); // Clean up path

        // It's crucial NOT to include ex.getMessage() or ex.getCause() in the response to the client
        // as it might leak sensitive information about the encryption process or data.

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // You can add handlers for other specific security-related exceptions here.
    // For example, if certain operations should return a 400 Bad Request instead of 500
    // for specific types of decryption failures that indicate bad input rather than system error.
}

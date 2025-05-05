/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.handler;

import me.amlu.shop.amlume_shop.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class AuthenticationExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger authLogger = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);
    private static final Logger log = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);

    // Custom Error Response Class
    public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
    }

    // Centralized Exception Handling Method
    private ResponseEntity<ErrorResponse> handleException(Exception ex, HttpStatus status, WebRequest request) {
        authLogger.error("Exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).substring(4)
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    // Specific Exception Handlers
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex, WebRequest request) {
        return handleException(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex, WebRequest request) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        return handleException(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        return handleException(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex, WebRequest request) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex, WebRequest request) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(InvalidCaptchaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCaptchaException(InvalidCaptchaException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttemptsException(TooManyAttemptsException ex, WebRequest request) {
        return handleException(ex, HttpStatus.TOO_MANY_REQUESTS, request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(LockedException ex, WebRequest request) {
        return handleException(ex, HttpStatus.LOCKED, request);
    }

    // Grouping MFA Exceptions
    @ExceptionHandler(MfaException.class)
    public ResponseEntity<ErrorResponse> handleMfaExceptions(MfaException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, request);
    }

    // Generic Exception Handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        authLogger.error("Unexpected exception: {}", ex.getMessage(), ex);
        return handleException(new RuntimeException("An unexpected error occurred."), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}

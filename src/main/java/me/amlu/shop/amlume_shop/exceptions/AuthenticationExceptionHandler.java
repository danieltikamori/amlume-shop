/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthenticationExceptionHandler {
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                           .body("User not found");
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<String> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCaptchaException.class)
    public ResponseEntity<String> handleInvalidCaptchaException(InvalidCaptchaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<String> handleTooManyAttemptsException(TooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<String> handleLockedException(LockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaTokenNotFoundException.class)
    public ResponseEntity<String> handleMfaTokenNotFoundException(MfaTokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaTokenException.class)
    public ResponseEntity<String> handleInvalidMfaTokenException(InvalidMfaTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaEnforcementException.class)
    public ResponseEntity<String> handleMfaEnforcementException(MfaEnforcementException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaSetupException.class)
    public ResponseEntity<String> handleMfaSetupException(MfaSetupException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaVerificationException.class)
    public ResponseEntity<String> handleMfaVerificationException(MfaVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaChallengeException.class)
    public ResponseEntity<String> handleMfaChallengeException(MfaChallengeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaRecoveryCodeException.class)
    public ResponseEntity<String> handleMfaRecoveryCodeException(MfaRecoveryCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaRecoveryCodeUsageException.class)
    public ResponseEntity<String> handleMfaRecoveryCodeUsageException(MfaRecoveryCodeUsageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(MfaRecoveryCodeGenerationException.class)
    public ResponseEntity<String> handleMfaRecoveryCodeGenerationException(MfaRecoveryCodeGenerationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaCodeException.class)
    public ResponseEntity<String> handleInvalidMfaCodeException(InvalidMfaCodeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaMethodException.class)
    public ResponseEntity<String> handleInvalidMfaMethodException(InvalidMfaMethodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaEnforcementException.class)
    public ResponseEntity<String> handleInvalidMfaEnforcementException(InvalidMfaEnforcementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaSetupException.class)
    public ResponseEntity<String> handleInvalidMfaSetupException(InvalidMfaSetupException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaVerificationException.class)
    public ResponseEntity<String> handleInvalidMfaVerificationException(InvalidMfaVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaChallengeException.class)
    public ResponseEntity<String> handleInvalidMfaChallengeException(InvalidMfaChallengeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeException(InvalidMfaRecoveryCodeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeUsageException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeUsageException(InvalidMfaRecoveryCodeUsageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeGenerationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeGenerationException(InvalidMfaRecoveryCodeGenerationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeDeletionException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeDeletionException(InvalidMfaRecoveryCodeDeletionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeExpirationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeExpirationException(InvalidMfaRecoveryCodeExpirationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeInvalidationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeInvalidationException(InvalidMfaRecoveryCodeInvalidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeRecoveryException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeRecoveryException(InvalidMfaRecoveryCodeRecoveryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeResetException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeResetException(InvalidMfaRecoveryCodeResetException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeVerificationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeVerificationException(InvalidMfaRecoveryCodeVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeListException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeListException(InvalidMfaRecoveryCodeListException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeCountException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeCountException(InvalidMfaRecoveryCodeCountException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeCreationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeCreationException(InvalidMfaRecoveryCodeCreationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeDeletionException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeDeletionException(InvalidMfaRecoveryCodeDeletionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeExpirationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeExpirationException(InvalidMfaRecoveryCodeExpirationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidMfaRecoveryCodeInvalidationException.class)
    public ResponseEntity<String> handleInvalidMfaRecoveryCodeInvalidationException(InvalidMfaRecoveryCodeInvalidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                           .body(ex.getMessage());
    }


}

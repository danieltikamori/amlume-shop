/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

//    @org.springframework.web.bind.annotation.ExceptionHandler(ResponseStatusException.class)
//    public String handleResponseStatusException(ResponseStatusException e) {
//        return e.getReason();
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> MethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> response = new HashMap<>();
          e.getBindingResult().getAllErrors().forEach(error -> {
    String fieldName = ((FieldError)error).getField();
    String errorMessage = error.getDefaultMessage();
    response.put(fieldName, errorMessage);
  });
         return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);

//        e.getBindingResult().getFieldErrors().forEach(error -> {
//            response.put(error.getField(), error.getDefaultMessage());
//        });
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<String> resourceNotFoundException(ResourceNotFoundException e) {
        String message = e.getMessage();
        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<String> apiException(APIException e) {
        String message = e.getMessage();
        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
    }


}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management.controller;

import me.amlu.shop.amlume_shop.user_management.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.dto.GetUserResponse;
import me.amlu.shop.amlume_shop.security.aspect.RequiresAuthentication;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    @RequiresAuthentication
    public ResponseEntity<GetUserResponse> getUserProfile() {
        // userService.getCurrentUser() should correctly fetch the user
        // based on the authServerSubjectId from the OIDC token.
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(GetUserResponse.fromUser(currentUser));
    }

    // This endpoint might be redundant given /api/profile, but if kept, it needs correction.
    // It assumes the Authentication object is populated by Spring Security after OIDC/JWT validation.
    @GetMapping("/user") // Changed to @GetMapping as it's a read operation
    public User getUserDetailsAfterLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Attempt to access /api/user without valid authentication.");
            // Depending on requirements, you might return HTTP 401 or null
            return null;
        }

        // After OIDC login and JWT validation by the resource server,
        // authentication.getName() will be the 'sub' claim from the JWT,
        // which is the authServerSubjectId.
        String authServerSubjectId = authentication.getName();
        log.debug("Fetching user details for /api/user endpoint using authServerSubjectId: {}", authServerSubjectId);

        // Ensure UserRepository has findByAuthServerSubjectId method
        Optional<User> optionalUser = userRepository.findByAuthServerSubjectId(authServerSubjectId);

        if (optionalUser.isEmpty()) {
            log.warn("No local user found for authServerSubjectId: {} when accessing /api/user endpoint. This might indicate a user provisioning issue.", authServerSubjectId);
        }
        return optionalUser.orElse(null);
    }

//    @PostMapping("admin/user/")
//    public ResponseEntity<UserRegistrationRequest> registerUser(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest, @PathVariable String password) throws ProductAlreadyExistsException {
//
//        User registeredUser = userService.registerUser(userRegistrationRequest);
//        return new ResponseEntity<>(userResponse, null, HttpStatus.CREATED);
//
//    }
}

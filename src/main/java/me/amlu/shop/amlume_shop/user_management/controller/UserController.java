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

private final UserService userService;
private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    @RequiresAuthentication
    public ResponseEntity<GetUserResponse> getUserProfile() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(GetUserResponse.fromUser(currentUser));
    }


    @RequestMapping("/user")
    public User getUserDetailsAfterLogin(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> optionalUser = userRepository.findByAuthenticationInfoUsername_Username(username);
        return optionalUser.orElse(null);
    }
//    @PostMapping("v1/admin/user/")
//    public ResponseEntity<UserRegistrationRequest> registerUser(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest, @PathVariable String password) throws ProductAlreadyExistsException {
//
//        User registeredUser = userService.registerUser(userRegistrationRequest);
//        return new ResponseEntity<>(userResponse, null, HttpStatus.CREATED);
//
//    }

}

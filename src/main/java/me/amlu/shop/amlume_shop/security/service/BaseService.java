/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import lombok.NoArgsConstructor;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@NoArgsConstructor
@Component
public abstract class BaseService {

    protected UserService userService;

    protected BaseService(UserService userService) {
        this.userService = userService;
    }
    
    protected Optional<User> getCurrentUser() throws UnauthorizedException {
        return Optional.ofNullable(userService.getCurrentUser());
    }

    @Cacheable("currentUserId")
    protected Long getCurrentUserId() throws UnauthorizedException {
        return userService.getCurrentUserId();
    }
}

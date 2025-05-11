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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class RoutingConfig {

    @Bean
    RouterFunction<ServerResponse> httpEndpoints() {
        return route()
                .GET("/hello", new HandlerFunction<ServerResponse>() {
                    @Override
                    @NonNull
                    public ServerResponse handle(@NonNull ServerRequest request) throws Exception {
                        var user = SecurityContextHolder
                                .getContextHolderStrategy()
                                .getContext()
                                .getAuthentication()
                                .getName();
//                                .getPrincipal();

                        return ServerResponse.ok().body(Map.of("message", "Hello, " + user + "!"));
                    }
                })
                .build();

    }
}

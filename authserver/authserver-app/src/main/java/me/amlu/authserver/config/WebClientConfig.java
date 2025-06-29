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
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Configures a WebClient bean for the amlume-shop service.
     *
     * @param builder The WebClient.Builder instance
     * @return The configured WebClient instance
     */
    @Bean
    public WebClient amlumeShopWebClient(WebClient.Builder builder) {
        return builder.baseUrl("http://amlume-shop:8080") // Internal service name and port
                // Add a filter for client credentials grant if amlume-shop requires it
                // .filter(new ClientCredentialsClientRequestInterceptor(...))
                .build();
    }
}

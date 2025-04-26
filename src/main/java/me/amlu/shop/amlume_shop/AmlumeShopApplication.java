/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop;

import io.github.cdimascio.dotenv.Dotenv;
import me.amlu.shop.amlume_shop.config.properties.AsnProperties;
import me.amlu.shop.amlume_shop.config.properties.TokenCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching // Enable Spring's caching annotations like @Cacheable
@ComponentScan(basePackages = {"me.amlu.shop.amlume_shop"})
@EnableScheduling
@EnableTransactionManagement
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableConfigurationProperties({TokenCacheProperties.class, AsnProperties.class})
public class AmlumeShopApplication {

    public static void main(String[] args) {

        // --- Load .env file VERY EARLY ---
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Optional: won't fail if .env is not there
                .load();
        // ---------------------------------
        SpringApplication.run(AmlumeShopApplication.class, args);

        // Log the available beans
//	ConfigurableApplicationContext context = SpringApplication.run(AmlumeShopApplication.class, args);
//        System.out.println("Available beans:");
//        for (String beanName : context.getBeanDefinitionNames()) {
//		System.out.println(beanName);
//	}


    }
}
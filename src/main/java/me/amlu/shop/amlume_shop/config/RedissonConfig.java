/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import me.amlu.shop.amlume_shop.config.properties.RedisConfigProperties;
import me.amlu.shop.amlume_shop.config.properties.RedisConfigPropertiesInterface;
import me.amlu.shop.amlume_shop.config.properties.ValkeyConfigProperties;
import me.amlu.shop.amlume_shop.resilience.service.ResilienceServiceImpl;
import me.amlu.shop.amlume_shop.security.service.TokenJtiServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableConfigurationProperties({ValkeyConfigProperties.class, RedisConfigProperties.class}) // Enable
@Configuration
public class RedissonConfig {

    private final RedisConfigPropertiesInterface redisConfig;

    public RedissonConfig(RedisConfigPropertiesInterface redisConfig) {
        this.redisConfig = redisConfig;
    }

    @Bean(destroyMethod = "shutdown")
    @Primary // Valkey is primary (or Redis if switched)
    public RedissonClient primaryRedissonClient() {
        Config config = new Config();
        String nodes = redisConfig.getNodes(); // Use abstract method

        if (nodes.contains(",")) {
            String[] nodeList = nodes.split(",");
            for (int i = 0; i < nodeList.length; i++) {
                nodeList[i] = "redis://" + nodeList[i];
            }
            config.useClusterServers().addNodeAddress(nodeList)
                    .setRetryAttempts(3)
                    .setScanInterval(2000);
        } else {
            String address = "redis://" + nodes;
            config.useSingleServer().setAddress(address);
            String password = redisConfig.getPassword();
            if (password != null && !password.isEmpty()) {
                config.useSingleServer().setPassword(password);
            }
            config.useSingleServer()
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(2)
                    .setRetryAttempts(3);
        }
        return Redisson.create(config);
    }

    @Bean
    public RBloomFilter<String> jtiBloomFilter(RedissonClient primaryRedissonClient) { // Inject the Valkey client
        return primaryRedissonClient.getBloomFilter("jtiBloomFilter"); // Name of your Bloom filter
    }

    @Bean
    public TokenJtiServiceImpl tokenJtiService(RedissonClient primaryRedissonClient) { // Inject primary client
        RBloomFilter<String> bloomFilter = primaryRedissonClient.getBloomFilter("jtiBloomFilter");
        // Get the required components from the RedissonClient
        return new TokenJtiServiceImpl(bloomFilter, primaryRedissonClient.getScoredSortedSet("jtiExpirations"), primaryRedissonClient.getExecutorService("virtualThreadExecutor"));
    }

    @Bean
    public ResilienceServiceImpl rateLimiterService(RedissonClient primaryRedissonClient) {
        return new ResilienceServiceImpl(primaryRedissonClient);
    }

}

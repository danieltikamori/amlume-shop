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

import me.amlu.shop.amlume_shop.security.service.TokenJtiService;
import me.amlu.shop.amlume_shop.security.service.TokenJtiServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    // Redis (General Use - e.g., Sessions)
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword; // Add password if needed

    // Valkey (JTI Bloom Filter)
    @Value("${valkey.nodes}")
    private String valkeyNodes;
    @Value("${valkey.password}") // Separate password for Valkey (optional)
    private String valkeyPassword;



    @Bean
    public RedissonClient redisRedissonClient() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%d", redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setAddress(redisUrl).setPassword(redisPassword);
        } else {
            config.useSingleServer().setAddress(redisUrl);
        }
        return Redisson.create(config);
    }

//    @Bean
//    public RedissonClient redisRedissonClient() {
//        Config config = new Config();
//        config.useSingleServer()
//            .setAddress("redis://" + redisHost + ":" + redisPort);
//        return Redisson.create(config);
//    }

    @Bean(destroyMethod = "shutdown") // Important for proper shutdown
    public RedissonClient valkeyRedissonClient() {
        Config config = new Config();

        // Single Node or Cluster configuration based on your setup
        if (valkeyNodes.contains(",")) { // Check if it looks like a cluster address
            String[] nodes = valkeyNodes.split(",");
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = "redis://" + nodes[i];
            }
            config.useClusterServers().addNodeAddress(nodes).setRetryAttempts(3).setScanInterval(2000);
        } else {
            config.useSingleServer().setAddress("redis://" + valkeyNodes).setConnectionMinimumIdleSize(1).setConnectionPoolSize(2).setRetryAttempts(3);
        }

        return Redisson.create(config);
    }

    @Bean
    public RBloomFilter<String> jtiBloomFilter(RedissonClient valkeyRedissonClient) { // Inject the Valkey client
        return valkeyRedissonClient.getBloomFilter("jtiBloomFilter"); // Name of your Bloom filter
    }

    @Bean
    public TokenJtiServiceImpl tokenJtiService(RedissonClient valkeyRedissonClient) {
        TokenJtiServiceImpl tokenJtiService = new TokenJtiServiceImpl(valkeyRedissonClient.getBloomFilter("jtiBloomFilter"), valkeyRedissonClient.getScoredSortedSet("jtiExpirations"), valkeyRedissonClient.getExecutorService("virtualThreadExecutor"));
        tokenJtiService.initialize();
        return tokenJtiService;
    }

//    @Bean
//    public RedissonClient valkeyRedissonClient() {
//        Config config = new Config();
//
//        // For single node
//        config.useSingleServer()
//                .setAddress("redis://" + valkeyNodes)
//                .setConnectionMinimumIdleSize(1)
//                .setConnectionPoolSize(2)
//                .setRetryAttempts(3);
//
//        // For cluster setup
//        /*
//        config.useClusterServers()
//            .addNodeAddress("redis://" + valkeyNodes)
//            .setRetryAttempts(3)
//            .setScanInterval(2000);
//        */
//
//        return Redisson.create(config);
//    }

}

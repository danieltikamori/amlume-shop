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

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration class for setting up caching with Valkey (Redis alternative).
 * This class is currently commented out, but can be uncommented and used if needed(future use).
 * There are usage examples below. Also see Kubernetes.md for more information.
 * <p>
 * Note: This configuration is not currently in use. It is a placeholder for future caching implementation.
 * <p>
 * Created by Daniel Itiro Tikamori on 2025-05-19.
 */

@Configuration
@EnableCaching
public class ValkeyCacheConfig {
//
//    @Bean
//    public ValkeyClient valkeyClient() {
//        return ValkeyClient.create(ValkeyClientConfig.builder()
//                .addEndpoint("localhost", 6379)
//                .build());
//    }
//
//    @Bean
//    public CacheManager cacheManager(ValkeyClient valkeyClient) {
//        ValkeyConnectionFactory connectionFactory = new ValkeyConnectionFactory(valkeyClient);
//
//        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(defaultCacheConfig())
//                .withInitialCacheConfigurations(customCacheConfigs())
//                .build();
//
//        return cacheManager;
//    }
//
//    private RedisCacheConfiguration defaultCacheConfig() {
//        return RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(10))
//                .serializeKeysWith(RedisSerializationContext.SerializationPair
//                        .fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair
//                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
//    }
//
//    private Map<String, RedisCacheConfiguration> customCacheConfigs() {
//        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
//
//        configs.put("roles", RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(5)));
//
//        configs.put("rateLimit", RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(1)));
//
//        return configs;
//    }


    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(System.getenv().getOrDefault("REDIS_HOST", "localhost"));
        config.setPort(Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379")));

        // Uncomment and configure if you need authentication
        // String password = System.getenv("REDIS_PASSWORD");
        // if (password != null && !password.isEmpty()) {
        //     config.setPassword(password);
        // }

        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisStandaloneConfiguration standaloneConfig) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig);
        // Enable TCP keepalive
        factory.setUseSsl(false); // Enable SSL for production if needed
        factory.setShareNativeConnection(true);
        return factory;
    }

    @Bean
    public LettuceClientConfiguration.LettuceSslClientConfigurationBuilder lettuceClientConfiguration() {
        return LettuceClientConfiguration.builder()
//                .useSsl() // Enable SSL for production if needed
                .clientOptions(LettuceClientConfiguration.builder().build().getClientOptions());
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable transaction support if needed
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues(); // Don't cache null values

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("userCache", // Custom configuration for specific cache
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))) // Different TTL for user cache
                .withCacheConfiguration("productCache",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))) // Different TTL for product cache
                .build();
    }

    /**
     * @Service
     * public class CacheableService {
     *
     *     @Autowired
     *     private RedisTemplate<String, Object> redisTemplate;
     *
     *     public Object getDataWithCaching(String key) {
     *         // Try to get from cache first
     *         Object cachedValue = redisTemplate.opsForValue().get(key);
     *         if (cachedValue != null) {
     *             return cachedValue;
     *         }
     *
     *         // If not in cache, get from database
     *         Object value = // your database query here
     *
     *         // Store in cache for future requests
     *         redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
     *
     *         return value;
     *     }
     * }
     */

    /**
     * @Service
     * public class ProductService {
     *
     *     @Cacheable(value = "productCache", key = "#id")
     *     public Product getProduct(Long id) {
     *         // Method implementation
     *     }
     *
     *     @CachePut(value = "productCache", key = "#product.id")
     *     public Product updateProduct(Product product) {
     *         // Method implementation
     *     }
     *
     *     @CacheEvict(value = "productCache", key = "#id")
     *     public void deleteProduct(Long id) {
     *         // Method implementation
     *     }
     * }
     */

    /**
     * @Service
     * public class AsnReputationServiceImpl implements AsnReputationService {
     *
     *     @Autowired
     *     private RedisTemplate<String, Object> redisTemplate;
     *
     *     private static final String ASN_REPUTATION_KEY = "asn:reputation:";
     *
     *     @Override
     *     public void recordActivity(String asn, boolean isSuspicious) {
     *         String key = ASN_REPUTATION_KEY + asn;
     *         Double currentScore = (Double) redisTemplate.opsForValue().get(key);
     *         double newScore;
     *
     *         if (currentScore == null) {
     *             newScore = isSuspicious ? 0.0 : 1.0;
     *         } else {
     *             // Adjust score based on activity
     *             newScore = isSuspicious ?
     *                 Math.max(0.0, currentScore - 0.1) :
     *                 Math.min(1.0, currentScore + 0.05);
     *         }
     *
     *         redisTemplate.opsForValue().set(key, newScore, Duration.ofDays(30));
     *     }
     *
     *     @Override
     *     public double getReputationScore(String asn) {
     *         String key = ASN_REPUTATION_KEY + asn;
     *         Double score = (Double) redisTemplate.opsForValue().get(key);
     *         return score != null ? score : 0.5; // Default neutral score
     *     }
     * }
     */
}

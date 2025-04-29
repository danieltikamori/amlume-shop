/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 * ... (rest of copyright notice) ...
 */

package me.amlu.shop.amlume_shop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo; // For default typing
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For Java 8+ time types
import me.amlu.shop.amlume_shop.commons.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer; // Use customizer
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.cache.RedisCacheManager; // BuilderCustomizer is preferred
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
// Removed HashMap and Map imports as BuilderCustomizer is used

// Import static constants for TTLs
import static me.amlu.shop.amlume_shop.commons.Constants.*;

/**
 * Configuration class for setting up caching and Redis/Valkey connection
 * using Spring Data Redis with Lettuce connector.
 * Configures RedisTemplate, StringRedisTemplate, and the primary CacheManager beans
 * using RedisCacheManagerBuilderCustomizer for better Spring Boot integration.
 */
@Configuration
@EnableCaching // Enables Spring's caching annotations like @Cacheable
public class ValkeyCacheConfig {

    // --- Cache Names for IP Security & Geolocation ---
    // Defined here for clarity, could also be in Constants.java
    public static final String IP_BLOCK_CACHE = "ipBlockCache";
    public static final String IP_METADATA_CACHE = "ipMetadataCache";
    public static final String GEO_LOCATION_CACHE = "geoLocationCache";
    public static final String GEO_HISTORY_CACHE = "geoHistoryCache";

    // Inject properties using @Value
    @Value("${valkey.host:localhost}")
    private String redisHost;

    @Value("${valkey.port:6379}")
    private int redisPort;

    @Value("${valkey.password:#{null}}")
    private String redisPassword;

    @Value("${security.geo.time-window-hours:24}")
    private int geoHistoryTtlHours;

    // Inject the application's pre-configured ObjectMapper
    private final ObjectMapper objectMapper;

    public ValkeyCacheConfig(ObjectMapper objectMapper) {
        // Configure the ObjectMapper specifically for cache serialization needs
        this.objectMapper = objectMapper.copy(); // Work on a copy to avoid side effects
        this.objectMapper.registerModule(new JavaTimeModule()); // Ensure Java 8+ time types are handled
        // Enable default typing for polymorphic types if needed, adjust security as necessary
        // Use NON_FINAL for flexibility, consider specific type registration for stricter security

        // Create a PolymorphicTypeValidator instance.
        // BasicPolymorphicTypeValidator is a standard implementation.
        // allowIfSubType(Object.class) is a permissive setting suitable for
        // internal caching where you trust the types being cached.
        // For stricter security, you might use allowIfBaseType or allowIfSubTypeIsArray etc.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class) // Allows any subtype of Object
                .build();

        // Pass the validator instance to activateDefaultTyping
        this.objectMapper.activateDefaultTyping(
                ptv, // Use the created validator
                ObjectMapper.DefaultTyping.NON_FINAL, // Or OBJECT_AND_NON_CONCRETE
                JsonTypeInfo.As.PROPERTY); // Store type info as a property (e.g., "@class")
    }

    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }
        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisStandaloneConfiguration standaloneConfig) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig);
        factory.setShareNativeConnection(true);
        factory.setValidateConnection(true);
        return factory;
    }

    // Keep RedisTemplate beans if they are used directly elsewhere in the application
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
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // Use the ObjectMapper configured in the constructor for consistency
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        // Define the default cache configuration (serializers, default TTL, null values)
        // Use the ObjectMapper configured in the constructor
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL if not overridden
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer) // Use configured ObjectMapper
                )
                .disableCachingNullValues(); // Prevent caching null values
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultCacheConfig) {
        // Use RedisCacheManagerBuilderCustomizer for better integration and flexibility
        return (builder) -> builder
                // Apply default config first
                .cacheDefaults(defaultCacheConfig)
                // --- Configure Specific Caches ---

                // IP Security Caches (using 24h TTL)
                .withCacheConfiguration(IP_BLOCK_CACHE,
                        defaultCacheConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration(IP_METADATA_CACHE,
                        defaultCacheConfig.entryTtl(Duration.ofHours(24)))

                // Geolocation Cache (using 24h TTL)
                .withCacheConfiguration(GEO_LOCATION_CACHE,
                        defaultCacheConfig.entryTtl(Duration.ofHours(24)))

                // GeoHistory cache
                .withCacheConfiguration(GEO_HISTORY_CACHE,
                        defaultCacheConfig.entryTtl(Duration.ofHours(this.geoHistoryTtlHours)))

                // --- Include Existing Cache Configurations from Constants ---
                .withCacheConfiguration(Constants.PRODUCTS_CACHE, defaultCacheConfig.entryTtl(PRODUCTS_CACHE_TTL))
                .withCacheConfiguration(Constants.CATEGORIES_CACHE, defaultCacheConfig.entryTtl(CATEGORIES_CACHE_TTL))
                .withCacheConfiguration(Constants.USERS_CACHE, defaultCacheConfig.entryTtl(USERS_CACHE_TTL))
                .withCacheConfiguration(Constants.ROLES_CACHE, defaultCacheConfig.entryTtl(ROLES_CACHE_TTL))
                .withCacheConfiguration(Constants.ASN_CACHE, defaultCacheConfig.entryTtl(ASN_CACHE_TTL))
                .withCacheConfiguration(Constants.TOKENS_CACHE, defaultCacheConfig.entryTtl(TOKENS_CACHE_TTL))
                .withCacheConfiguration(Constants.TEMPORARY_CACHE, defaultCacheConfig.entryTtl(TEMPORARY_CACHE_TTL))
                .withCacheConfiguration(Constants.HCP_SECRETS_CACHE, defaultCacheConfig.entryTtl(HCP_SECRETS_CACHE_TTL))
                // Add any other caches defined in Constants that need specific TTLs
                // e.g., .withCacheConfiguration(Constants.AUTH_CACHE, defaultCacheConfig.entryTtl(AUTH_CACHE_TTL))

                // --- Optional Features ---
                .enableStatistics() // Enable cache statistics for monitoring (requires management.metrics.cache.instrument=true)
                .transactionAware(); // Enable if cache operations should participate in Spring transactions
    }

    // --- Optional: Explicit CacheManager Bean (if needed elsewhere, otherwise customizer is enough) ---
    // @Bean
    // public CacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration defaultCacheConfig, RedisCacheManagerBuilderCustomizer customizer) {
    //     RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
    //             .cacheDefaults(defaultCacheConfig);
    //     customizer.customize(builder); // Apply customizations
    //     return builder.build();
    // }

    // Keep Lua script bean if used for rate limiting or other custom Redis operations
    @Bean
    public RedisScript<Long> slidingWindowRateLimiterScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // Make sure the path is correct based on your project structure
        redisScript.setLocation(new ClassPathResource("scripts/sliding_window_rate_limit_check_first.lua"));
        redisScript.setResultType(Long.class); // Lua script returns 1 or 0 (number)
        return redisScript;
    }
}
/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 * ... (rest of copyright notice) ...
 */

package me.amlu.shop.amlume_shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import me.amlu.shop.amlume_shop.commons.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
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
    @Value("${valkey.host:localhost}") // Default to localhost for local development
    private String redisHost;

    @Value("${valkey.port:6379}") // Now TLS port
    private int redisPort;

    @Value("${valkey.password:#{null}}") // Ensure this is set in application.yml or environment
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

        // --- IMPLEMENTING ENCRYPTION - COMMENTED OUT FOR THE IMPLEMENTATION WORK ---
//        // Create a PolymorphicTypeValidator instance.
//        // BasicPolymorphicTypeValidator is a standard implementation.
//        // allowIfSubType(Object.class) is a permissive setting suitable for
//        // internal caching where you trust the types being cached.
//        // For stricter security, you might use allowIfBaseType or allowIfSubTypeIsArray etc.
//        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
//                .allowIfSubType(Object.class) // Allows any subtype of Object
//                .build();
//
//        // Pass the validator instance to activateDefaultTyping
//        this.objectMapper.activateDefaultTyping(
//                ptv, // Use the created validator
//                ObjectMapper.DefaultTyping.NON_FINAL, // Or OBJECT_AND_NON_CONCRETE
//                JsonTypeInfo.As.PROPERTY); // Store type info as a property (e.g., "@class")
        // --- END ENCRYPTION IMPLEMENTATION RELATED COMMENT ---
    }

    /**
     * Must use Valkey password
     *
     * @return configuration
     */
    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        // Connect to the HOSTNAME defined in docker-compose (valkey-cache)
        // OR use localhost if running app outside docker but connecting to exposed port
        // Using service name is generally better within docker network
        config.setHostName("valkey-cache"); // <-- Use Docker service name
        config.setPort(redisPort); // Port 6379 (which is now the TLS port)

        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword); // Set password
        } else {
            // Consider throwing an error if password is required but missing
            throw new IllegalStateException("Valkey password is required but not configured!");
        }
        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisStandaloneConfiguration standaloneConfig) {
        // --- Configure Lettuce Client for TLS ---
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder();

        // 1. Enable SSL/TLS
        clientConfigBuilder.useSsl();

        // 2. Configure Trust Strategy (Choose ONE)

        // --- Option A: Disable Peer Verification (INSECURE - Development/Testing ONLY) ---
        // This trusts *any* certificate presented by the server. Convenient for self-signed certs locally.
        // DO NOT USE IN PRODUCTION.
        // This uses the hook provided by Lettuce to configure the underlying Netty SslContextBuilder
        // to trust all certificates using Netty's InsecureTrustManagerFactory.
        clientConfigBuilder.clientOptions(ClientOptions.builder()
                .sslOptions(SslOptions.builder()
                        .sslContext(sslCustomizer -> { // Use the Consumer<SslContextBuilder>
                            try {
                                // Configure the Netty SslContextBuilder to use the insecure trust manager
                                sslCustomizer.trustManager(InsecureTrustManagerFactory.INSTANCE);
                            } catch (Exception e) {
                                // Handle potential exceptions during customization, though unlikely here
                                throw new RuntimeException("Failed to configure Lettuce SSL context with insecure trust manager", e);
                            }
                        })
                        .build()) // Build SslOptions
                .build()); // Build ClientOptions
        // --- End Option A ---


        /*
        // --- Option B: Use a Truststore (SECURE - Recommended for Production) ---
        // Requires creating a JKS truststore containing the server's CA or self-signed cert.
        // Example using keytool:
        // keytool -importcert -file ./certificates/valkey.crt -alias valkey-dev -keystore truststore.jks -storepass your_truststore_password -noprompt

    try {
            // Adjust path and password as needed
            File truststoreFile = new File("./path/to/your/truststore.jks"); // Or use SslOptions.Resource
            String truststorePassword = "your_truststore_password";

            SslOptions sslOptionsB = SslOptions.builder()
                    .truststore(truststoreFile, truststorePassword) // Use the correct method
                    .build();

            ClientOptions clientOptionsB = ClientOptions.builder()
                    .sslOptions(sslOptionsB)
                    .build();

            clientConfigBuilder.clientOptions(clientOptionsB);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure Lettuce SSL options with truststore", e);
        }
        // --- End Option B ---

        */

        // Build the final client configuration
        LettuceClientConfiguration clientConfiguration = clientConfigBuilder.build();

        // Create the factory using standalone config and client config
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
        factory.setShareNativeConnection(true); // Usually recommended
        factory.setValidateConnection(true); // Validate connections on checkout
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
        // IMPORTANT NOTE for ENCRYPTION:
        // ENSURE  the serializer uses the ObjectMapper *without* default typing

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
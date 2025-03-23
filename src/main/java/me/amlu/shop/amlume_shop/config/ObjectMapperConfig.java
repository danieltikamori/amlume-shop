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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.beans.PropertyEditor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Configuration
@EnableConfigurationProperties
public class ObjectMapperConfig {

    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register modules
        mapper.registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
//                .registerModule(new JavaTimeModule());

        // Configure serialization features
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Configure deserialization features
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                // Adjust instant handling
                .enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

        // Configure timezone to UTC
        mapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

        // Configure visibility
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);


        return mapper;
    }

    /**
     * Configure Jackson's ObjectMapper for RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.setMessageConverters(Collections.singletonList(converter));
        return restTemplate;
    }

    /**
     * Configure Jackson's ObjectMapper for WebMvc
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer(ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();
                converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
            }
        };
    }

    /**
     * Configure Jackson's ObjectMapper for Redis
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Set up Jackson2JsonRedisSerializer with our ObjectMapper
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        serializer.setObjectMapper(objectMapper);

        // Configure serializers
        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

//    /**
//     * Configure custom property editor for dates
//     */
//    @Bean
//    public CustomEditorConfigurer customEditorConfigurer() {
//        CustomEditorConfigurer configurer = new CustomEditorConfigurer();
//        Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<>();
//        editors.put(LocalDateTime.class, CustomLocalDateTimeEditor.class);
//        configurer.setCustomEditors(editors);
//        return configurer;
//    }

    /**
     * Configure custom property editor for Instant
     */
    @Bean
    public CustomEditorConfigurer customEditorConfigurer() {
        CustomEditorConfigurer configurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<>();
        editors.put(Instant.class, CustomInstantEditor.class);
        configurer.setCustomEditors(editors);
        return configurer;
    }

    /**
     * Configure ObjectMapper for Spring Security
     */
    @Bean
    public SecurityJackson2Modules securityJackson2Modules() {
        return new SecurityJackson2Modules();
    }
//    @Bean
//    public SecurityJackson2Modules securityJackson2Modules(ObjectMapper objectMapper) {
//        SecurityJackson2Modules.register(objectMapper);
//        return null; // or remove the return statement altogether
//    }
}

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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;


@Configuration
@EnableConfigurationProperties
public class ObjectMapperConfig {

    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                // Register modules
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                // Configure serialization features
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Correct with JavaTimeModule
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Configure deserialization features
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Consider if this is truly needed application-wide:
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                // REMOVED: Likely unnecessary/confusing with JavaTimeModule. Rely on the module.
                // .enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                // Configure timezone to UTC
                .defaultTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
                .build();

        // Configure visibility
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);

        // Register security modules
        ClassLoader loader = getClass().getClassLoader();
        List<com.fasterxml.jackson.databind.Module> securityModules =
                SecurityJackson2Modules.getModules(loader);
        mapper.registerModules(securityModules);

        return mapper;
    }

    /**
     * Configure Jackson's ObjectMapper for RestTemplate
     */
    @Bean
    public RestTemplate objectMapperRestTemplate(ObjectMapper objectMapper) {
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
                // Clear defaults and add only our configured converter
                converters.clear();
                converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
            }
        };
    }

    // --- CustomEditorConfigurer Bean ---
    // MOVED: This bean configures Spring data binding (e.g., for request params),
    // not Jackson. It should be moved to a WebConfig or DataBindingConfig class.
    /*
    @Bean
    public CustomEditorConfigurer customEditorConfigurer() {
        CustomEditorConfigurer configurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<>();
        editors.put(Instant.class, CustomInstantEditor.class);
        // If you need LocalDateTime editor too, add it here:
        // editors.put(LocalDateTime.class, CustomLocalDateTimeEditor.class);
        configurer.setCustomEditors(editors);
        return configurer;
    }
    */

}

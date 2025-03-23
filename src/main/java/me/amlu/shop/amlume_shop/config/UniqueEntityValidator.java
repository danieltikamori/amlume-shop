/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.EntityUniquenessException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Tikamori
 *  This validator is used to check if an entity already exists in the database, based on the fields passed in the annotation.
 *  Use the annotation in your DTOs/Requests:
 *  public class IngredientCategoryRequest {
 *     @UniqueEntity(fields = {"categoryName", "restaurantId"})
 *     private String categoryName;
 *      private Long restaurantId;
 *      // ...
 *  }
 */

@Slf4j
@Component
public class UniqueEntityValidator implements ConstraintValidator<UniqueEntity, Object> {

    private final ApplicationContext applicationContext;
    private UniqueEntity annotation;

    public UniqueEntityValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void initialize(UniqueEntity annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            // Get repository based on entity type
            Class<?> entityClass = value.getClass();
            JpaRepository<?, ?> repository = getRepository(entityClass);

            // Build the query criteria
//            Example<?> example = buildExample(value, entityClass);
            Example<S> example = (Example<S>) buildExample(value, entityClass);

            // Check for existing entity
            boolean exists = repository.exists(example);

            if (exists) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(annotation.message())
                        .addPropertyNode(String.join(",", annotation.fields()))
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating uniqueness for entity: {}", value.getClass(), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> JpaRepository<T, ?> getRepository(Class<T> entityClass) {
        String repositoryBeanName = entityClass.getSimpleName() + "Repository";
        repositoryBeanName = repositoryBeanName.substring(0, 1).toLowerCase() +
                repositoryBeanName.substring(1);

        return (JpaRepository<T, ?>) applicationContext.getBean(repositoryBeanName);
    }

    @SuppressWarnings("unchecked")
    private <T> Example<T> buildExample(Object entity, Class<T> entityClass) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnorePaths("id")  // Ignore ID field
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT)
                .withIgnoreNullValues();

        for (String field : annotation.fields()) {
            matcher = matcher.withMatcher(field,
                    ExampleMatcher.GenericPropertyMatchers.exact());
        }

        return Example.of((T) entity, matcher);
    }
}

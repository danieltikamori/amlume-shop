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

    public UniqueEntityValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private String[] fields;

    // Store repository instances for faster lookup (optional, but recommended)
    private final Map<Class<?>, JpaRepository<?, ?>> repositoryCache = new HashMap<>();

    @Override
    public void initialize(UniqueEntity constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object entity, ConstraintValidatorContext context) {
        Object repository = getRepositoryForEntity(entity.getClass());
        boolean exists = checkIfEntityExists(repository, entity);

        if (exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(fields[0])
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private Object getRepositoryForEntity(Class<?> entityClass) {
        return repositoryCache.computeIfAbsent(entityClass, clazz -> {
            // Assuming your repositories follow a naming convention (e.g., EntityNameRepository)
            String repositoryName = clazz.getSimpleName() + "Repository";
            return (JpaRepository<?, ?>) applicationContext.getBean(repositoryName);
        });
    }

    private boolean checkIfEntityExists(Object repository, Object entity) {
        try {
            for (String fieldName : fields) {
                Field field = entity.getClass().getDeclaredField(fieldName);
                field.trySetAccessible();
                Object fieldValue = field.get(entity);

                // 1. Get Field Types Dynamically:
                Class<?>[] fieldTypes = new Class<?>[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    fieldTypes[i] = entity.getClass().getDeclaredField(fields[i]).getType();
                }

                // 2. Construct Method Name:
                String methodName = "findBy" + String.join("And", fields);

                // 3. Invoke Method with Correct Types:
                Object existingEntity = repository.getClass().getMethod(methodName, fieldTypes)
                        .invoke(repository, fieldValue); // Pass only fieldValue once

                if (existingEntity != null) {
                    return true; // Duplicate found
                }
            }
        } catch (Exception e) {
            log.error("Error checking for entity uniqueness", e);
            throw new EntityUniquenessException("Error checking for entity uniqueness", e);
        }
        return false; // No duplicates found
    }
}
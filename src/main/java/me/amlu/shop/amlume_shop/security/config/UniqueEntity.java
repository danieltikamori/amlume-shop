/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/*
Usage example:

@Entity
@UniqueEntity(fields = {"name", "userEmail"}, message = "User with this name and userEmail already exists")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String userEmail;
    // other fields...
}

Alternative Approaches :

// Using @Column unique constraint
@Column(unique = true)
private String userEmail;

// Or using @Table unique constraint
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "userEmail"})
})

Database-Level Constraints (SQL) :

ALTER TABLE users
ADD CONSTRAINT uk_name_email UNIQUE (name, userEmail);

Spring Data JPA Query Methods(Java) :

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNameAndEmail(String name, String userEmail);
}

Recommendations:

For simple unique constraints, use @Column(unique = true)
For composite unique constraints, use @Table(uniqueConstraints = ...)

Use this custom validator when you need:

Complex validation logic
Custom error messages
Runtime validation without database constraints
Validation during DTO processing

Example with all approaches combined:

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "userEmail"})
})
@UniqueEntity(fields = {"name", "userEmail"},
    message = "User with this name and userEmail already exists")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String userEmail;

    // getters and setters
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String userEmail);
    boolean existsByNameAndEmail(String name, String userEmail);
}

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEntityException("Email already exists");
        }
        return userRepository.save(user);
    }
}


This provides multiple layers of validation:

Application-level validation (custom validator)
Database-level constraints
Programmatic checks in service layer
Spring Data JPA query methods
 */

@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
//@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEntityValidator.class)
public @interface UniqueEntity {
    String message() default "Entity with these attributes already exists";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // Attributes to check for uniqueness (e.g., "name", "restaurantId")
    String[] fields();
}


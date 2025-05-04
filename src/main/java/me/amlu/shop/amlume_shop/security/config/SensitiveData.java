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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SensitiveDataValidator.class)
public @interface SensitiveData {
    String[] rolesAllowed() default {}; // Roles allowed to see this data

    String message() default "Unauthorized access to sensitive data.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

/** Example usage with @SensitiveData annotation
 * @SensitiveData(rolesAllowed = {"ADMIN"}) // Static roles
 * public class DocumentService {
 *
 *     @SensitiveData(rolesAllowed = {}) // Only dynamic roles
 *     public Document getDocument(String documentId) {
 *         // Method implementation
 *     }
 *
 *     @SensitiveData(rolesAllowed = {"MANAGER"}) // Both static and dynamic roles
 *     public void updateDocument(Document document) {
 *         // Method implementation
 *     }
 * }
 */

/** Example usage with @SensitiveData annotation
 * @SensitiveData(rolesAllowed = {"ADMIN"}) // Static roles
 * public class DocumentService {
 *
 *     @SensitiveData(rolesAllowed = {}) // Only dynamic roles
 *     public Document getDocument(String documentId) {
 *         // Method implementation
 *     }
 *
 *     @SensitiveData(rolesAllowed = {"MANAGER"}) // Both static and dynamic roles
 *     public void updateDocument(Document document) {
 *         // Method implementation
 *     }
 * }
 */
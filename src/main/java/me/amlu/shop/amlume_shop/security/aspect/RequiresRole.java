/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.aspect;

import me.amlu.shop.amlume_shop.user_management.AppRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;

/**
 * Annotation to enforce role-based access control on methods.
 *
 * <p>This annotation requires the user to have a specific role to execute the annotated method.
 * Used in conjunction with {@link AuthenticationAspect} for role verification.</p>
 *
 * <p>If the user lacks the required role, a security exception will be thrown.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @RequiresAuthentication
 * @RequiresRole({AppRole.ROLE_ADMIN, AppRole.ROLE_MANAGER}) // Use enum array
 * public void adminOrManagerMethod() {
 * // Method implementation
 * }
 * }
 * </pre>
 */

@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    /**
     * The roles required to execute the annotated method.
     */
    AppRole[] value();

    /**
     * If true, the user must possess *all* roles specified in value().
     * If false (default), the user must possess *at least one* of the roles.
     */
    boolean requireAll() default false;
}


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
 * @RequiresRole(value = "ROLE_ADMIN", allowMultiple = true)
 * public void adminMethod() {
 * // Method implementation
 * }
 * }
 * </pre>
 *
 * @see me.amlu.shop.amlume_shop.security.aspect.AuthenticationAspect
 * @see me.amlu.shop.amlume_shop.security.aspect.RequiresAuthentication
 */
@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
//    /**
//     * @return the required role name for method access
//     */
//    String value();

    /**
     * @return whether to allow multiple roles (default: false)
     */
    boolean allowMultiple() default false;

    /**
     * The roles required to execute the annotated method.
     * If multiple roles are specified, the user must have at least one of them.
     */
    AppRole[] value(); // Takes an array of AppRole enums
}

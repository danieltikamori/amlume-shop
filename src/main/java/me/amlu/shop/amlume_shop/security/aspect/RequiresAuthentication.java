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

import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;

/**
 * Indicates that a method requires user authentication before execution.
 *
 * <p>This security annotation is processed by {@link AuthenticationAspect} to enforce
 * authentication requirements. Methods annotated with {@code @RequiresAuthentication}
 * will throw an {@link UnauthorizedException} if accessed by an unauthenticated user.</p>
 *
 * <p>Common usage patterns:</p>
 * <ul>
 *     <li>Use alone to require basic authentication</li>
 *     <li>Combine with {@link RequiresRole} for role-based access control</li>
 *     <li>Apply to sensitive operations requiring user identity verification</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RequiresAuthentication
 * public void securedMethod() {
 *     // Only authenticated users can access this method
 * }
 *
 * @RequiresAuthentication
 * @RequiresRole("ADMIN")
 * public void adminMethod() {
 *     // Only authenticated users with ADMIN role can access this method
 * }
 * }</pre>
 *
 * @see AuthenticationAspect
 * @see RequiresRole
 * @see UnauthorizedException
 * @since 1.0
 */
@Documented
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuthentication {
    /**
     * Determines whether to throw an exception or return gracefully on authentication failure.
     *
     * @return true to throw exception (default), false to return gracefully
     */
    boolean strict() default true;

    /**
     * Optional message to include in the UnauthorizedException if authentication fails.
     *
     * @return custom error message for authentication failure
     */
    String message() default "Authentication required to access this resource";
}

/*
* // Basic usage
@RequiresAuthentication
public void securedMethod() {
    // Only authenticated users can access
}

// With custom error message
@RequiresAuthentication(message = "Please log in to access user profile")
public void getUserProfile() {
    // Custom error message if not authenticated
}

// Non-strict mode (won't throw exception)
@RequiresAuthentication(strict = false)
public Optional<UserData> getUserData() {
    // Returns empty Optional if not authenticated
    return Optional.empty();
}

// Class-level authentication requirement
@RequiresAuthentication
public class SecuredService {
    // All methods in this class require authentication
}
 */


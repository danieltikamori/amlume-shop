/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.events;

import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
public class AuthorizationEvents {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AuthorizationEvents.class);

    /**
     * Listens for AuthorizationDeniedEvent and logs the details.
     * Uses the generic type <Object> to avoid raw type usage and handle
     * denials from potentially different sources (web, method security).
     * <p>
     * In newer Spring Security versions, getAuthentication() returns a Supplier.
     *
     * @param deniedEvent The authorization denied event.
     */
    @EventListener
    public void onFailure(AuthorizationDeniedEvent<Object> deniedEvent) { // Specify the generic type parameter <Object>
        // Get the Supplier<Authentication>
        Supplier<Authentication> authSupplier = deniedEvent.getAuthentication();

        // Get the Authentication object from the supplier and wrap in Optional
        // Use Optional.ofNullable in case the supplier returns null
        Optional<Authentication> authOptional = Optional.ofNullable(authSupplier.get());

        // Safely get the username from the Authentication Optional
        String username = authOptional.map(Authentication::getName).orElse("unknown user"); // Use map orElse

        // Safely get the decision details
        AuthorizationResult decision = deniedEvent.getAuthorizationResult();
        String decisionDetails = (decision != null) ? decision.toString() : "unknown decision details";

        // Log the denial - Changed level to WARN as it's a security-relevant denial, not necessarily an ERROR in the app flow
        log.warn("Authorization DENIED for user: '{}'. Decision: {}", username, decisionDetails);

        // Optionally log the type of object that was secured if needed for debugging
        // Object securedObject = deniedEvent.getAuthorizationContext().getSource();
        // log.debug("Secured object type: {}", securedObject != null ? securedObject.getClass().getName() : "null");
    }

    // --- Keep other listeners if they exist in your actual class ---
    /*
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent successEvent) {
        // ... handle success ...
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failureEvent) {
        // ... handle authentication failure ...
    }
    */
}

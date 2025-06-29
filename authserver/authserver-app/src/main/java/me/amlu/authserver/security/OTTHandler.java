/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;

import java.io.IOException;

/**
 * Handles the successful generation of a One-Time-Token (OTT).
 * In a real-world application, this handler would be responsible for sending the
 * token to the user via email or SMS. For development and demonstration, this
 * implementation logs the token and provides it in the HTTP response.
 *
 * <p><b>Usage:</b> This class is typically configured within Spring Security's
 * authentication flow to handle the successful generation of a One-Time Token.
 * It should be registered as the success handler for OTT generation.
 *
 * <p><b>Security Note:</b> The current implementation returns the OTT directly
 * in the HTTP response for demonstration purposes. In a production environment,
 * this is a severe security vulnerability. The token MUST be sent via a secure,
 * out-of-band channel (e.g., email, SMS) to the user's registered contact information.
 * Never expose the OTT directly in the response.
 */
public class OTTHandler implements OneTimeTokenGenerationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OTTHandler.class); // NOSONAR: Logger is static and final, not a security risk.

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) throws IOException, ServletException {
        // In a real-world app, this would be sent to the user via email/SMS.
        // For this implementation, we will log it and return it in the response for easy testing.
        String tokenValue = oneTimeToken.getTokenValue();
        String username = oneTimeToken.getUsername();

        log.info("One-Time-Token generated for user '{}'. Token: {}", username, tokenValue);
        log.warn("OTT FEATURE: In a real application, this token would be sent via a secure channel (email/SMS), not returned in the response.");

        // For demonstration purposes, write a simple response.
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().printf("OTT Generated for %s. Login link: /login/ott?token=%s", username, tokenValue);
    }
}

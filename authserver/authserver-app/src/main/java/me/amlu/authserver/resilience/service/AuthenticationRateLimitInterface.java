/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.service;

//import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException; // Assuming this might be thrown too

import me.amlu.authserver.exceptions.RateLimitExceededException;

public interface AuthenticationRateLimitInterface {

    /**
     * Checks if a request is allowed based on IP address rate limiting.
     * Throws RateLimitExceededException if the limit is hit.
     *
     * @param ipAddress The IP address making the request.
     * @throws RateLimitExceededException if the rate limit for the IP is exceeded.
     */
    void verifyIpRateLimit(String ipAddress) throws RateLimitExceededException;

    /**
     * Checks if a request is allowed based on username rate limiting.
     * Throws RateLimitExceededException if the limit is hit.
     *
     * @param username The username making the request.
     * @throws RateLimitExceededException if the rate limit for the username is exceeded.
     */
    void verifyUsernameRateLimit(String username) throws RateLimitExceededException;

    /**
     * Gets the current request count for a given IP address within the active window.
     * Returns 0 if the key doesn't exist or an error occurs.
     *
     * @param ipAddress The IP address to check.
     * @return The current count, or 0 on error/absence.
     */
    long getRemainingIpLimit(String ipAddress);

    /**
     * Gets the current request count for a given username within the active window.
     * Returns 0 if the key doesn't exist or an error occurs.
     *
     * @param username The username to check.
     * @return The current count, or 0 on error/absence.
     */
    long getRemainingUsernameLimit(String username);

//    /**
//     * Gets the configured request limit per IP address.
//     *
//     * @return The IP request limit.
//     */
//    long getIpRequestLimit(); // New method
}

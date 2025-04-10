/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import me.amlu.shop.amlume_shop.exceptions.InvalidCaptchaException;
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.payload.user.AuthenticationRequest;
import me.amlu.shop.amlume_shop.payload.user.LoginRequest;
import me.amlu.shop.amlume_shop.payload.user.MfaVerificationRequest;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public interface EnhancedAuthenticationService {


    AuthResponse authenticateUser(LoginRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException, InvalidCaptchaException;

    AuthResponse handleMfaAuthentication(User user, AuthenticationRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException;

    MfaToken initializeMfaToken(User user);

    AuthResponse initiateMfaChallenge(User user);

    AuthResponse verifyMfaAndLogin(MfaVerificationRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException;

    AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint);

    void handleFailedLogin(User user, String ipAddress);

    void handleLockedAccount(User user, String ipAddress);

    void resetFailedAttempts(User user);

    void increaseFailedAttempts(User user);

    void lockUser(User user);

    void unlockUser(String username);

    boolean unlockWhenTimeExpired(User user);

    Authentication createSuccessfulAuthentication(User user);

//    String generateDeviceFingerprint(LoginRequest request);

    String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight);

    String determineUserScope();

    Duration getAccessTokenDuration();

    Duration getRefreshTokenDuration();

    Duration getJtiDuration();
}

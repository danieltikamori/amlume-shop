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

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.exceptions.DeviceFingerprintAdditionException;
import me.amlu.shop.amlume_shop.exceptions.DeviceFingerprintMismatchException;
import me.amlu.shop.amlume_shop.exceptions.DeviceFingerprintRegistrationException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;

public interface DeviceFingerprintService {

    void registerDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) throws DeviceFingerprintRegistrationException;


    void deleteDeviceFingerprint(String userId, Long fingerprintId);

    void deleteDeviceFingerprint(User user, Long fingerprintId);

    String generateDeviceFingerprint(String userAgent, String screenWidth, String screenHeight, HttpServletRequest request);

    void verifyDeviceFingerprint(String tokenFingerprint, HttpServletRequest request, String userId) throws DeviceFingerprintMismatchException, UserNotFoundException;

//    void storeOrUpdateFingerprint(User user, String deviceFingerprint);

    void checkDeviceLimit(User user);

    void validateInput(User user, String deviceFingerprint);

    boolean verifyDevice(User user, String fingerprint);

    void trustDevice(long userId, String fingerprint);

    void storeOrUpdateFingerprint(User user, String accessToken, String refreshToken, String deviceFingerprint);

    void updateExistingFingerprint(UserDeviceFingerprint fingerprint,
                                   String accessToken,
                                   String refreshToken);

    void createNewFingerprint(User user,
                              String accessToken,
                              String refreshToken,
                              String deviceFingerprint);

    void markDeviceSuspicious(String userId, String fingerprint);

    void revokeAllDevices(String userId, String exceptFingerprint);

    void deactivateDevice(String userId, String fingerprint);

    void disableDeviceFingerprinting(User user);

    void enableDeviceFingerprinting(User user);
}

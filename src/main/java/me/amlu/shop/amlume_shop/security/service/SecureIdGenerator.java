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

import me.amlu.shop.amlume_shop.exceptions.MfaException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecureIdGenerator {
    private static final int ID_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureIdGenerator() {
        // Prevent instantiation
        throw new AssertionError("This class should not be instantiated");
    }

    public static String generateSecureId() {
        byte[] randomBytes = new byte[ID_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // For time-based challenge IDs
    public static String generateTimeBasedId(String username) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = generateSecureId().substring(0, 16);
        String data = username + timestamp + randomPart;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "Error generating secure ID", e);
        }
    }
}

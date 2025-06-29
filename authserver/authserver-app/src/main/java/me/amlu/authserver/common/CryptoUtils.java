/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility methods for cryptographic operations.
 */
public final class CryptoUtils {
    private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Generates a random byte array of the specified length.
     *
     * @param length The length of the byte array
     * @return The random byte array
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates a random hex string of the specified length.
     *
     * @param length The length of the hex string (in bytes)
     * @return The random hex string
     */
    public static String generateRandomHex(int length) {
        byte[] bytes = generateRandomBytes(length);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Computes the SHA-256 hash of a string.
     *
     * @param input The input string
     * @return The hash as a hex string
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Computes the HMAC-SHA256 of a message.
     *
     * @param message The message
     * @param key     The key
     * @return The HMAC as a hex string
     */
    public static String hmacSha256(String message, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            log.error("HMAC-SHA256 computation failed", e);
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    /**
     * Generates a secure random API key.
     *
     * @param length The length of the key in bytes
     * @return The API key as a Base64 string
     */
    public static String generateApiKey(int length) {
        byte[] keyBytes = generateRandomBytes(length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }

    /**
     * Generates a secure AES key.
     *
     * @param keySize The key size in bits (128, 192, or 256)
     * @return The AES key
     */
    public static SecretKey generateAesKey(int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize, SECURE_RANDOM);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            log.error("AES algorithm not available", e);
            throw new RuntimeException("AES algorithm not available", e);
        }
    }

    /**
     * Performs a constant-time comparison of two strings.
     * This helps prevent timing attacks when comparing sensitive values.
     *
     * @param a The first string
     * @param b The second string
     * @return true if the strings are equal
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}

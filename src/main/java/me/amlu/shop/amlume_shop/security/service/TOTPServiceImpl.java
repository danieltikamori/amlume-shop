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

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import me.amlu.shop.amlume_shop.security.config.properties.MfaProperties;
import me.amlu.shop.amlume_shop.exceptions.TotpVerificationException;
import me.amlu.shop.amlume_shop.user_management.User;
import org.bouncycastle.util.encoders.Base32;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;

@Service
public class TOTPServiceImpl implements TOTPService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TOTPServiceImpl.class);
    // --- Configuration ---
    private final MfaProperties mfaProperties;

    // --- Dependencies ---
    private final TimeBasedOneTimePasswordGenerator totpGenerator;
    // Removed: private final TextEncryptor textEncryptor; - Should be injected if needed elsewhere, but not for core TOTP logic

    /**
     * Constructor for TOTPServiceImpl.
     *
     * @param totpGenerator The underlying TOTP generator library instance.
     *                      (Consider creating this bean elsewhere based on configured parameters if needed)
     */
    public TOTPServiceImpl(MfaProperties mfaProperties, TimeBasedOneTimePasswordGenerator totpGenerator
            /* Inject other dependencies if needed, but not TextEncryptor here */) {
        this.mfaProperties = mfaProperties;
        this.totpGenerator = Objects.requireNonNull(totpGenerator, "totpGenerator cannot be null");

        // Example of creating totpGenerator based on config (if not injected as a pre-configured bean)
        /*
        try {
            this.totpGenerator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(timeStepSeconds), otpLength, algorithm);
        } catch (NoSuchAlgorithmException e) {
            log.error("FATAL: Could not initialize TOTPGenerator with algorithm '{}'", algorithm, e);
            throw new IllegalStateException("Failed to initialize TOTPGenerator", e);
        }
        */

        // Removed: TextEncryptor initialization - Decryption should happen before calling verifyCode
        // this.textEncryptor = Encryptors.stronger(encryptionPassword, salt);
    }

    /**
     * Generates a new Base32 encoded secret key suitable for TOTP.
     *
     * @return A Base32 encoded secret key string.
     */
    @Override
    public String generateSecretKey() {
        // 160 bits (20 bytes) is common for HmacSHA1. Use 32 bytes for SHA256, 64 for SHA512.
        int keyLengthBytes = switch (mfaProperties.getAlgorithm().toUpperCase()) {
            case "HMACSHA256" -> 32;
            case "HMACSHA512" -> 64;
            default -> 20; // Default for SHA1
        };
        byte[] key = KeyGenerators.secureRandom(keyLengthBytes).generateKey();
        String base32Key = new String(Base32.encode(key), StandardCharsets.UTF_8);
        log.debug("Generated new TOTP secret key (Base32 encoded)");
        return base32Key;
    }

    /**
     * Generates the provisioning URI (otpauth:// URL) for QR code generation.
     *
     * @param user      The user for whom the QR code is generated.
     * @param secretKey The plain (Base32 encoded) secret key.
     * @param issuer    The issuer name (e.g., application name) to display in the authenticator app.
     * @return The otpauth:// URL string.
     */
    @Override
    public String generateQrCodeUrl(User user, String secretKey, String issuer) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(user.getUsername(), "Username cannot be null");
        Objects.requireNonNull(secretKey, "Secret key cannot be null");
        Objects.requireNonNull(issuer, "Issuer cannot be null");

        // URL Encode issuer and username for safety
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedUsername = URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8).replace("+", "%20");

        // Ensure secretKey doesn't contain padding characters that might cause issues (though Base32 usually doesn't add them)
        String url = getUrlString(secretKey, encodedIssuer, encodedUsername);
        log.debug("Generated QR Code URL for user: {}", user.getUsername());
        return url;

    }

    @NotNull
    private String getUrlString(String secretKey, String encodedIssuer, String encodedUsername) {
        String safeSecretKey = secretKey.replace("=", "");

        // Construct the otpauth URL
        // Standard parameters: secret, issuer
        // Optional: algorithm (SHA1, SHA256, SHA512), digits (6 or 8), period (usually 30)
        String url = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                encodedIssuer, // Label: Issuer:Username
                encodedUsername,
                safeSecretKey,   // The Base32 secret itself
                encodedIssuer,   // Issuer parameter
                mfaProperties.getAlgorithm().replace("Hmac", ""), // Algorithm: SHA1, SHA256, SHA512
                mfaProperties.getCodeLength(),       // Digits: 6 or 8
                mfaProperties.getTimeStepSeconds()  // Period: e.g., 30
        );
        return url;
    }

    /**
     * Verifies a TOTP code against the provided plain secret.
     * Checks the current time window and a configurable number of adjacent windows (for clock skew).
     *
     * @param plainSecret The plain (Base32 encoded) secret key.
     * @param code        The TOTP code entered by the user.
     * @return {@code true} if the code is valid for the current or adjacent time windows, {@code false} otherwise.
     * @throws TotpVerificationException if the secret key is invalid or another verification error occurs.
     */
    @Override
    public boolean verifyCode(String plainSecret, String code) throws TotpVerificationException {
        Objects.requireNonNull(plainSecret, "Plain secret cannot be null");
        Objects.requireNonNull(code, "Code cannot be null");

        if (code.length() != mfaProperties.getCodeLength() || !code.matches("\\d+")) {
            log.warn("Invalid code format received. Length: {}, Content: {}", code.length(), code.replaceAll("\\d", "*"));
            return false; // Invalid format, definitely not a valid code
        }

        String substring = plainSecret.substring(0, Math.min(plainSecret.length(), 4));

        try {
            // 1. Decode the Base32 Secret
            byte[] secretBytes = Base32.decode(plainSecret);
            SecretKey secretKey = new SecretKeySpec(secretBytes, mfaProperties.getAlgorithm());

            // 2. Calculate Time Window
            // Use Instant for time calculations
            Instant now = Instant.now();
            long currentWindow = now.getEpochSecond() / mfaProperties.getTimeStepSeconds();

            // 3. Check Codes in Window(s)
            // Check current window, plus 'windowSize' windows before and after
            for (int i = -mfaProperties.getWindowSize(); i <= mfaProperties.getWindowSize(); ++i) {
                long checkWindow = currentWindow + i;
                Instant checkTime = Instant.ofEpochSecond(checkWindow * mfaProperties.getTimeStepSeconds());

                // Generate OTP for the specific time instant
                // Ensure totpGenerator is configured with the correct algorithm, length, and time step
                long otp = totpGenerator.generateOneTimePassword(secretKey, checkTime);

                // Format OTP to the expected length with leading zeros
                String expectedCode = String.format("%0" + mfaProperties.getCodeLength() + "d", otp);

                // Securely compare the expected code with the provided code
                if (MessageDigest.isEqual(expectedCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
                    log.debug("TOTP code verified successfully for window offset: {}", i);
                    return true; // Code matches
                }
            }

            // 4. Code did not match any window
            log.warn("Invalid TOTP code provided.");
            return false;

        } catch (InvalidKeyException e) {
            log.error("Error verifying TOTP code due to an invalid key (length/format mismatch for algorithm '{}'). Secret prefix: {}", mfaProperties.getAlgorithm(), substring, e);
            throw new TotpVerificationException("Invalid secret key configuration.", e);
        } catch (IllegalArgumentException e) {
            // Catch potential errors from Base32.decode
            log.error("Error verifying TOTP code due to illegal argument (likely Base32 decoding failed). Secret prefix: {}", substring, e);
            throw new TotpVerificationException("Invalid secret key format (Base32 decoding failed).", e);
        } catch (Exception e) {
            // Catch any other unexpected errors during generation/comparison
            log.error("An unexpected error occurred during TOTP code verification.", e);
            throw new TotpVerificationException("TOTP verification failed unexpectedly.", e);
        }
    }
}

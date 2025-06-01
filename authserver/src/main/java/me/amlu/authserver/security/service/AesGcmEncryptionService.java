/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import me.amlu.authserver.exceptions.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service implementation for AES/GCM encryption and decryption.
 * <p>
 * This service uses AES with Galois/Counter Mode (GCM) which provides both confidentiality
 * and authenticity. It requires a securely managed encryption key and uses a randomly generated
 * Initialization Vector (IV) for each encryption operation, prepending the IV to the ciphertext.
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 *     <li>Initializing with a Base64 encoded AES key from configuration.</li>
 *     <li>Encrypting byte array data, prepending a 12-byte IV.</li>
 *     <li>Decrypting byte array data that includes the prepended IV.</li>
 *     <li>Measuring encryption and decryption operation durations using Micrometer.</li>
 * </ul>
 * </p>
 * <h2>Key Management:</h2>
 * <p>The encryption key ({@code app.security.encryption.passkey-data-key}) is critical and
 * <strong>must</strong> be managed securely (e.g., stored in HashiCorp Vault and loaded at runtime).
 * It should be a strong, randomly generated key (16, 24, or 32 bytes for AES-128, AES-192, or AES-256 respectively).
 * Key rotation strategies should be considered for long-term security.</p>
 *
 * <h2>Usage with JPA AttributeConverter:</h2>
 * <p>This service is typically used by a JPA {@link jakarta.persistence.AttributeConverter}
 * (like {@link me.amlu.authserver.security.util.EncryptedByteArrayConverter}) to automatically
 * encrypt/decrypt entity fields before persisting to or after reading from the database.</p>
 *
 * @see EncryptionService
 * @see me.amlu.authserver.security.util.EncryptedByteArrayConverter
 * @see EncryptionException
 */
@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits is recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // AES block size, common for GCM

    private final SecretKey secretKey;
    private final Timer encryptionTimer;
    private final Timer decryptionTimer;

    /**
     * Constructs the {@code AesGcmEncryptionService}.
     * <p>
     * Initializes the AES secret key from the provided Base64 encoded string and sets up
     * Micrometer timers for monitoring encryption and decryption performance.
     * </p>
     *
     * @param encryptionKeyBase64 The encryption key, Base64 encoded. Must correspond to a valid AES key length
     *                            (128, 192, or 256 bits) when decoded. This key should be sourced securely,
     *                            typically from Vault via {@code @Value("${app.security.encryption.passkey-data-key}")}.
     * @param meterRegistry       The Micrometer registry for creating performance timers.
     * @throws IllegalArgumentException if the encryption key is invalid (null, empty, not Base64, or incorrect length).
     */
    public AesGcmEncryptionService(@Value("${app.security.encryption.passkey-data-key}") String encryptionKeyBase64,
                                   MeterRegistry meterRegistry) {
        Assert.hasText(encryptionKeyBase64, "Encryption key cannot be null or empty.");
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(encryptionKeyBase64);
        } catch (IllegalArgumentException e) {
            log.error("Encryption key is not valid Base64: {}", e.getMessage());
            throw new IllegalArgumentException("Encryption key is not valid Base64.", e);
        }

        if (decodedKey.length != 16 && decodedKey.length != 24 && decodedKey.length != 32) {
            log.error("Invalid encryption key length: {} bytes. Must be 16, 24, or 32 bytes for AES-128, AES-192, or AES-256.", decodedKey.length);
            throw new IllegalArgumentException("Invalid AES key length. Must be 128, 192, or 256 bits.");
        }
        this.secretKey = new SecretKeySpec(decodedKey, "AES");

        this.encryptionTimer = Timer.builder("authserver.encryption.service")
                .tag("operation", "encrypt")
                .description("Time taken to encrypt data")
                .register(meterRegistry);
        this.decryptionTimer = Timer.builder("authserver.encryption.service")
                .tag("operation", "decrypt")
                .description("Time taken to decrypt data")
                .register(meterRegistry);
        log.info("AesGcmEncryptionService initialized with key length: {} bits and metrics.", decodedKey.length * 8);
    }

    /**
     * Encrypts the given byte array using AES/GCM.
     * A new 12-byte Initialization Vector (IV) is generated for each encryption operation
     * and prepended to the resulting ciphertext.
     *
     * @param data The raw byte array to be encrypted. If null, null is returned.
     * @return A byte array containing the IV prepended to the ciphertext, or null if input was null.
     * @throws EncryptionException if any error occurs during the encryption process.
     */
    @Override
    public byte[] encrypt(byte[] data) throws EncryptionException {
        if (data == null) {
            return null;
        }
        try {
            return encryptionTimer.recordCallable(() -> {
                try {
                    byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
                    SecureRandom random = SecureRandom.getInstanceStrong(); // Prefer strong instance
                    random.nextBytes(iv);

                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

                    byte[] cipherText = cipher.doFinal(data);

                    ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
                    byteBuffer.put(iv);
                    byteBuffer.put(cipherText);
                    return byteBuffer.array();
                } catch (Exception e) { // Catches checked exceptions from crypto operations
                    log.error("Error during AES/GCM encryption: {}", e.getMessage(), e);
                    throw new EncryptionException("Encryption failed", e); // Re-throws as unchecked EncryptionException
                }
            });
        } catch (EncryptionException e) { // Catch the EncryptionException rethrown by the lambda
            throw e;
        } catch (Exception e) { // Catch any other Exception that recordCallable itself might throw
            log.error("Unexpected error from Timer.recordCallable during encryption: {}", e.getMessage(), e);
            throw new EncryptionException("Timer recording failed during encryption", e);
        }
    }

    /**
     * Decrypts the given byte array which is expected to contain an IV prepended to the AES/GCM ciphertext.
     *
     * @param encryptedDataWithIv The byte array containing the IV and ciphertext. If null, null is returned.
     *                            Must be at least {@value #GCM_IV_LENGTH_BYTES} bytes long.
     * @return The original decrypted byte array, or null if input was null.
     * @throws EncryptionException if decryption fails due to issues like incorrect key, tampered data,
     *                             invalid IV, or if the input data is too short.
     */
    @Override
    public byte[] decrypt(byte[] encryptedDataWithIv) throws EncryptionException {
        if (encryptedDataWithIv == null) {
            return null;
        }
        try {
            return decryptionTimer.recordCallable(() -> {
                if (encryptedDataWithIv.length < GCM_IV_LENGTH_BYTES) {
                    log.error("Invalid encrypted data length: {} bytes. Too short to contain IV ({} bytes).", encryptedDataWithIv.length, GCM_IV_LENGTH_BYTES);
                    throw new EncryptionException("Invalid encrypted data: too short to contain IV.");
                }
                try {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedDataWithIv);

                    byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
                    byteBuffer.get(iv);

                    byte[] cipherText = new byte[byteBuffer.remaining()];
                    byteBuffer.get(cipherText);

                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

                    return cipher.doFinal(cipherText);
                } catch (Exception e) { // Catches checked exceptions from crypto operations (e.g., AEADBadTagException)
                    log.error("Error during AES/GCM decryption: {}", e.getMessage(), e);
                    // Do not log encryptedDataWithIv directly in production if it's sensitive
                    throw new EncryptionException("Decryption failed. Data may be tampered or key may be incorrect.", e);
                }
            });
        } catch (EncryptionException e) { // Catch the EncryptionException rethrown by the lambda
            throw e;
        } catch (Exception e) { // Catch any other Exception that recordCallable itself might throw
            log.error("Unexpected error from Timer.recordCallable during decryption: {}", e.getMessage(), e);
            throw new EncryptionException("Timer recording failed during decryption", e);
        }
    }
}

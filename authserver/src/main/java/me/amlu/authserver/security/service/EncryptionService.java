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

import me.amlu.authserver.exceptions.EncryptionException;

/**
 * Defines the contract for encryption and decryption services.
 *
 * <p>Implementations of this interface are responsible for securely encrypting and decrypting byte arrays.
 * This interface provides a common abstraction for various encryption algorithms and strategies
 * that might be used within the application.</p>
 *
 * <h2>How to Implement:</h2>
 * <p>When implementing this interface, ensure that:
 * <ul>
 *     <li>The chosen encryption algorithm is strong and appropriate for the data being protected.
 *         For example, AES-GCM is a good choice for symmetric encryption as shown in {@link AesGcmEncryptionService}.</li>
 *     <li>Keys are managed securely. Avoid hardcoding keys; prefer loading them from a secure configuration source
 *         like HashiCorp Vault or environment variables.</li>
 *     <li>Initialization Vectors (IVs) or nonces are handled correctly, typically generated randomly for each encryption
 *         and prepended or stored alongside the ciphertext.</li>
 *     <li>Exceptions are handled gracefully, and specific {@link EncryptionException} is thrown for errors
 *         during the encryption or decryption process.</li>
 *     <li>Consider thread-safety if the service instance is to be shared.</li>
 * </ul>
 * </p>
 *
 * <h2>Important Notes:</h2>
 * <ul>
 *     <li>The {@code encrypt} method should ideally handle null input gracefully, perhaps by returning null.</li>
 *     <li>The {@code decrypt} method should also handle null input and be robust against malformed or tampered
 *         encrypted data.</li>
 *     <li>It is crucial <em>not</em> to expose sensitive details (like raw exception messages from underlying crypto libraries)
 *         to clients in case of failures. The {@link EncryptionException} should carry a generic message,
 *         while detailed logs are kept internally. See {@link me.amlu.authserver.advice.GlobalSecurityExceptionHandler}
 *         for an example of how these exceptions can be handled at the controller level.</li>
 * </ul>
 *
 * @see AesGcmEncryptionService
 * @see EncryptionException
 */
public interface EncryptionService {

    /**
     * Encrypts the given byte array.
     *
     * <p>Implementations should ensure that the encryption process is secure,
     * typically involving a strong algorithm, proper key management, and correct
     * handling of initialization vectors (IVs) or nonces if required by the algorithm.
     * The IV, if used, is often prepended to the resulting ciphertext.</p>
     *
     * @param data The raw byte array to be encrypted. Can be {@code null}.
     * @return The encrypted byte array, which may include any necessary metadata like an IV.
     * Returns {@code null} if the input {@code data} is {@code null}.
     * @throws EncryptionException if an error occurs during the encryption process.
     *                             This could be due to misconfiguration, issues with the
     *                             underlying cryptographic operations, or invalid input data
     *                             (though basic null checks should be handled gracefully).
     */
    byte[] encrypt(byte[] data) throws EncryptionException;

    /**
     * Decrypts the given byte array.
     *
     * <p>Implementations must be able to correctly process the output of their corresponding
     * {@link #encrypt(byte[])} method. This typically involves extracting any prepended
     * IV or nonce, and then performing the decryption using the same algorithm and key.</p>
     *
     * @param encryptedData The byte array containing the encrypted data, potentially
     *                      prefixed with an IV or other metadata. Can be {@code null}.
     * @return The original, decrypted byte array.
     * Returns {@code null} if the input {@code encryptedData} is {@code null}.
     * @throws EncryptionException if an error occurs during the decryption process.
     *                             This could be due to an incorrect key, corrupted data,
     *                             tampered ciphertext (e.g., authentication tag mismatch in GCM),
     *                             or other cryptographic issues.
     */
    byte[] decrypt(byte[] encryptedData) throws EncryptionException;
}

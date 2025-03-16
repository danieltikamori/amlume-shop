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
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.model.User;
import org.bouncycastle.util.encoders.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TOTPServiceImpl implements TOTPService {

    private static final int TIME_STEP = 30;
    private static final int OTP_LENGTH = 6;
    private static final String ALGORITHM = "HmacSHA1";
    private final TimeBasedOneTimePasswordGenerator totpGenerator;
    private final TextEncryptor textEncryptor;


    public TOTPServiceImpl(TimeBasedOneTimePasswordGenerator totpGenerator, @Value("${mfa.encryption.password}") String encryptionPassword, @Value("${mfa.encryption.salt}") String salt) {
        this.totpGenerator = totpGenerator;

        this.textEncryptor = (TextEncryptor) Encryptors.stronger(encryptionPassword, salt); // or any other key derivation/salt method


    }

    @Override
    public String generateSecretKey() {
        byte[] key = KeyGenerators.secureRandom(20).generateKey(); // Adjust key size as needed
        return new String(Base32.encode(key));
    }


    @Override
    public String generateQrCodeUrl(User user, String secretKey, String issuer) {
        String company = issuer; // Or retrieve dynamically
        String username = user.getUsername();
        // ... existing QR code URL generation logic

//        String qrCodeData = String.format(
//                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
//                company, username, secretKey, company
//
//        );
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                company, username, secretKey, company

        );
    }


    @Override
    public boolean verifyCode(String encryptedSecret, String code) {
        try {
            String decryptedSecret = textEncryptor.decrypt(encryptedSecret);
            byte[] secretBytes = Base32.decode(decryptedSecret);
            SecretKey secretKey = new SecretKeySpec(secretBytes, ALGORITHM);

            long time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            long timeWindow = time / TIME_STEP;

            for (int i = -1; i <= 1; ++i) {  // Check previous, current, and next time timeWindowMinutes
                long checkTime = timeWindow + i;
                long otp = totpGenerator.generateOneTimePassword(secretKey, Instant.ofEpochSecond(checkTime * TIME_STEP));

                if (String.format("%0" + OTP_LENGTH + "d", otp).equals(code)) {
                    return true;
                }
            }

        } catch (InvalidKeyException | IllegalArgumentException e) {
            log.error("Error verifying TOTP code: {}", e.getMessage());

        }
        return false;
    }


}

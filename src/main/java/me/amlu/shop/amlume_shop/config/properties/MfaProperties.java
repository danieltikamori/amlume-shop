/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

//import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.DEFAULT_ISSUER;

@ConfigurationProperties("mfa")
@Component // Make it a Spring-managed bean
public class MfaProperties {

    // --- Properties ---

//    private String issuer = DEFAULT_ISSUER;

    @NotBlank
    String mfaEncryptionPassword; // Assuming this is set in the application properties or Vault

    @NotBlank
    String mfaEncryptionSalt;

    @Min(1)
    private int timeStepSeconds = 30;

    @Min(6)
    private int codeLength = 6;

    @NotBlank
    private String algorithm = "HmacSHA1"; // e.g., HmacSHA1, HmacSHA256, HmacSHA512

    @Min(0)
    private int windowSize = 1; // Number of windows to check before/after current (1 means -1, 0, +1)

    // --- Getters and Setters ---

//    public String getIssuer() {
//        return issuer;
//    }

//    public void setIssuer(String issuer) {
//        this.issuer = issuer;
//    }

    public String getMfaEncryptionPassword() {
        return mfaEncryptionPassword;
    }

    public void setMfaEncryptionPassword(String mfaEncryptionPassword) {
        this.mfaEncryptionPassword = mfaEncryptionPassword;
    }

    public String getMfaEncryptionSalt() {
        return mfaEncryptionSalt;
    }

    public void setMfaEncryptionSalt(String mfaEncryptionSalt) {
        this.mfaEncryptionSalt = mfaEncryptionSalt;
    }

    public int getTimeStepSeconds() {
        return timeStepSeconds;
    }

    public void setTimeStepSeconds(int timeStepSeconds) {
        this.timeStepSeconds = timeStepSeconds;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }
}
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
import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.exceptions.UserLockedException;
import me.amlu.shop.amlume_shop.model.MfaSettings;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.repositories.MfaSettingsRepository;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants.DEFAULT_ISSUER;

@Service
@Slf4j
public class MfaServiceImpl extends BaseService implements MfaService {

    private static final int MAX_ATTEMPTS = 3;     // Configure as needed
    private static final int LOCK_TIME_MINUTES = 15;    // Configure as needed

    private final PasetoTokenServiceImpl pasetoTokenService;
    private final MfaSettingsRepository mfaSettingsRepository;
    private final MfaTokenRepository mfaTokenRepository;

    private final TOTPService totpService;
    private final TimeBasedOneTimePasswordGenerator totpGenerator;

    public MfaServiceImpl(PasetoTokenServiceImpl pasetoTokenService, MfaSettingsRepository mfaSettingsRepository, MfaTokenRepository mfaTokenRepository, TOTPService totpService, TimeBasedOneTimePasswordGenerator totpGenerator) {
        this.pasetoTokenService = pasetoTokenService;
        this.mfaSettingsRepository = mfaSettingsRepository;
        this.mfaTokenRepository = mfaTokenRepository;
        this.totpService = totpService;
        this.totpGenerator = totpGenerator;
    }

    @Override
    public boolean isMfaEnabled(User user) {
        return mfaTokenRepository.findByUser(user).isPresent();

    }

    @Override
    public boolean isMfaEnforced(User user) {
        Optional<MfaSettings> settings = mfaSettingsRepository.findById(1L); // Assuming only one setting. Could use a different approach for multiple settings.
        return settings.map(MfaSettings::isMfaEnforced).orElse(false); // Default to false if no setting found
    }

    @Override
    public void updateMfaEnforced(boolean enforced) {
        MfaSettings settings = mfaSettingsRepository.findById(1L)
                .orElse(new MfaSettings()); // Create if not found
        settings.setMfaEnforced(enforced);
        mfaSettingsRepository.save(settings);
    }

    @Override
    public String generateSecretKey() {
        return totpService.generateSecretKey();
    }

    @Override
    public String generateQrCodeImageUrl(User user, String encryptedSecret) {
        return totpService.generateQrCodeUrl(user, encryptedSecret, DEFAULT_ISSUER);
    }

    @Override
    public void recordFailedAttempt(User user) {

    }

    @Override
    public boolean isUserLocked(User user) throws ExecutionException {
        return false;
    }

    @Override
    public void resetFailedAttempts(User user) {

    }


    @Override
    public boolean shouldLockAccount(User user) {
        return false;
    }

    @Override
    public void lockAccount(String userId) {

    }

    @Override
    public boolean verifyCode(String secret, String code) throws TooManyAttemptsException {
        try {
            User user = userService.getCurrentUser();
            if (isUserLocked(user)) { // isUserLocked now checks if user is locked due to failed MFA attempts only
                throw new UserLockedException("Too many failed attempts. Try again later.");
            }

            boolean isValid = totpService.verifyCode(secret, code); // Verification logic in TOTPService

            if (!isValid) {

            }

            return isValid;
        } catch (ExecutionException | UserLockedException e) {

            throw new RuntimeException(e);
        }


    }

    @Override
    @Transactional
    public void enableMfaForUser(User user) {
        String secret = generateSecretKey();
        MfaToken mfaToken = MfaToken.builder().user(user).secret(secret).enabled(true).build(); // Set enabled = true
        mfaTokenRepository.save(mfaToken);
    }

    @Override
    public void disableMfaForUser(User user) {
        mfaTokenRepository.deleteByUser(user);
    }

    @Override
    public void resetMfaForUser(User user) {

    }

    @Override
    public void updateSecretForUser(User user, String encryptedSecret) {

    }

    @Override
    public void updateMfaTokenForUser(User user, String mfaToken) {

    }

    @Override
    public void deleteMfaTokenForUser(User user) {

    }

    @Override
    public void deleteSecretForUser(User user) {

    }

    @Override
    public void deleteAllMfaTokens() {

    }

    @Override
    public void deleteAllSecrets() {

    }

    @Override
    public void deleteAllMfaSettings() {

    }

    @Override
    public void deleteAllMfaData() {

    }

    @Override
    public void deleteAllMfaDataForUser(User user) {

    }

    @Override
    public void deleteAllMfaDataForAllUsers() {

    }

    @Override
    public void deleteAllMfaDataForAllUsersAndSecrets() {

    }

    @Override
    public void deleteAllMfaDataForAllUsersAndSecretsAndTokens() {

    }

    @Override
    public void deleteAllMfaDataForAllUsersAndSecretsAndTokensAndSettings() {

    }

    @Override
    public boolean isRateLimitExceeded(User user) {
        return false;
    }


    private String getRateLimitKey(String username) {
        return "mfa:" + username;
    }

}

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

import me.amlu.shop.amlume_shop.exceptions.TooManyAttemptsException;
import me.amlu.shop.amlume_shop.user_management.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

public interface MfaService {
//    boolean validateMfaCode(String challengeId, String code, User user);

    boolean isMfaEnabled(User user);

    boolean verifyCode(String secret, String code) throws TooManyAttemptsException, ExecutionException;

    void updateMfaEnforced(boolean forced);
    boolean isMfaEnforced(User user);
    String generateSecretKey();


    String generateQrCodeImageUrl(User user, String encryptedSecret);

    void recordFailedAttempt(User user);

    boolean isUserLocked(User user) throws ExecutionException;

    void resetFailedAttempts(User user);

    void enableMfaForUser(User user);
    void disableMfaForUser(User user);
    void resetMfaForUser(User user);
    void updateSecretForUser(User user, String encryptedSecret);
//    void updateMfaTokenForUser(User user, String mfaToken);
    void deleteMfaTokenForUser(User user);
    void deleteSecretForUser(User user);
    void deleteAllMfaTokens();
    void deleteAllSecrets();
    void deleteAllMfaSettings();
    void deleteAllMfaData();
    void deleteAllMfaDataForUser(User user);
    void deleteAllMfaDataForAllUsers();
    void deleteAllMfaDataForAllUsersAndSecrets();
    void deleteAllMfaDataForAllUsersAndSecretsAndTokens();
    void deleteAllMfaDataForAllUsersAndSecretsAndTokensAndSettings();

    boolean isRateLimitExceeded(User user);
}

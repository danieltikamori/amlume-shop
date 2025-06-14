/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.exceptions;

import lombok.Getter;
import me.amlu.shop.amlume_shop.exceptions.BaseException;

import java.io.Serial;

@Getter
public class MfaException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum MfaErrorType {
        TOKEN_NOT_FOUND,
        INVALID_TOKEN,
        INVALID_CODE,
        INVALID_METHOD,
        ENFORCEMENT_FAILED,
        SETUP_FAILED,
        VERIFICATION_FAILED,
        CHALLENGE_FAILED,
        RECOVERY_CODE_ERROR,
        RECOVERY_CODE_USAGE_ERROR,
        RECOVERY_CODE_GENERATION_ERROR,
        RECOVERY_CODE_DELETION_ERROR,
        RECOVERY_CODE_EXPIRATION_ERROR,
        RECOVERY_CODE_INVALIDATION_ERROR,
        RECOVERY_CODE_RECOVERY_ERROR,
        RECOVERY_CODE_RESET_ERROR,
        RECOVERY_CODE_VERIFICATION_ERROR,
        RECOVERY_CODE_LIST_ERROR,
        RECOVERY_CODE_COUNT_ERROR,
        AUTHENTICATION_ERROR, VERIFICATION_ERROR, RECOVERY_CODE_CREATION_ERROR
    }

    private final MfaErrorType errorType;

    public MfaException(MfaErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public MfaException(MfaErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

}

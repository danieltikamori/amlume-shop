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
import me.amlu.authserver.common.TokenConstants;

// Constants as enum for better organization and type safety
@Getter
public enum ErrorMessages {

    // PASETO token validation errors
    INVALID_TOKEN_SIGNATURE("Invalid token signature"),
    INVALID_TOKEN("Invalid token"),
    KEY_CONVERSION_ALGORITHM_NOT_AVAILABLE(TokenConstants.KEY_CONVERSION_ALGORITHM + " algorithm not available"),
    INVALID_TOKEN_FORMAT("Invalid token format"),
    INVALID_PASETO_TOKEN("Invalid PASETO Token"),
    INVALID_REFRESH_TOKEN("Invalid refresh token"),
    INVALID_KEY_ID("Invalid key ID"),
    KID_MISSING("kid is missing in the token footer"),
    TOKEN_VALIDATION_FAILED("Token validation failed"),
    ERROR_PARSING_CLAIMS("Error parsing claims"),
    FAILED_TO_SERIALIZE_CLAIMS("Failed to serialize claims"),
    KID_IS_MISSING_IN_THE_TOKEN_FOOTER("kid is missing in the token footer");


    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}

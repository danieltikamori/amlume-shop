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

import java.io.Serial;

public class TokenValidationFailureException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TokenValidationFailureException(String failedToValidateToken) {
        super("Token validation failed: " + failedToValidateToken);
    }

    public TokenValidationFailureException(String failedToValidateToken, Throwable cause) {
        super(failedToValidateToken, cause);
    }

    public TokenValidationFailureException(Throwable cause) {
        super(cause);
    }

    public TokenValidationFailureException() {
        super("Token validation failed");
    }

    public TokenValidationFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

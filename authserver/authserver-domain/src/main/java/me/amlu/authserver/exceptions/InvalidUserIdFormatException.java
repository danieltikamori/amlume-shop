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

public class InvalidUserIdFormatException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidUserIdFormatException(String invalidUserIdFormat, NumberFormatException e) {
        super(invalidUserIdFormat, e);
    }

    public InvalidUserIdFormatException(String invalidUserIdFormat, Exception e) {
        super(invalidUserIdFormat, e);
    }

    public InvalidUserIdFormatException(String invalidUserIdFormat) {
        super(invalidUserIdFormat);
    }

    public InvalidUserIdFormatException(String invalidUserIdFormat, Throwable cause) {
        super(invalidUserIdFormat, cause);
    }

    public InvalidUserIdFormatException(Throwable cause) {
        super(cause);
    }

    public InvalidUserIdFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

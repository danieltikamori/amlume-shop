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

public class MfaAlreadyDisabledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MfaAlreadyDisabledException(String message) {
        super(message);
    }

    public MfaAlreadyDisabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public MfaAlreadyDisabledException(Throwable cause) {
        super(cause);
    }

    public MfaAlreadyDisabledException() {
        super("MFA is already disabled.");
    }

    public MfaAlreadyDisabledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

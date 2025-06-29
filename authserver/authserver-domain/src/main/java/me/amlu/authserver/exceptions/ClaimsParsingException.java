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

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serial;

public class ClaimsParsingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ClaimsParsingException(String errorParsingClaims, JsonProcessingException e) {

        super(errorParsingClaims, e);
    }

    public ClaimsParsingException(String errorParsingClaims) {

        super(errorParsingClaims);
    }

    public ClaimsParsingException(String errorParsingClaims, Throwable cause) {

        super(errorParsingClaims, cause);
    }

    public ClaimsParsingException(Throwable cause) {

        super(cause);
    }

    protected ClaimsParsingException(String errorParsingClaims, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {

        super(errorParsingClaims, cause, enableSuppression, writableStackTrace);
    }
}

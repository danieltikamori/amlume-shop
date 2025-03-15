/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.exceptions;

import java.io.Serial;

public class ClaimsExtractionFailureException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ClaimsExtractionFailureException(String errorExtractingClaimsFromToken, Exception e) {
        super(errorExtractingClaimsFromToken, e);
    }

    public ClaimsExtractionFailureException(String errorExtractingClaimsFromToken) {
        super(errorExtractingClaimsFromToken);
    }

    public ClaimsExtractionFailureException(Exception e) {
        super(e);
    }

    public ClaimsExtractionFailureException() {
        super();
    }

    public ClaimsExtractionFailureException(String errorExtractingClaimsFromToken, Throwable cause) {
        super(errorExtractingClaimsFromToken, cause);
    }

    public ClaimsExtractionFailureException(Throwable cause) {
        super(cause);
    }

    protected ClaimsExtractionFailureException(String errorExtractingClaimsFromToken, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(errorExtractingClaimsFromToken, cause, enableSuppression, writableStackTrace);
    }

}

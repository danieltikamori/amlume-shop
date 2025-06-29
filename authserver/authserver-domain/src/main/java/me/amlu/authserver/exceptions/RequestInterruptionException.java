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

public class RequestInterruptionException extends RuntimeException {
    public static final String REQUEST_WAS_INTERRUPTED = "Request was interrupted";
    public static final String REQUEST_WAS_INTERRUPTED_FOR_USER = "Request was interrupted for user: ";

    @Serial
    private static final long serialVersionUID = 1L;

    public RequestInterruptionException(String userId, InterruptedException e) {
        super(REQUEST_WAS_INTERRUPTED_FOR_USER + userId, e);
    }

    public RequestInterruptionException(String userId, RuntimeException e) {
        super(REQUEST_WAS_INTERRUPTED_FOR_USER + userId, e);
    }

    public RequestInterruptionException(String userId, Exception e) {
        super(REQUEST_WAS_INTERRUPTED_FOR_USER + userId, e);
    }

    public RequestInterruptionException(String userId, Throwable e) {
        super(REQUEST_WAS_INTERRUPTED_FOR_USER + userId, e);
    }

    public RequestInterruptionException(String userId, String message) {
        super(REQUEST_WAS_INTERRUPTED_FOR_USER + userId + " with message: " + message);
    }

    public RequestInterruptionException(String message) {
        super(message);
    }
}

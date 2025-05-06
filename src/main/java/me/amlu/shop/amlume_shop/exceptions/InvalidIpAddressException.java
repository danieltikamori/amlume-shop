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

public class InvalidIpAddressException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEVICE_FINGERPRINT_MESSAGE = " for device fingerprint: ";

    public InvalidIpAddressException(String deviceFingerprint) {
        super(deviceFingerprint);
    }

    public InvalidIpAddressException(String deviceFingerprint, String message) {
        super(message + DEVICE_FINGERPRINT_MESSAGE + deviceFingerprint);
    }

    public InvalidIpAddressException(String deviceFingerprint, String message, Throwable cause) {
        super(message + DEVICE_FINGERPRINT_MESSAGE + deviceFingerprint, cause);
    }

    public InvalidIpAddressException(String deviceFingerprint, Throwable cause) {
        super("Invalid IP address for device fingerprint: " + deviceFingerprint, cause);
    }

    public InvalidIpAddressException(String deviceFingerprint, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message + DEVICE_FINGERPRINT_MESSAGE + deviceFingerprint, cause, enableSuppression, writableStackTrace);
    }
}

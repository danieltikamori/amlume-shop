/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.passkey.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

public record GetPasskeyDetailResponse(
        String credentialId,
        String friendlyName,
        Instant createdAt,
        Instant lastUsedAt,
        Set<String> transports,
        Boolean backupEligible,
        Boolean backupState
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    // Can add static factory methods or compact constructors if needed,
    // but for a simple DTO, the above is often sufficient.

    // Example of a compact constructor for validation (if needed):
    /*
    public GetPasskeyDetailResponse {
        if (StringUtils.isBlank(credentialId)) {
            throw new IllegalArgumentException("credentialId cannot be blank");
        }
        // Add other validations if necessary
    }
    */
}

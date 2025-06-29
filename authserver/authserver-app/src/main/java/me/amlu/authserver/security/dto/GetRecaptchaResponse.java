/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the response from the Google reCAPTCHA verification API.
 * Implemented as an immutable record.
 */
public record GetRecaptchaResponse(
        boolean success,

        @JsonProperty("challenge_ts") // Maps JSON "challenge_ts" to this component
        String challenge_ts,

        String hostname,

        @JsonProperty("error-codes") // Maps JSON "error-codes" to this component
        List<String> errorCodes // Keep Java convention as camelCase
) {
}

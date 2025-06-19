/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.passkey.service.org.springframework.security.web.webauthn.jackson;

import org.springframework.stereotype.Component;

@Component
public class AuthenticatorSelectionCriteriaMixin {
    // This is a placeholder mixin.
    // The actual mixin logic for deserialization (e.g., handling enums like
    // AuthenticatorAttachment, ResidentKeyRequirement, UserVerificationRequirement)
    // is typically provided by Spring Security's WebauthnJackson2Module.
    // This empty mixin class exists primarily to allow the ObjectMapper
    // in PasskeyServiceImpl to register a mixin for this class,
    // potentially overriding or supplementing default deserialization behavior
    // if needed, or simply ensuring the class is known to the ObjectMapper
    // in a way that allows WebauthnJackson2Module's annotations/handlers to apply.

    // If you need custom deserialization logic for AuthenticatorSelectionCriteria
    // (e.g., handling specific edge cases or formats not covered by WebauthnJackson2Module),
    // you would add @JsonCreator or @JsonProperty annotations here, or potentially
    // register a custom Deserializer.

    // Example (illustrative - WebauthnJackson2Module likely handles this):
    /*
    @JsonCreator
    public AuthenticatorSelectionCriteriaMixin(
            @JsonProperty("authenticatorAttachment") AuthenticatorAttachment authenticatorAttachment,
            @JsonProperty("residentKey") ResidentKeyRequirement residentKey,
            @JsonProperty("userVerification") UserVerificationRequirement userVerification
    ) {
        // Constructor logic if needed for deserialization
    }
    */


}

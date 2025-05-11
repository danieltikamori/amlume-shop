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

import jakarta.validation.constraints.NotBlank;

public record PostPasskeyRegistrationRequest(
        @NotBlank(message = "Friendly name cannot be blank")
        String friendlyName,

        @NotBlank(message = "Credential ID (id) cannot be blank")
        String id,

        @NotBlank(message = "Raw ID (rawId) cannot be blank")
        String rawId,

        @NotBlank(message = "Type cannot be blank")
        String type,

        @NotBlank(message = "ClientDataJSON cannot be blank")
        String clientDataJSON,

        @NotBlank(message = "AttestationObject cannot be blank")
        String attestationObject,

        String authenticatorAttachment, // Can be null

        java.util.Map<String, Object> clientExtensionResults // Can be null or empty
) {
//    private String id; // Base64URL of rawId
//    private String rawId; // Base64URL
//    private String type; // e.g., "public-key"
//    private String clientDataJSON; // Base64URL
//    private String attestationObject; // Base64URL
//    private String authenticatorAttachment; // Optional: "platform" or "cross-platform"
//    private Map<String, Object> clientExtensionResults; // Optional
//    private String friendlyName; // User-provided name for this passkey

}
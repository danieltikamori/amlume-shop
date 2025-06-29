/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.webauthn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

/**
 * Simple implementation for WebAuthn authentication extensions client outputs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleAuthenticationExtensionsClientOutputs {

    private final Map<String, Object> extensions;

    @JsonCreator
    public SimpleAuthenticationExtensionsClientOutputs(Map<String, Object> extensions) {
        this.extensions = extensions != null ? extensions : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtension(String name) {
        return (T) this.extensions.get(name);
    }

    public Map<String, Object> getExtensions() {
        return Collections.unmodifiableMap(this.extensions);
    }
}

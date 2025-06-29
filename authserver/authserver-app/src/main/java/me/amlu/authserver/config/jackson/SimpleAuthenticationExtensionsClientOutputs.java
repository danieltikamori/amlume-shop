/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.jackson;

import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientOutput;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientOutputs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SimpleAuthenticationExtensionsClientOutputs implements AuthenticationExtensionsClientOutputs {
    private final Map<String, Object> extensions;

    public SimpleAuthenticationExtensionsClientOutputs() {
        this.extensions = Collections.emptyMap();
    }

    public SimpleAuthenticationExtensionsClientOutputs(Map<String, Object> extensions) {
        this.extensions = extensions != null ?
                Map.copyOf(extensions) :
                Collections.emptyMap();
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public List<AuthenticationExtensionsClientOutput<?>> getOutputs() {
        return Collections.emptyList();
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config.jackson.module;

import com.fasterxml.jackson.databind.module.SimpleModule;
import me.amlu.authserver.config.jackson.mixin.webauthn.AuthenticationExtensionsClientInputsMixIn;
import me.amlu.authserver.config.jackson.mixin.webauthn.AuthenticationExtensionsClientOutputsMixIn;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientOutputs;

import java.io.Serial;

public class WebAuthnCustomModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    public WebAuthnCustomModule() {
        super("WebAuthnCustomModule");

        // Register MixIns for WebAuthn classes
        setMixInAnnotation(AuthenticationExtensionsClientInputs.class, AuthenticationExtensionsClientInputsMixIn.class);
        setMixInAnnotation(AuthenticationExtensionsClientOutputs.class, AuthenticationExtensionsClientOutputsMixIn.class);
    }
}

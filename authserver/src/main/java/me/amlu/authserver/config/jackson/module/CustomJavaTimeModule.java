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
import me.amlu.authserver.config.jackson.serializer.DurationDeserializer;
import me.amlu.authserver.config.jackson.serializer.DurationSerializer;

import java.time.Duration;

/**
 * Custom Jackson module for handling Java time types, particularly Duration.
 * This module registers custom serializers and deserializers for Duration to ensure
 * proper handling of WebAuthn session data.
 */
public class CustomJavaTimeModule extends SimpleModule {

    public CustomJavaTimeModule() {
        super("CustomJavaTimeModule");
        addSerializer(Duration.class, new DurationSerializer());
        addDeserializer(Duration.class, new DurationDeserializer());
    }
}

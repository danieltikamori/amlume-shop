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
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;

import java.time.Duration;

/**
 * Custom module for handling Java 8 time types, particularly Duration.
 */
public class CustomJavaTimeModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public CustomJavaTimeModule() {
        super("CustomJavaTimeModule");

        // Register serializers and deserializers for Duration
        addSerializer(Duration.class, DurationSerializer.INSTANCE);
        addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.util;

import org.jspecify.annotations.NonNull;
import org.springframework.session.SessionIdGenerator;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Similar goal as UUID Type 7, but with improved security and enhanced usability.
 * Timestamp prefix ensures ordering, and bucketed as 1 hour intervals to prevent guessing precise times.
 * Randomness suffix is increased to 32-bytes, the minimum for NIST to consider it sufficiently unique to be secure.
 * Unsigned short counter extra suffix assisted debugging within each instance of an application (e.g. 0001, ..., FFFE, FFFF, 0000, ...).
 * Base64-URL encoding makes it useful as web session id cookie value, URL magic link query parameter, JWT jti/nonce, etc.
 * Binary data structure is...
 * Bytes (42):  timestamp bucket (8-bytes), random (32-bytes), rollover counter (2-bytes)
 * String encoding size is...
 * String (56): 42 bytes * 4 / 3 => 56 base64-url characters
 */
public class CustomSessionIdGenerator implements SessionIdGenerator {
    // Define segment lengths as constants
    private static final int TIMESTAMP_SEGMENT_BYTES = 8;
    private static final int RANDOM_SEGMENT_BYTES = 32; // NIST minimum randomness to be considered unique
    private static final int COUNTER_SEGMENT_BYTES = 2; // Represents a short
    private static final int TOTAL_ID_BYTES = TIMESTAMP_SEGMENT_BYTES + RANDOM_SEGMENT_BYTES + COUNTER_SEGMENT_BYTES;

    private static final long TIMESTAMP_GRANULARITY_SECONDS = Duration.ofHours(1).toSeconds();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final AtomicInteger COUNTER = new AtomicInteger(1); // Starts at 1 as in original
    private static final Base64.Encoder URL_SAFE_NO_PADDING_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public CustomSessionIdGenerator() {
        // Constructor for Spring bean instantiation
    }

    @Override
    @NonNull
    public String generate() {
        return URL_SAFE_NO_PADDING_ENCODER.encodeToString(generateBytes());
    }

    public byte[] generateBytes() {
        final long timestampBucket = Instant.now().getEpochSecond() / TIMESTAMP_GRANULARITY_SECONDS;
        // The AtomicInteger will cycle through int values. Casting to short takes the lower 16 bits.
        final short shortRolloverCounter = (short) COUNTER.getAndIncrement();

        final byte[] randomPart = new byte[RANDOM_SEGMENT_BYTES];
        SECURE_RANDOM.nextBytes(randomPart);

        ByteBuffer idBuffer = ByteBuffer.allocate(TOTAL_ID_BYTES);
        // ByteBuffer defaults to BIG_ENDIAN, so .order(ByteOrder.BIG_ENDIAN) is optional but can be kept for clarity.
        // idBuffer.order(ByteOrder.BIG_ENDIAN);

        idBuffer.putLong(timestampBucket);       // 8 bytes
        idBuffer.put(randomPart);                // 32 bytes
        idBuffer.putShort(shortRolloverCounter); // 2 bytes

        return idBuffer.array();
    }
}

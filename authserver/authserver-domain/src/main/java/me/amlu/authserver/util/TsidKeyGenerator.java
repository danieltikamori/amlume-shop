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

import io.hypersistence.tsid.TSID;

import java.time.Instant;

public class TsidKeyGenerator {

    // Discord Snowflakes have 5 bits for worker ID and 5 bits for process ID
    private static final int worker = 1;  // max: 2^5-1 = 31
    private static final int process = 1; // max: 2^5-1 = 31
    private static final int node = (worker << 5 | process); // max: 2^10-1 = 1023

    // Discord Epoch starts in the first millisecond of 2015
    private static final Instant customEpoch = Instant.parse("2025-01-01T00:00:00.000Z");

    public static String next() {
        // a factory that returns TSIDs similar to Discord Snowflakes

        return TSID.Factory.builder()
                .withCustomEpoch(customEpoch)
                .withNode(node)
                .build()
                .toString(); // Call toString() as build() returns a TSID object, not a String
    }
}

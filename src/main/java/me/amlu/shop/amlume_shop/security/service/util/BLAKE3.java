/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service.util;

import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.util.encoders.Hex;

public class BLAKE3 {

    private BLAKE3() {
        // This constructor is intentionally left blank to prevent instantiation
    }

    public static String hash(byte[] input) {
        Blake3Digest digest = new Blake3Digest();
        byte[] hash = new byte[digest.getDigestSize()];
        digest.update(input, 0, input.length);
        digest.doFinal(hash, 0);
        return Hex.toHexString(hash);
    }
}
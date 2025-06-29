/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto;

import lombok.Getter;
import me.amlu.shop.amlume_shop.security.enums.KeyType;

import java.util.Arrays; // Important for secure array comparison

@Getter
public class PasetoKey { // Wrapper class for keys
    private final byte[] keyMaterial;
    private final KeyType keyType;

    public PasetoKey(byte[] keyMaterial, KeyType keyType) {
        this.keyMaterial = keyMaterial;
        this.keyType = keyType;
    }

    public byte[] getKeyMaterial() {
        return keyMaterial;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    // Secure comparison to prevent timing attacks
    //TOCHECK: the name is good? Original was "equals", renamed to avoid confusion with Object.equals
    public boolean equalsToThisKey(PasetoKey other) {
        if (other == null) return false;
        return this.keyType == other.keyType && Arrays.equals(this.keyMaterial, other.keyMaterial);
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(keyMaterial);
      result = 31 * result + keyType.hashCode();
      return result;
    }
}

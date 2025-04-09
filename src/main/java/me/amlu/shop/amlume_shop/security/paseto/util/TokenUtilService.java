/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.paseto.util;

import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.user_management.User;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;

public interface TokenUtilService {

    @NotNull User getUserByUserId(String userId);

    String extractUserScope(Map<String, Object> claims);

    String generateTokenId();

    String extractUserId(Map<String, Object> claims) throws TokenValidationFailureException;

    String extractTokenId(Map<String, Object> claims) throws TokenValidationFailureException;

    Instant extractClaimInstant(Map<String, Object> claims, String claimName)
            throws TokenValidationFailureException;

    String extractSessionId(Map<String, Object> claims);

    Instant getFormattedInstant(Object claimInstant);
}

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

import me.amlu.shop.amlume_shop.exceptions.ClaimsSizeException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;

import java.security.SignatureException;
import java.util.Map;

public interface TokenValidationService {

    // It is at TokenRevocationService
//    void validateNotRevoked(Map<String, Object> claims) throws TokenRevokedException;

    boolean isAccessTokenValid(String token);

    Map<String, Object> validatePublicAccessToken(String token) throws TokenValidationFailureException, SignatureException;

    Map<String, Object> validateLocalAccessToken(String token) throws TokenValidationFailureException;

    Map<String, Object> validateLocalRefreshToken(String token) throws TokenValidationFailureException;

    void validateClaimsSize(Map<String, Object> claims) throws ClaimsSizeException;

    Map<String, Object> extractClaimsFromPublicAccessToken(String token);
}

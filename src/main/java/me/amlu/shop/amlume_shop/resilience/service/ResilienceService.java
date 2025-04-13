/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;

public interface ResilienceService {

    boolean allowRequestByIp(String ipAddress) throws RateLimitExceededException;

    boolean allowRequestByUsername(String userId) throws RateLimitExceededException;

//    boolean allowRequestByUserId(String userId) throws RateLimitExceededException;

    // Per instance of application
//    boolean allowRequestByIpPerInstance(String ipAddress) throws RateLimitExceededException;
//    boolean allowRequestByUserPerInstance(String username) throws RateLimitExceededException;

    boolean isRateLimitExceeded(String username);
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionManagementService {
    private final SessionRegistry sessionRegistry;

    public SessionManagementService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public List<SessionInformation> getUserSessions(String username) {
        return sessionRegistry.getAllSessions(username, false);
    }

    public void expireUserSessions(String username) {
        List<SessionInformation> sessions = getUserSessions(username);
        sessions.forEach(SessionInformation::expireNow);
    }

    public void expireOtherSessions(String sessionId) {
        sessionRegistry.getAllPrincipals().forEach(principal -> {
            sessionRegistry.getAllSessions(principal, false)
                    .stream()
                    .filter(session -> !session.getSessionId().equals(sessionId))
                    .forEach(SessionInformation::expireNow);
        });
    }
}

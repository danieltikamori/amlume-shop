/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.commons;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SLF4J logging markers used throughout the application.
 * These markers help categorize log messages for filtering and processing.
 */
public final class LoggingMarkers {
    // Security-related markers
    public static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");
    public static final Marker AUTHENTICATION = buildChildMarker("AUTHENTICATION", SECURITY);
    public static final Marker AUTHORIZATION = buildChildMarker("AUTHORIZATION", SECURITY);
    public static final Marker ACCESS_DENIED = buildChildMarker("ACCESS_DENIED", SECURITY);
    public static final Marker SUSPICIOUS = buildChildMarker("SUSPICIOUS", SECURITY);

    // Performance-related markers
    public static final Marker PERFORMANCE = MarkerFactory.getMarker("PERFORMANCE");
    public static final Marker SLOW_QUERY = buildChildMarker("SLOW_QUERY", PERFORMANCE);
    public static final Marker CACHE_MISS = buildChildMarker("CACHE_MISS", PERFORMANCE);
    public static final Marker HIGH_CPU = buildChildMarker("HIGH_CPU", PERFORMANCE);
    public static final Marker HIGH_MEMORY = buildChildMarker("HIGH_MEMORY", PERFORMANCE);

    // Audit-related markers
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    public static final Marker USER_ACTIVITY = buildChildMarker("USER_ACTIVITY", AUDIT);
    public static final Marker DATA_CHANGE = buildChildMarker("DATA_CHANGE", AUDIT);
    public static final Marker ADMIN_ACTION = buildChildMarker("ADMIN_ACTION", AUDIT);

    // Error-related markers
    public static final Marker ERROR = MarkerFactory.getMarker("ERROR");
    public static final Marker DATABASE_ERROR = buildChildMarker("DATABASE_ERROR", ERROR);
    public static final Marker NETWORK_ERROR = buildChildMarker("NETWORK_ERROR", ERROR);
    public static final Marker VALIDATION_ERROR = buildChildMarker("VALIDATION_ERROR", ERROR);
    public static final Marker INTEGRATION_ERROR = buildChildMarker("INTEGRATION_ERROR", ERROR);

    // External service markers
    public static final Marker EXTERNAL = MarkerFactory.getMarker("EXTERNAL");
    public static final Marker API_CALL = buildChildMarker("API_CALL", EXTERNAL);
    public static final Marker OAUTH2 = buildChildMarker("OAUTH2", EXTERNAL);
    public static final Marker WEBAUTHN = buildChildMarker("WEBAUTHN", EXTERNAL);

    private LoggingMarkers() {
    } // Private constructor to prevent instantiation

    /**
     * Helper method to build a child marker with a parent.
     *
     * @param name   The name of the child marker
     * @param parent The parent marker
     * @return The created child marker
     */
    private static Marker buildChildMarker(String name, Marker parent) {
        Marker child = MarkerFactory.getMarker(name);
        child.add(parent);
        return child;
    }
}

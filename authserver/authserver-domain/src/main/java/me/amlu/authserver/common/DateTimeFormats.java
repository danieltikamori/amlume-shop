/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.common;

import java.time.format.DateTimeFormatter;

/**
 * Date and time format constants used throughout the application.
 */
public final class DateTimeFormats {
    // ISO formats
    public static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    public static final String ISO_TIME_PATTERN = "HH:mm:ss";
    public static final String ISO_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String ISO_DATE_TIME_ZONE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    // Predefined formatters
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    public static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_TIME;
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    // Custom formatters
    public static final DateTimeFormatter SIMPLE_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter SIMPLE_TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter SIMPLE_DATE_TIME = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    public static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm:ss");

    // Log formatters
    public static final DateTimeFormatter LOG_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // File name formatters
    public static final DateTimeFormatter FILE_NAME_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateTimeFormats() {
    } // Private constructor to prevent instantiation
}

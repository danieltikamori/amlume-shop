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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for exception handling.
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    } // Private constructor to prevent instantiation

    /**
     * Gets the root cause of an exception.
     *
     * @param throwable The throwable
     * @return The root cause
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }

        return getRootCause(cause);
    }

    /**
     * Gets the stack trace as a string.
     *
     * @param throwable The throwable
     * @return The stack trace string
     */
    public static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Gets a simplified stack trace with only application packages.
     *
     * @param throwable   The throwable
     * @param basePackage The base package to include (e.g., "me.amlu")
     * @return The simplified stack trace
     */
    public static String getSimplifiedStackTrace(Throwable throwable, String basePackage) {
        if (throwable == null) {
            return "";
        }

        List<StackTraceElement> relevantTrace = Arrays.stream(throwable.getStackTrace())
                .filter(element -> element.getClassName().startsWith(basePackage))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : relevantTrace) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Checks if an exception or any of its causes is of a specific type.
     *
     * @param throwable      The throwable
     * @param exceptionClass The exception class to check
     * @return true if the throwable or any cause is of the specified type
     */
    public static boolean hasCauseOfType(Throwable throwable, Class<? extends Throwable> exceptionClass) {
        if (throwable == null || exceptionClass == null) {
            return false;
        }

        if (exceptionClass.isInstance(throwable)) {
            return true;
        }

        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return false;
        }

        return hasCauseOfType(cause, exceptionClass);
    }

    /**
     * Gets the first cause of a specific type.
     *
     * @param <T>            The exception type
     * @param throwable      The throwable
     * @param exceptionClass The exception class to find
     * @return The first cause of the specified type, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T getCauseOfType(Throwable throwable, Class<T> exceptionClass) {
        if (throwable == null || exceptionClass == null) {
            return null;
        }

        if (exceptionClass.isInstance(throwable)) {
            return (T) throwable;
        }

        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return null;
        }

        return getCauseOfType(cause, exceptionClass);
    }
}

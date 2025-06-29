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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * File type constants and validation utilities.
 */
public final class FileTypes {
    // Image file extensions
    public static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    );

    // Document file extensions
    public static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt")
    );

    // Maximum file sizes in bytes
    public static final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB

    private FileTypes() {
    } // Private constructor to prevent instantiation

    /**
     * Checks if a file extension is an allowed image type.
     *
     * @param extension The file extension (without the dot)
     * @return true if the extension is an allowed image type
     */
    public static boolean isAllowedImageExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Checks if a file extension is an allowed document type.
     *
     * @param extension The file extension (without the dot)
     * @return true if the extension is an allowed document type
     */
    public static boolean isAllowedDocumentExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return DOCUMENT_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param filename The filename
     * @return The extension (without the dot) or empty string if no extension
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1).toLowerCase();
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.file.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Controller for handling file download requests.
 */
@RestController
@RequestMapping("/api/files") // Base path for file-related operations
public class FileDownloadController {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadController.class);

    private final FileService fileService;

    // Inject the base path used for uploads/downloads from application properties
//    @Value("${project.image.path}") // Make sure this property matches your configuration
    @Value("${project.file.path}") // Make sure this property matches your configuration
    private String fileStoragePath;

    public FileDownloadController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Handles GET requests to download a file by its name.
     *
     * @param filename The name of the file to download (including extension).
     * @param request  The HttpServletRequest to help determine content type.
     * @return ResponseEntity containing the file Resource or an error status.
     */
    @GetMapping("/download/{filename:.+}") // Use .+ to capture filenames with dots correctly
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
        log.debug("Request received to download file: {}", filename);

        Resource resource;
        try {
            // Load the file using the configured path and provided filename
            resource = fileService.downloadFile(fileStoragePath, filename);
            log.debug("File resource loaded successfully for: {}", filename);

        } catch (ResourceNotFoundException e) {
            log.warn("Download failed: File not found - {}", filename, e);
            // Return 404 Not Found if the file doesn't exist
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            log.error("Download failed: Malformed URL for file - {}", filename, e);
            // Return 500 Internal Server Error for URL issues (likely config problem)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Or a custom error body
        } catch (IllegalArgumentException e) {
            log.warn("Download failed: Invalid argument (path or filename) - {}", filename, e);
            // Return 400 Bad Request for invalid input
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Catch any other unexpected errors during file loading
            log.error("Download failed: Unexpected error loading file - {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Try to determine the file's content type
        String contentType = null;
        try {
            // Use ServletContext to guess MIME type based on file extension
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            log.debug("Determined content type for {}: {}", filename, contentType);
        } catch (IOException ex) {
            // Log error but continue with a default content type
            log.warn("Could not determine content type for file {}: {}", filename, ex.getMessage());
        }

        // Fallback to generic binary type if MIME type couldn't be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
            log.debug("Using default content type for {}: {}", filename, contentType);
        }

        // Build the ResponseEntity
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // Set Content-Disposition header to prompt download with the original filename
                // Use resource.getFilename() which should be the filename passed in
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                // Optional: Add Content-Length header if known (can help browsers show progress)
                // .contentLength(resource.contentLength()) // Be cautious: this can throw IOException
                .body(resource);
    }
}

/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.payload.FileUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    public FileUploadResult uploadImage(String path, MultipartFile imageFile) throws IOException {
        // --- Input Validation ---
        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("Attempted to upload an empty or null image file.");
            throw new IllegalArgumentException("Image file cannot be null or empty.");
        }

        String originalFilename = imageFile.getOriginalFilename(); // Keep the original name
        if (!StringUtils.hasText(originalFilename)) {
            log.warn("Attempted to upload a file with no original filename.");
            throw new IllegalArgumentException("Image file must have an original filename.");
        }

        // --- Generate Unique Filename ---
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        } else {
            log.warn("File '{}' has no extension. Upload might proceed without one.", originalFilename);
        }

        // Use UUID for better uniqueness
        String uniqueFileNameBase = UUID.randomUUID().toString();
        String generatedFileName = uniqueFileNameBase + fileExtension; // This is the name for storage
        Path targetDirectory = Paths.get(path);
        Path targetFilePath = targetDirectory.resolve(generatedFileName); // Use resolve for path construction

        log.debug("Attempting to upload file '{}' as '{}' to path '{}'", originalFilename, generatedFileName, targetFilePath);

        // --- Ensure Directory Exists ---
        if (!Files.exists(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
                log.info("Created directory: {}", targetDirectory);
            } catch (IOException e) {
                log.error("Failed to create directory: {}", targetDirectory, e);
                throw new IOException("Could not create directory for upload: " + targetDirectory, e);
            }
        }

        // --- Upload File ---
        try {
            Files.copy(imageFile.getInputStream(), targetFilePath);
            log.info("Successfully uploaded file '{}' to '{}'", generatedFileName, targetFilePath);
            // Return both names
            return new FileUploadResult(generatedFileName, originalFilename); // Return the generated unique filename
        } catch (IOException e) {
            log.error("Failed to upload file '{}' to '{}'", generatedFileName, targetFilePath, e);
            // Delete the partially uploaded file if possible/necessary
            try {
                Files.deleteIfExists(targetFilePath);
            } catch (IOException delEx) {
                log.warn("Failed to delete partial file after upload error: {}", delEx.getMessage());
            }
            throw new IOException("Failed to save uploaded file: " + generatedFileName, e);
        }
    }

    @Override
    public void deleteImage(String path, String imageNameToDelete) throws IOException {
        if (!StringUtils.hasText(imageNameToDelete) || imageNameToDelete.equals("default.png")) {
            log.warn("Attempted to delete an invalid or default image name: '{}'", imageNameToDelete);
            return; // Don't attempt to delete null, empty, or default names
        }

        Path targetDirectory = Paths.get(path);
        Path targetFilePath = targetDirectory.resolve(imageNameToDelete);

        log.debug("Attempting to delete file: {}", targetFilePath);

        try {
            if (Files.exists(targetFilePath) && !Files.isDirectory(targetFilePath)) {
                boolean deleted = Files.deleteIfExists(targetFilePath);
                if (deleted) {
                    log.info("Successfully deleted file: {}", targetFilePath);
                } else {
                    // This case might happen due to race conditions or permissions
                    log.warn("File deletion reported false, but file might already be gone or inaccessible: {}", targetFilePath);
                }
            } else {
                log.warn("File not found or is a directory, cannot delete: {}", targetFilePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", targetFilePath, e);
            throw new IOException("Could not delete file: " + imageNameToDelete, e);
        }
    }

    // --- Download file Method Implementation ---
    @Override
    public Resource downloadFile(String path, String fileName) throws ResourceNotFoundException, MalformedURLException {
        // --- Input Validation ---
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("Filename cannot be empty.");
        }

        try {
            Path fileDirectory = Paths.get(path);
            Path filePath = fileDirectory.resolve(fileName).normalize(); // Normalize a path

            log.debug("Attempting to load file for download: {}", filePath);

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("File found and readable: {}", filePath);
                return resource;
            } else {
                log.error("File not found or not readable: {}", filePath);
                // Throw specific exception if a file doesn't exist or isn't readable
                throw new ResourceNotFoundException("File", "fileName", fileName);
            }
        } catch (MalformedURLException e) {
            // This indicates an issue converting the file path to a URI, likely an internal error
            log.error("Error creating URL resource for path: {}/{}", path, fileName, e);
            // Re-throw as MalformedURLException as declared in the interface,
            // or wrap in a runtime exception if preferred (e.g., APIException)
            throw new MalformedURLException("Could not create URL resource for file: " + fileName);
            // Alternative: throw new APIException("Internal error creating file resource URL", e);
        } catch (Exception e) {
            // Catch unexpected errors during path resolution or resource access
            log.error("Unexpected error downloading file {} from path {}: {}", fileName, path, e.getMessage(), e);
            throw new ResourceNotFoundException("File", "fileName", fileName);
        }
    }
}
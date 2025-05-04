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
import me.amlu.shop.amlume_shop.file.dto.GetFileUploadResultResponse;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * When dealing file files, check these best practices:
 * <p>
 * 1.Input Validation: Checks for null/empty files, file size, filename length, and path length.
 * 2.Path Traversal Prevention: Uses toAbsolutePath().normalize() on the path.
 * 3.Unique Filenames: Generates unique filenames using hashing and UUIDs, preventing collisions and making direct access harder.
 * 4.Error Handling: Attempts to delete partial files on upload failure.
 * 5.Validate the actual content and type of the uploaded file:
 * 1.Validate MIME Type: Check the Content-Type provided by the client, although this can be spoofed.
 * 2.Validate File Signature (Magic Bytes): Read the beginning of the file stream to identify the actual file type based on its binary signature. This is much more reliable than the filename or MIME type.
 * We'll use Apache Tika for this.
 * 3.Enforce Allowed Types: Maintain an explicit allowlist of MIME types.
 * 4.Ensure Path Confinement: Double-check that the resolved upload path doesn't escape the intended base directory.
 */

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_PATH_LENGTH = 512; // Max length for the *relative* path parameter

    // Define allowed MIME types for images
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
            // Add other image types if needed
    );

    // Inject the base path from configuration
    @Value("${project.image.path}")
    private String baseUploadPath;

    private final Tika tika = new Tika(); // Tika instance for content type detection

    // Input: User-controlled 'relativePath'
    // Sink: Path construction and file system operations (createDirectories, copy)
    private boolean isValidPath(String path) {
        // Allow only alphanumeric characters and specific symbols
        // This regex prevents '..' and '/' or '\' which are key for traversal.
        String regex = "^[a-zA-Z0-9._-]+$";
        return path.matches(regex);
    }

    @Override
    public GetFileUploadResultResponse uploadImage(String relativePath, MultipartFile imageFile) throws IOException {
        // --- Input Validation ---
        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("Attempted to upload an empty or null image file.");
            throw new IllegalArgumentException("Image file cannot be null or empty.");
        }

        // --- File Size Validation ---
        if (imageFile.getSize() > MAX_FILE_SIZE_BYTES) {
            log.warn("Attempted to upload a file exceeding the maximum allowed size ({} bytes). File size: {}", MAX_FILE_SIZE_BYTES, imageFile.getSize());
            throw new IllegalArgumentException("File size exceeds the maximum allowed size of " + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + "MB.");
        }

        // Input: User-controlled original filename
        String originalFilename = imageFile.getOriginalFilename(); // Keep the original name
        if (!StringUtils.hasText(originalFilename)) {
            log.warn("Attempted to upload a file with no original filename.");
            throw new IllegalArgumentException("Image file must have an original filename.");
        }
        // Sanitize original filename (removes '..' and normalizes separators)
        originalFilename = StringUtils.cleanPath(originalFilename); // Basic sanitization

        // --- Filename Length Validation ---
        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            log.warn("Attempted to upload a file with a filename exceeding the maximum allowed length ({} chars). Filename: '{}'", MAX_FILENAME_LENGTH, originalFilename);
            throw new IllegalArgumentException("Filename exceeds the maximum allowed length of " + MAX_FILENAME_LENGTH + " characters.");
        }

        // --- Relative Path Validation ---
        // Input: User-controlled 'relativePath'
        // Validation: isValidPath uses a regex that prevents traversal characters.
        if (!isValidPath(relativePath)) {
            log.warn("Attempted to upload a file with an invalid relative path: '{}'", relativePath);
            throw new IllegalArgumentException("Invalid relative path specified.");
        }
        // Redundant check given the regex, but kept for clarity/defense-in-depth.
        if (relativePath.contains("..")) {
            log.error("Potential path traversal attempt detected in relative path: {}", relativePath);
            throw new IllegalArgumentException("Invalid relative path specified.");
        }
        if (relativePath.length() > MAX_PATH_LENGTH) {
            log.warn("Attempted to upload a file with a relative path exceeding the maximum allowed length ({} chars). Path: '{}'", MAX_PATH_LENGTH, relativePath);
            throw new IllegalArgumentException("Relative path exceeds the maximum allowed length of " + MAX_PATH_LENGTH + " characters.");
        }


        // --- File Content Type Validation ---
        String detectedMimeType;
        try (InputStream inputStream = imageFile.getInputStream()) {
            // Use Tika to detect the MIME type from the actual content
            detectedMimeType = tika.detect(inputStream);
        } catch (IOException e) {
            log.error("Error reading input stream for MIME type detection for file '{}'", originalFilename, e);
            throw new IOException("Could not read file content for validation.", e);
        }

        if (!ALLOWED_IMAGE_TYPES.contains(detectedMimeType)) {
            log.warn("Disallowed file type detected: '{}' for file '{}'. Allowed types: {}", detectedMimeType, originalFilename, ALLOWED_IMAGE_TYPES);
            throw new IllegalArgumentException("Invalid file type. Only " + ALLOWED_IMAGE_TYPES + " are allowed.");
        }
        log.debug("Detected MIME type '{}' for file '{}' is allowed.", detectedMimeType, originalFilename);

        // --- Generate Unique Filename ---
        // This part uses the sanitized original filename and detected type, but primarily relies on UUID/hash,
        // making direct traversal injection via filename unlikely here.
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            // Use a safe extension based on the *detected* MIME type if possible, fallback to original
            fileExtension = mapMimeTypeToExtension(detectedMimeType, originalFilename.substring(lastDotIndex));
        } else {
            log.warn("File '{}' has no extension. Upload might proceed without one, using default based on MIME.", originalFilename);
            fileExtension = mapMimeTypeToExtension(detectedMimeType, ""); // Try to get extension from MIME
        }

        // Generate a secure unique filename using a hash of the original filename and a UUID
        String uniqueFileNameBase = DigestUtils.md5DigestAsHex((originalFilename + UUID.randomUUID().toString()).getBytes());
        String generatedFileName = uniqueFileNameBase + fileExtension;

        // --- Path Construction and Validation ---
        // Sink: Paths.get uses external input 'baseUploadPath' (config, assumed safe)
        Path baseDir = Paths.get(baseUploadPath).toAbsolutePath().normalize();
        // Sink: baseDir.resolve uses validated 'relativePath'
        Path targetDirectory = baseDir.resolve(relativePath).normalize(); // Combine base and relative path

        // **Crucial Security Check:** Ensure the target directory is *within* the base directory
        // This prevents traversal via 'relativePath'.
        if (!targetDirectory.startsWith(baseDir)) {
            log.error("Path confinement violation: Resolved path '{}' is outside the allowed base directory '{}'", targetDirectory, baseDir);
            throw new SecurityException("Invalid path specified: Attempt to escape base directory.");
        }

        // Sink: targetDirectory.resolve uses generated filename (less likely vulnerable)
        Path targetFilePath = targetDirectory.resolve(generatedFileName); // Use resolve for path construction

        // **FIX:** Added final path confinement check for defense-in-depth.
        // **ANALYSIS:** This check ensures that even if the generatedFileName somehow contained traversal elements
        // (unlikely due to hashing/UUID), the final path is still confined within the base directory.
        if (!targetFilePath.startsWith(baseDir)) {
            log.error("Path confinement violation (final path): Resolved path '{}' is outside the allowed base directory '{}'", targetFilePath, baseDir);
            throw new SecurityException("Invalid file path specified: Attempt to escape base directory.");
        }


        log.debug("Attempting to upload file '{}' as '{}' to final path '{}'", originalFilename, generatedFileName, targetFilePath);

        // --- Ensure Directory Exists ---
        // Sink: Files.createDirectories uses 'targetDirectory' which was validated.
        if (!Files.exists(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
                log.info("Created directory: {}", targetDirectory);
            } catch (IOException e) {
                log.error("Failed to create directory: {}", targetDirectory, e);
                throw new IOException("Could not create directory for upload: " + targetDirectory, e);
            } catch (SecurityException se) {
                log.error("Security error creating directory: {}", targetDirectory, se);
                throw new IOException("Security error creating directory: " + targetDirectory, se);
            }
        }

        // --- Upload File ---
        // Sink: Files.copy uses 'targetFilePath' which was validated.
        // ANALYSIS: The targetFilePath variable is constructed from baseUploadPath (configuration),
        // relativePath (validated against traversal), and generatedFileName (hashed/UUID).
        // Crucially, two path confinement checks (`targetDirectory.startsWith(baseDir)` and
        // `targetFilePath.startsWith(baseDir)`) are performed *after* normalization.
        // These checks effectively prevent writing outside the intended baseUploadPath directory.
        // Therefore, the input reaching Files.copy is considered safe from path traversal originating
        // from user-controlled `relativePath` or `originalFilename`.
        try (InputStream inputStream = imageFile.getInputStream()) { // Get a new stream for copying
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully uploaded file '{}' to '{}'", generatedFileName, targetFilePath);
            // Return both names
            return new GetFileUploadResultResponse(generatedFileName, originalFilename); // Return the generated unique filename
        } catch (IOException e) {
            log.error("Failed to save uploaded file '{}' to '{}'", generatedFileName, targetFilePath, e);
            // Delete the partially uploaded file if possible/necessary
            try {
                Files.deleteIfExists(targetFilePath);
            } catch (IOException delEx) {
                log.warn("Failed to delete partial file '{}' after upload error: {}", targetFilePath, delEx.getMessage());
            }
            throw new IOException("Failed to save uploaded file: " + generatedFileName, e);
        } catch (SecurityException se) {
            log.error("Security error saving file '{}' to '{}'", generatedFileName, targetFilePath, se);
            throw new IOException("Security error saving file: " + generatedFileName, se);
        }
    }

    // Helper to map detected MIME type to a safe extension
    private String mapMimeTypeToExtension(String mimeType, String originalExtension) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            // Add more mappings as needed
            default -> {
                // Fallback to original extension if it's simple, otherwise use a default or reject
                if (originalExtension.matches("\\.[a-zA-Z0-9]+")) {
                    log.warn("Using original extension '{}' for unrecognized but allowed MIME type '{}'", originalExtension, mimeType);
                    yield originalExtension;
                } else {
                    log.error("Cannot determine safe extension for detected MIME type '{}' and invalid original extension '{}'", mimeType, originalExtension);
                    // You might want to throw an exception here instead of using .bin
                    // throw new IllegalArgumentException("Cannot determine safe file extension");
                    yield ".bin"; // Or reject the upload
                }
            }
        };
    }


    @Override
    public void deleteImage(String relativePath, String imageNameToDelete) throws IOException {
        // Input: User-controlled 'imageNameToDelete'
        if (!StringUtils.hasText(imageNameToDelete) || imageNameToDelete.equals("default.png")) {
            log.warn("Attempted to delete an invalid or default image name: '{}'", imageNameToDelete);
            return; // Don't attempt to delete null, empty, or default names
        }

        // --- Filename Length Validation ---
        if (imageNameToDelete.length() > MAX_FILENAME_LENGTH) {
            log.warn("Attempted to delete a file with a filename exceeding the maximum allowed length.");
            throw new IllegalArgumentException("Filename exceeds the maximum allowed length of " + MAX_FILENAME_LENGTH + " characters.");
        }

        // --- Relative Path Validation ---
        // Input: User-controlled 'relativePath'
        if (!StringUtils.hasText(relativePath)) {
            log.warn("Attempted to delete a file with an empty or null relative path.");
            throw new IllegalArgumentException("Relative path cannot be null or empty.");
        }
        if (relativePath.length() > MAX_PATH_LENGTH) {
            log.warn("Attempted to delete a file with a relative path exceeding the maximum allowed length.");
            throw new IllegalArgumentException("Relative path exceeds the maximum allowed length of " + MAX_PATH_LENGTH + " characters.");
        }
        // Validation: Explicit '..' check prevents traversal.
        if (relativePath.contains("..")) {
            log.error("Potential path traversal attempt detected in relative path for deletion: {}", relativePath);
            throw new IllegalArgumentException("Invalid relative path specified for deletion.");
        }

        // --- Path Construction and Validation ---
        // Sink: Paths.get uses external input 'baseUploadPath' (config, assumed safe)
        Path baseDir = Paths.get(baseUploadPath).toAbsolutePath().normalize();
        // Sink: baseDir.resolve uses validated 'relativePath'
        Path targetDirectory = baseDir.resolve(relativePath).normalize();

        // **Crucial Security Check:** Ensure the target directory is *within* the base directory
        // This prevents traversal via 'relativePath'.
        if (!targetDirectory.startsWith(baseDir)) {
            log.error("Path confinement violation during delete: Resolved path '{}' is outside the allowed base directory '{}'", targetDirectory, baseDir);
            throw new SecurityException("Invalid path specified for deletion: Attempt to escape base directory.");
        }

        // Sink: targetDirectory.resolve uses 'imageNameToDelete' (user input)
        // Validation: Normalization happens here.
        Path targetFilePath = targetDirectory.resolve(imageNameToDelete).normalize(); // Normalize final path too

        // **Crucial Security Check:** Ensure the final file path is still within the base directory
        // This prevents traversal via 'imageNameToDelete' combined with 'relativePath'.
        if (!targetFilePath.startsWith(baseDir)) {
            log.error("Path confinement violation during delete (final path): Resolved path '{}' is outside the allowed base directory '{}'", targetFilePath, baseDir);
            throw new SecurityException("Invalid file path specified for deletion: Attempt to escape base directory.");
        }


        log.debug("Attempting to delete file: {}", targetFilePath);

        // Sink: Files.deleteIfExists uses 'targetFilePath' which was validated.
        try {
            // Check existence *after* path validation
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
                // Consider throwing ResourceNotFoundException if the file *should* exist
                // throw new ResourceNotFoundException("File", "fileName", imageNameToDelete);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", targetFilePath, e);
            throw new IOException("Could not delete file: " + imageNameToDelete, e);
        } catch (SecurityException se) {
            log.error("Security error deleting file: {}", targetFilePath, se);
            throw new IOException("Security error deleting file: " + imageNameToDelete, se);
        }
    }

    // --- Download file Method Implementation ---
    @Override
    public Resource downloadFile(String relativePath, String fileName) throws ResourceNotFoundException, MalformedURLException {
        // --- Input Validation ---
        // Input: User-controlled 'fileName'
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("Filename cannot be empty.");
        }
        // Sanitize filename (removes '..' and normalizes separators)
        fileName = StringUtils.cleanPath(fileName);
        // Validation: Explicit '..' check after cleaning.
        if (fileName.contains("..")) { // Double check after cleaning
            log.error("Potential path traversal attempt detected in filename for download: {}", fileName);
            throw new IllegalArgumentException("Invalid filename specified for download.");
        }


        // --- Filename Length Validation ---
        if (fileName.length() > MAX_FILENAME_LENGTH) {
            log.warn("Attempted to download a file with a filename exceeding the maximum allowed length.");
            throw new IllegalArgumentException("Filename exceeds the maximum allowed length of " + MAX_FILENAME_LENGTH + " characters.");
        }

        // --- Relative Path Validation ---
        // Input: User-controlled 'relativePath'
        if (!StringUtils.hasText(relativePath)) {
            log.warn("Attempted to download a file with an empty or null relative path.");
            throw new IllegalArgumentException("Relative path cannot be null or empty.");
        }
        if (relativePath.length() > MAX_PATH_LENGTH) {
            log.warn("Attempted to download a file with a relative path exceeding the maximum allowed length.");
            throw new IllegalArgumentException("Relative path exceeds the maximum allowed length of " + MAX_PATH_LENGTH + " characters.");
        }
        // Validation: Explicit '..' check prevents traversal.
        if (relativePath.contains("..")) {
            log.error("Potential path traversal attempt detected in relative path for download: {}", relativePath);
            throw new IllegalArgumentException("Invalid relative path specified for download.");
        }

        try {
            // --- Path Construction and Validation ---
            // Sink: Paths.get uses external input 'baseUploadPath' (config, assumed safe)
            Path baseDir = Paths.get(baseUploadPath).toAbsolutePath().normalize();
            // Sink: baseDir.resolve uses validated 'relativePath'
            Path targetDirectory = baseDir.resolve(relativePath).normalize();

            // **Crucial Security Check:** Ensure the target directory is *within* the base directory
            // This prevents traversal via 'relativePath'.
            if (!targetDirectory.startsWith(baseDir)) {
                log.error("Path confinement violation during download: Resolved path '{}' is outside the allowed base directory '{}'", targetDirectory, baseDir);
                throw new SecurityException("Invalid path specified for download: Attempt to escape base directory.");
            }

            // Sink: targetDirectory.resolve uses sanitized 'fileName'
            // Validation: Normalization happens here.
            Path filePath = targetDirectory.resolve(fileName).normalize(); // Normalize final path

            // **Crucial Security Check:** Ensure the final file path is still within the base directory
            // This prevents traversal via 'fileName' combined with 'relativePath'.
            if (!filePath.startsWith(baseDir)) {
                log.error("Path confinement violation during download (final path): Resolved path '{}' is outside the allowed base directory '{}'", filePath, baseDir);
                throw new SecurityException("Invalid file path specified for download: Attempt to escape base directory.");
            }

            log.debug("Attempting to load file for download: {}", filePath);

            // Sink: new UrlResource uses 'filePath' which was validated.
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
            log.error("Error creating URL resource for path: {}/{} and file: {}", baseUploadPath, relativePath, fileName, e);
            // Re-throw as MalformedURLException as declared in the interface,
            // or wrap in a runtime exception if preferred (e.g., APIException)
            throw new MalformedURLException("Could not create URL resource for file: " + fileName);
            // Alternative: throw new APIException("Internal error creating file resource URL", e);
        } catch (SecurityException se) {
            log.error("Security error accessing file for download: {}/{}/{}", baseUploadPath, relativePath, fileName, se);
            // Map security exception to ResourceNotFound to avoid leaking information
            throw new ResourceNotFoundException("File", "fileName", fileName);
        } catch (Exception e) {
            // Catch unexpected errors during path resolution or resource access
            log.error("Unexpected error downloading file {} from path {}/{}: {}", fileName, baseUploadPath, relativePath, e.getMessage(), e);
            throw new ResourceNotFoundException("File", "fileName", fileName);
        }
    }
}
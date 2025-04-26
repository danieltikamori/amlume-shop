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
import me.amlu.shop.amlume_shop.payload.GetFileUploadResultResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Service interface for handling file operations, specifically image uploads, deletions, and downloads.
 */
public interface FileService {

    /**
     * Uploads an image file to the specified path, generating a unique filename.
     *
     * @param path      The directory path where the image should be stored.
     * @param imageFile The image file received from the client.
     * @return A FileUploadResult containing both the generated (stored) filename and the original filename.
     * @throws IOException              if an I/O error occurs during file saving or directory creation.
     * @throws IllegalArgumentException if the imageFile is null, empty, or has no original filename.
     */
    GetFileUploadResultResponse uploadImage(String path, MultipartFile imageFile) throws IOException;

    /**
     * Deletes an image file from the specified path.
     *
     * @param path         The directory path where the image is stored.
     * @param oldImageName The filename of the image to delete.
     * @throws IOException if an I/O error occurs during file deletion.
     */
    void deleteImage(String path, String oldImageName) throws IOException;

    /**
     * Loads a file as a Spring Resource for downloading.
     *
     * @param path     The directory path where the file is stored.
     * @param fileName The unique filename of the file to download.
     * @return A Resource object representing the file.
     * @throws ResourceNotFoundException if the file does not exist or is not readable.
     * @throws MalformedURLException     if the file path cannot be converted to a valid URL (internal error).
     * @throws IllegalArgumentException  if a path or fileName is invalid.
     */
    Resource downloadFile(String path, String fileName) throws ResourceNotFoundException, MalformedURLException;

}
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

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service interface for handling file operations, specifically image uploads and deletions.
 */
public interface FileService {

    /**
     * Uploads an image file to the specified path, generating a unique filename.
     *
     * @param path      The directory path where the image should be stored.
     * @param imageFile The image file received from the client.
     * @return The unique filename under which the image was stored.
     * @throws IOException              if an I/O error occurs during file saving or directory creation.
     * @throws IllegalArgumentException if the imageFile is null, empty, or has no original filename.
     */
    String uploadImage(String path, MultipartFile imageFile) throws IOException;

    /**
     * Deletes an image file from the specified path.
     *
     * @param path         The directory path where the image is stored.
     * @param oldImageName The filename of the image to delete.
     * @throws IOException if an I/O error occurs during file deletion.
     */
    void deleteImage(String path, String oldImageName) throws IOException;
}
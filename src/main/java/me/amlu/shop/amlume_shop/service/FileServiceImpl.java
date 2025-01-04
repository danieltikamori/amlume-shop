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

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile imageFile) throws IOException {
        // Get the file name of the new image / original file
        String originalFilename = imageFile.getOriginalFilename();

        // Rename the file name to avoid duplicates
        // Generate a unique file name
        String uniqueFileName = System.currentTimeMillis() + "_" + originalFilename;
        assert originalFilename != null;
        String fileName = uniqueFileName.concat(originalFilename.substring(originalFilename.lastIndexOf(".")));
        String filePath = path + File.separator + fileName;

        // Check if path exists, if not create it
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Upload the image to server
        Files.copy(imageFile.getInputStream(), Paths.get(filePath));

        // Check if a new image was uploaded
        if (imageFile.isEmpty()) {
            throw new IOException("Failed to upload image. Try again");
        } else {
            System.out.println("Image uploaded successfully");

            // Return the file name
            return fileName;
        }
    }
}

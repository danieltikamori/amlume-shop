    /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */    // src/main/java/me/amlu/authserver/security/util/EncryptedStringConverter.java
    package me.amlu.authserver.security.util;

    import jakarta.persistence.AttributeConverter;
    import jakarta.persistence.Converter;
    import me.amlu.authserver.exceptions.EncryptionException;
    import me.amlu.authserver.security.service.EncryptionService;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Component;

    import java.nio.charset.StandardCharsets;

    @Converter
    @Component
    public class EncryptedStringConverter implements AttributeConverter<String, byte[]> {

        private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);
        private static EncryptionService staticEncryptionService;

        @Autowired
        public void setEncryptionService(EncryptionService encryptionService) {
            if (EncryptedStringConverter.staticEncryptionService == null) {
                EncryptedStringConverter.staticEncryptionService = encryptionService;
                log.info("EncryptionService injected into EncryptedStringConverter.");
            }
        }

        @Override
        public byte[] convertToDatabaseColumn(String attribute) {
            if (attribute == null) {
                return null;
            }
            if (staticEncryptionService == null) {
                log.error("EncryptionService not available in EncryptedStringConverter for encryption.");
                throw new IllegalStateException("EncryptionService not available.");
            }
            try {
                byte[] stringBytes = attribute.getBytes(StandardCharsets.UTF_8);
                return staticEncryptionService.encrypt(stringBytes);
            } catch (EncryptionException e) {
                log.error("Failed to encrypt String: {}", attribute, e);
                throw e;
            }
        }

        @Override
        public String convertToEntityAttribute(byte[] dbData) {
            if (dbData == null) {
                return null;
            }
            if (staticEncryptionService == null) {
                log.error("EncryptionService not available in EncryptedStringConverter for decryption.");
                throw new IllegalStateException("EncryptionService not available.");
            }
            try {
                byte[] decryptedBytes = staticEncryptionService.decrypt(dbData);
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            } catch (EncryptionException e) {
                log.error("Failed to decrypt String from database data.", e);
                throw e;
            }
        }
    }

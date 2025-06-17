/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// src/main/java/me/amlu/authserver/security/util/EncryptedPhoneNumberConverter.java
package me.amlu.authserver.security.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.amlu.authserver.exceptions.EncryptionException;
import me.amlu.authserver.security.service.EncryptionService;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Converter
@Component
public class EncryptedPhoneNumberConverter implements AttributeConverter<PhoneNumber, byte[]> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedPhoneNumberConverter.class);
    private static EncryptionService staticEncryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        if (EncryptedPhoneNumberConverter.staticEncryptionService == null) {
            EncryptedPhoneNumberConverter.staticEncryptionService = encryptionService;
            log.info("EncryptionService injected into EncryptedPhoneNumberConverter.");
        }
    }

    @Override
    public byte[] convertToDatabaseColumn(PhoneNumber attribute) {
        if (attribute == null || !StringUtils.hasText(attribute.e164Value())) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not available in EncryptedPhoneNumberConverter for encryption.");
            throw new IllegalStateException("EncryptionService not available.");
        }
        try {
            byte[] phoneBytes = attribute.e164Value().getBytes(StandardCharsets.UTF_8);
            return staticEncryptionService.encrypt(phoneBytes);
        } catch (EncryptionException e) {
            log.error("Failed to encrypt PhoneNumber: {}", attribute.e164Value(), e);
            throw e;
        }
    }

    @Override
    public PhoneNumber convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not available in EncryptedPhoneNumberConverter for decryption.");
            throw new IllegalStateException("EncryptionService not available.");
        }
        try {
            byte[] decryptedBytes = staticEncryptionService.decrypt(dbData);
            String phoneNumberValue = new String(decryptedBytes, StandardCharsets.UTF_8);
            // Use the factory method of PhoneNumber to handle parsing and validation
            return PhoneNumber.ofNullable(phoneNumberValue);
        } catch (EncryptionException e) {
            log.error("Failed to decrypt PhoneNumber from database data.", e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Failed to parse decrypted string into PhoneNumber: {}", e.getMessage(), e);
            // Decide how to handle: return null, throw specific exception?
            // For now, rethrowing as EncryptionException to signal a problem with the stored data.
            throw new EncryptionException("Decrypted data could not be parsed into a valid PhoneNumber.", e);
        }
    }
}

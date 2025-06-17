/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// src/main/java/me/amlu/authserver/security/util/EncryptedEmailAddressConverter.java
package me.amlu.authserver.security.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import me.amlu.authserver.exceptions.EncryptionException;
import me.amlu.authserver.security.service.EncryptionService;
import me.amlu.authserver.user.model.vo.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Converter
@Component
public class EncryptedEmailAddressConverter implements AttributeConverter<EmailAddress, byte[]> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedEmailAddressConverter.class);
    private static EncryptionService staticEncryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        if (EncryptedEmailAddressConverter.staticEncryptionService == null) {
            EncryptedEmailAddressConverter.staticEncryptionService = encryptionService;
            log.info("EncryptionService injected into EncryptedEmailAddressConverter.");
        }
    }

    @Override
    public byte[] convertToDatabaseColumn(EmailAddress attribute) {
        if (attribute == null || !StringUtils.hasText(attribute.getValue())) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not available in EncryptedEmailAddressConverter for encryption.");
            throw new IllegalStateException("EncryptionService not available.");
        }
        try {
            byte[] emailBytes = attribute.getValue().getBytes(StandardCharsets.UTF_8);
            return staticEncryptionService.encrypt(emailBytes);
        } catch (EncryptionException e) {
            log.error("Failed to encrypt EmailAddress: {}", attribute.getValue(), e);
            throw e; // Re-throw to indicate failure
        }
    }

    @Override
    public EmailAddress convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            log.error("EncryptionService not available in EncryptedEmailAddressConverter for decryption.");
            throw new IllegalStateException("EncryptionService not available.");
        }
        try {
            byte[] decryptedBytes = staticEncryptionService.decrypt(dbData);
            String emailValue = new String(decryptedBytes, StandardCharsets.UTF_8);
            return new EmailAddress(emailValue);
        } catch (EncryptionException e) {
            log.error("Failed to decrypt EmailAddress from database data.", e);
            throw e; // Re-throw to indicate failure
        }
    }
}

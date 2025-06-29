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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = false) // autoApply=false means you must explicitly use @Convert
public class PhoneNumberConverter implements AttributeConverter<PhoneNumber, String> {

    private static final Logger log = LoggerFactory.getLogger(PhoneNumberConverter.class);
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Override
    public String convertToDatabaseColumn(PhoneNumber attribute) {
        if (attribute == null) {
            return null;
        }
        // Store in E.164 format (e.g., +14155552671) - recommended standard
        return phoneUtil.format(attribute, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    @Override
    public PhoneNumber convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            // Assuming E.164 format in DB, null for default region
            return phoneUtil.parse(dbData, null);
        } catch (NumberParseException e) {
            log.error("Failed to parse phone number string '{}' from database: {}", dbData, e.getMessage());
            // Depending on requirements, return null or throw a runtime exception
            // Returning null might be safer for loading existing data
            return null;
            // Or: throw new IllegalArgumentException("Invalid phone number format in database: " + dbData, e);
        }
    }
}

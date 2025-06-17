/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// src/main/java/me/amlu/authserver/validation/RegionCodeValidator.java
package me.amlu.authserver.validation;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Set;

public class RegionCodeValidator implements ConstraintValidator<ValidRegionCode, String> {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private Set<String> supportedRegions;

    @Override
    public void initialize(ValidRegionCode constraintAnnotation) {
        // Pre-load supported regions for efficiency
        supportedRegions = phoneUtil.getSupportedRegions();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null or empty values are considered valid here, as the field might be optional.
        // Use @NotBlank or @NotNull on the DTO field if it's mandatory.
        if (!StringUtils.hasText(value)) {
            return true;
        }
        // Region codes are typically uppercase
        return supportedRegions.contains(value.toUpperCase());
    }
}

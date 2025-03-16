/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import me.amlu.shop.amlume_shop.commons.PostalCodeUtils;

public class ValidPostalCodeValidator implements ConstraintValidator<ValidPostalCode, String> {

    private me.amlu.shop.amlume_shop.payload.AddressDto addressDto;

    @Override
    public boolean isValid(String postalCode, ConstraintValidatorContext context) {
        if (postalCode == null) {
            return true; // Let @NotBlank handle null values
        }
        // Get the country code from AddressDto (might need to pass it as a parameter)
        String countryCode = addressDto.country(); // Get country code from DTO
        String regex = PostalCodeUtils.getPostalCodeRegex(countryCode);
        return postalCode.matches(regex);
    }
}

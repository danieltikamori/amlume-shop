/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component // Make it a Spring component to inject CompromisedPasswordChecker
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    // Define regular expression for password patterns - consider making these configurable
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    // OWASP recommendation: !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~].*");
    private static final int MIN_LENGTH = 12; // Already in @Size, but good to have here too
    private static final int MAX_LENGTH = 127;

    private final CompromisedPasswordChecker compromisedPasswordChecker;

    @Autowired // Spring will inject this bean
    public PasswordConstraintValidator(CompromisedPasswordChecker compromisedPasswordChecker) {
        this.compromisedPasswordChecker = compromisedPasswordChecker;
    }

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // We can access annotation parameters here if we add any
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(password)) {
            // Let @NotBlank handle empty/null. If it can be optional, return true here.
            // For a password field that IS mandatory, this check might be redundant
            // if @NotBlank is also present. If password can be truly optional (like in
            // passkey-first registration where password field in DTO might be null),
            // then return true for null/blank.
            return true; // Assuming @NotBlank handles mandatory check
        }

        List<String> violations = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password.length() > MAX_LENGTH) {
            violations.add("Password must be no more than " + MAX_LENGTH + " characters long.");
        }
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one uppercase letter.");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one lowercase letter.");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one digit.");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one special character (e.g., !@#$%^&*).");
        }

        // Check for compromised password
        if (compromisedPasswordChecker.check(password).isCompromised()) {
            violations.add("Password has been found in a data breach and cannot be used.");
        }

        if (!violations.isEmpty()) {
            context.disableDefaultConstraintViolation(); // Disable default message
            // Add all specific violation messages
            for (String violation : violations) {
                context.buildConstraintViolationWithTemplate(violation).addConstraintViolation();
            }
            return false;
        }
        return true;
    }
}

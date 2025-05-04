/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import me.amlu.shop.amlume_shop.security.config.properties.MfaProperties;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Objects;

@Configuration
public class PasswordConfig {

    /**
     * Creates a TimeBasedOneTimePasswordGenerator bean configured using MfaProperties.
     *
     * @param mfaProperties The configuration properties for MFA/TOTP.
     * @return A configured TimeBasedOneTimePasswordGenerator instance.
     * @throws BeanCreationException if the configuration is invalid (e.g., unsupported algorithm, invalid length/time step).
     */
    @Bean
    public TimeBasedOneTimePasswordGenerator timeBasedOneTimePasswordGenerator(
            MfaProperties mfaProperties) { // Inject MfaProperties instead of individual @Value parameters
        Objects.requireNonNull(mfaProperties, "MfaProperties cannot be null");
        try {
            // Use getters from the injected MfaProperties bean
            return new TimeBasedOneTimePasswordGenerator(
                    Duration.ofSeconds(mfaProperties.getTimeStepSeconds()),
                    mfaProperties.getCodeLength(),
                    mfaProperties.getAlgorithm()
            );
            // } catch (NoSuchAlgorithmException e) { // REMOVED - This exception is not thrown by the constructor
            //     // Log the specific algorithm that failed
            //     throw new BeanCreationException(
            //             "Failed to create TimeBasedOneTimePasswordGenerator due to unsupported algorithm: " + mfaProperties.getAlgorithm(),
            //             e
            //     );
        } catch (IllegalArgumentException e) {
            // Catch potential issues like invalid algorithm name, length, or time step from the library constructor
            // Provide more context in the error message
            throw new BeanCreationException(
                    String.format("Failed to create TimeBasedOneTimePasswordGenerator due to invalid configuration " +
                                    "(algorithm: '%s', length: %d, timeStep: %d seconds): %s",
                            mfaProperties.getAlgorithm(),
                            mfaProperties.getCodeLength(),
                            mfaProperties.getTimeStepSeconds(),
                            e.getMessage()),
                    e
            );
        }
        // Optional: Catch broader Exception if concerned about other potential runtime issues, though less specific.
        // catch (Exception e) {
        //     throw new BeanCreationException(
        //             "An unexpected error occurred while creating TimeBasedOneTimePasswordGenerator", e);
        // }
    }

}


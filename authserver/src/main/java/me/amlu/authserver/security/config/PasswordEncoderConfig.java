    /*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */    // Example: PasswordEncoderConfig.java
    package me.amlu.authserver.security.config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
    import org.springframework.security.crypto.password.PasswordEncoder;

    @Configuration
    public class PasswordEncoderConfig {

        /**
         * Bean for PasswordEncoder.
         * This uses Argon2 or bcrypt as the default password hashing algorithm.
         *
         * @return PasswordEncoder instance.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // Uses the default bcrypt encoder
            // Argon2 configuration - parameters can be tuned
            return new Argon2PasswordEncoder(
                    16,    // saltLength
                    32,    // hashLength
                    1,     // parallelism (adjust based on CPU cores)
                    1 << 14, // memory cost (16MB - adjust based on available RAM)
                    3       // iterations (increase for more security, impacts performance)
            );
        }
    }

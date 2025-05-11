/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// If you have a custom @Password annotation for more complex rules,
// you can add it here as well, e.g.:
// import me.amlu.authserver.validation.Password; // Assuming this is your custom annotation

public record ChangePasswordRequest(
        @NotBlank(message = "Old password cannot be blank.")
        String oldPassword,

        @NotBlank(message = "Password cannot be blank.")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
        // If you have a custom @Password annotation for complexity (e.g., requiring uppercase, number, symbol):
        // @Password // Add this back if it's a valid, defined custom annotation in your project
        String password
) {
    // Records are concise. No further methods are typically needed for a simple DTO.
    // If you have a custom @Password annotation, ensure it's correctly defined with a
    // ConstraintValidator.
}
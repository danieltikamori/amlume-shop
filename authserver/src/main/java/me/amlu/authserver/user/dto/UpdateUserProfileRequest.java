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

import jakarta.validation.constraints.Size;
// If you have a custom @Phone annotation that validates a String, import it here.
// e.g., import me.amlu.shop.amlume_shop.security.config.Phone;

public record UpdateUserProfileRequest(
        // If firstName is provided, it will be updated.
        // User.updateFirstName() contains an Assert.hasText() for validation.
        // Max length can be defined here as a DTO-level check.
        @Size(max = 50, message = "First name cannot exceed 50 characters.")
        String firstName,

        // If lastName is provided (can be an empty string to clear, or null for no change).
        @Size(max = 50, message = "Last name cannot exceed 50 characters.")
        String lastName,

        // If nickname is provided (can be an empty string to clear, or null for no change).
        @Size(max = 50, message = "Nickname cannot exceed 50 characters.")
        String nickname,

        // Accept mobile number as a String. Validation will occur in the service layer
        // when converting to the PhoneNumber VO, or via a custom @Phone annotation if adapted for String.
        // @Phone // If you adapt/create a @Phone annotation for String
        String mobileNumber
) {
}
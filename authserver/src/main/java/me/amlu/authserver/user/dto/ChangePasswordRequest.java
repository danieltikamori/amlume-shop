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
import me.amlu.authserver.validation.ValidPassword;

import static me.amlu.authserver.common.SecurityConstants.MAX_PASSWORD_LENGTH;
import static me.amlu.authserver.common.SecurityConstants.MIN_PASSWORD_LENGTH;

// If you have a custom @Password annotation for more complex rules,
// you can add it here as well, e.g.:
// import me.amlu.authserver.validation.Password; // Assuming this is your custom annotation

public record ChangePasswordRequest(
        @NotBlank(message = "Old password cannot be blank.")
        String oldPassword,

        @NotBlank(message = "Password cannot be blank.")
        @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH, message = "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + "characters.")
        @ValidPassword
        String password
) {
}

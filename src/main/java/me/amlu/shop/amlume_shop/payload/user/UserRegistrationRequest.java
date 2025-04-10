/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.payload.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import me.amlu.shop.amlume_shop.user_management.Username;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserRegistrationRequest {

//    private String firstname;
//    private String lastname;

    private Username username;

    @NotBlank
    @Size(min = 12, max = 120)
    private String password;

    @Email
    @Size(min = 5, max = 50)
    private String userEmail;

    private UserRole roles;

    private boolean mfaEnabled;

    private String captchaResponse;

}

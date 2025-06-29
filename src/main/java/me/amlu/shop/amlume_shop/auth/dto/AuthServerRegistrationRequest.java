/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.auth.dto;

import org.jspecify.annotations.NonNull;

// This DTO is used to send registration data to the authserver API.
// Its structure should match the RegistrationRequest DTO expected by authserver's /api/register endpoint.
public record AuthServerRegistrationRequest(
        String givenName,
        String surname,
        String nickname,
        String userEmail,
        String password, // Contains the raw password
        String mobileNumber,
        String defaultRegion
) {
    // NOTE: The default toString() for records includes all components, including the password.
    // If instances of this record are logged, the password will appear in logs.
    // Consider overriding toString() to redact the password if logging this object is necessary.

    @NullMarked
    @Override
    public String toString() {
        return "AuthServerRegistrationRequest{" +
                "givenName='" + givenName + '\'' +
                ", surname='" + surname + '\'' +
                ", nickname='" + nickname + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", password='[REDACTED]'" + // Redact the password
                ", mobileNumber='" + mobileNumber + '\'' +
                ", defaultRegion='" + defaultRegion + '\'' +
                '}';
    }
}

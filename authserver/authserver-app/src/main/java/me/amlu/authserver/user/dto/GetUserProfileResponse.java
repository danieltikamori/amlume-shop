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

// No validation annotations are strictly necessary for a response DTO,
// but can be added if they serve a specific documentation or tooling purpose.
// For example: @Size(max = 50) if you want to indicate a max display length.

public record GetUserProfileResponse(
        Long id,
        String externalId, // User handle for WebAuthn
        String givenName,
        String middleName, // Can be null
        String surname,    // Can be null
        String nickname,   // Can be null
        String email,
        String recoveryEmail, // Can be null
        String mobileNumber // String representation. Can be null
) {
}

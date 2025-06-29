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

import java.util.Set;

public record GetUserResponse(String userIdSubject, String givenName, String surname, String nickname, String email,
                              String mobileNumber, Set<String> roles) {
//    private String userIdSubject; // Could be email or a UUID, not the internal DB ID

//    private String email; // From EmailAddress.getValue()
//    private String mobileNumber; // From PhoneNumber.getE164Value() or a formatted version

    // NO password field.
    // NO HashedPassword field.

}

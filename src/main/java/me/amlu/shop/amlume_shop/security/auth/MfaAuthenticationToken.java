/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.auth;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class MfaAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final String mfaCode;

    // Constructor for initial authentication request
    public MfaAuthenticationToken(String username, String password, String mfaCode) {
        super(username, password);
        this.mfaCode = mfaCode;
        setAuthenticated(false);
    }

    // Constructor for successful authentication
    public MfaAuthenticationToken(Object principal, Object credentials,
                                  String mfaCode,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.mfaCode = mfaCode;
    }

}

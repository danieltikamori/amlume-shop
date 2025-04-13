/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.exceptions;

import com.maxmind.db.ClosedDatabaseException;
import com.maxmind.db.DeserializationException;
import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.db.ParameterNotFoundException;

public class GeoIpLookupException extends RuntimeException {

    public GeoIpLookupException(String message, InvalidDatabaseException e) {
        super(message);
    }

    public GeoIpLookupException(String message, DeserializationException cause) {
        super(message, cause);
    }

    public GeoIpLookupException(String message, ParameterNotFoundException cause) {
        super(message, cause);
    }

    public GeoIpLookupException(String message, ClosedDatabaseException cause) {
        super(message, cause);
    }

    public GeoIpLookupException(String message) {
        super(message);
    }

    public GeoIpLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}

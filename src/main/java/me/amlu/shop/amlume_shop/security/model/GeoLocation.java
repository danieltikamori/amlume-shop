/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeoLocation {
    private static final String UNKNOWN = "Unknown";
    private static final String DEFAULT_LOCATION = "Null Island";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_COUNTRY_CODE = "XX";


    private final String countryCode;
    private final String countryName;
    private final String city;
    private final String postalCode;
    private final Double latitude;
    private final Double longitude;
    private final String timeZone;
    private final String subdivisionName;
    private final String subdivisionCode;
    private final String asn;

    public static GeoLocation unknown() {
        return GeoLocation.builder()
                .countryCode(DEFAULT_COUNTRY_CODE)
                .countryName(DEFAULT_LOCATION)
                .city(DEFAULT_LOCATION)
                .postalCode(UNKNOWN)
                .latitude(0.0)
                .longitude(0.0)
                .timeZone(DEFAULT_TIMEZONE)
                .subdivisionName(UNKNOWN)
                .subdivisionCode(DEFAULT_COUNTRY_CODE)
                .asn(null)
                .build();
    }
}

//This implementation provides:
//
//Comprehensive list of known VPN ASNs
//
//Null-safe VPN detection
//
//Metrics for VPN detection
//
//Debug logging for VPN detection
//
//Easy way to update the VPN ASN list
//
//Support for multiple VPN providers worldwide
//
//
//You might want to consider:
//
//Loading the ASN list from a configuration file
//
//Implementing regular updates of the ASN list
//
//Adding more sophisticated VPN detection logic
//
//Implementing ASN reputation scoring
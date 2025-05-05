/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.dto;

public record ErrorResponse(int statusCode, String code, String message) {

    public ErrorResponse(String message, int statusCode) {
        this(statusCode, null, message);
    }

    public ErrorResponse(String code, String message) {
        this(0, code, message); // Assuming a default status code of 0 or you can choose a more appropriate default
    }

    public ErrorResponse(String code, String message, int value) {
        this(value, code, message);
    }

    public ErrorResponse() {
        this(0, null, null); // Default constructor
    }
}

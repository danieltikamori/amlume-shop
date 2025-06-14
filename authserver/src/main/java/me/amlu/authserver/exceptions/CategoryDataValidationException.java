/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.exceptions;

import me.amlu.shop.amlume_shop.exceptions.BaseException;

public class CategoryDataValidationException extends BaseException {
    public CategoryDataValidationException(String message) {
        super(message);
    }

    public CategoryDataValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CategoryDataValidationException(Throwable cause) {
        super(cause);
    }

    public CategoryDataValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

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

import java.io.Serial;

public class CacheOperationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CacheOperationException(String message) {
        super(message);
    }

    public CacheOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class CacheInitializationException extends CacheOperationException {
        public CacheInitializationException(String message) {
            super(message);
        }

        public CacheInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    public static class CacheWriteException extends CacheOperationException {
        public CacheWriteException(String message) {
            super(message);
        }

        public CacheWriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CacheReadException extends CacheOperationException {
        public CacheReadException(String message) {
            super(message);
        }

        public CacheReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CacheUpdateException extends CacheOperationException {
        public CacheUpdateException(String message) {
            super(message);
        }

        public CacheUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    public static class CacheInvalidationException extends CacheOperationException {
        public CacheInvalidationException(String message) {
            super(message);
        }

        public CacheInvalidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CacheDeleteException extends CacheOperationException {
        public CacheDeleteException(String message) {
            super(message);
        }

        public CacheDeleteException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CacheClearException extends CacheOperationException {
        public CacheClearException(String message) {
            super(message);
        }

        public CacheClearException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

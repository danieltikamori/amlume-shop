/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management.dto;

/**
 * Represents category data for transfer (API requests).
 * Implemented as an immutable record.
 */
public record CreateCategoryRequest(
        Long categoryId,
        String categoryName,
        String description,
        Long parentId,
        // Fields derived/mapped from entity VOs
        Integer level,
        String path,
        String status,
        Boolean active,
        String reason,
        Long managerId
        // Add other fields like productCount, subCategoryCount if needed
) {
    // We can add validation annotations here if desired
    // e.g., @NotBlank String categoryName, ...
}

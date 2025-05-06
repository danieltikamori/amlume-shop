/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.order_management.Order;
import me.amlu.shop.amlume_shop.product_management.Product;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    private static final int MAX_DEPTH = 10;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(InputValidator.class);

    public <T> boolean validateResource(T resource) {
        if (resource == null) {
            log.warn("Resource validation failed: null resource");
            return false;
        }

        // Check for maximum object depth to prevent stack overflow attacks
        if (exceedsMaxDepth(resource, 0)) {
            log.warn("Resource validation failed: max depth exceeded");
            return false;
        }

        // Type validation
        if (!isSupportedType(resource)) {
            log.warn("Resource validation failed: unsupported type {}",
                    resource.getClass().getSimpleName());
            return false;
        }

        return true;
    }

    private boolean exceedsMaxDepth(Object obj, int depth) {
        if (depth > MAX_DEPTH) return true;
        if (obj instanceof Product product) {
            return exceedsMaxDepth(product.getCategory(), depth + 1);
//        } else if (obj instanceof Category) {
//            Category category = (Category) obj;
//            // Assuming Category has a method to get its subcategories
//            for (Category subcategory : category.getSubcategories()) {
//                if (exceedsMaxDepth(subcategory, depth + 1)) return true;
//            }
        } else if (obj instanceof Order order) {
            // Assuming Order has a method to get its products
            for (Product product : order.getProducts()) {
                if (exceedsMaxDepth(product, depth + 1)) return true;
            }
        }
        return false;
    }

    private boolean isSupportedType(Object resource) {
        return resource instanceof Product ||
                resource instanceof Order ||
                resource instanceof Category;
    }
}

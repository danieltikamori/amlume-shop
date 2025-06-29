/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

/**
 * Defines the application roles used for authorization.
 * The names include the "ROLE_" prefix, which is commonly used by Spring Security.
 */
public enum AppRole {

    // Customer roles
    ROLE_CUSTOMER,
    ROLE_ORDER_OWNER,

    // Order roles
    ROLE_DISPUTE_HANDLER,
    ROLE_ORDER_VIEWER,
    ROLE_ORDER_APPROVER,

    // Management roles
    ROLE_MANAGER,
    ROLE_CATEGORY_MANAGER,
    ROLE_CUSTOMER_MANAGER,
    ROLE_PRODUCT_MANAGER,
    ROLE_PREMIUM_PRODUCT_MANAGER,
    ROLE_PRODUCT_MANAGER_DELIVERY_PERSON,
    ROLE_PRODUCT_REVIEWER_MANAGER,
    ROLE_SELLER_MANAGER,
    ROLE_SHIPPING_MANAGER,

    // Staff roles
    ROLE_CUSTOMER_STAFF,
    ROLE_CATEGORY_MANAGER_STAFF,
    ROLE_PRODUCT_MANAGER_STAFF,
    ROLE_PRODUCT_REVIEWER_STAFF,

    // Seller member roles
    ROLE_SELLER,
    ROLE_SELLER_STAFF,
    ROLE_SELLER_DELIVERY_PERSON,

    // Category handlers
    ROLE_RESTRICTED_CATEGORY_HANDLER,
    ROLE_CATEGORY_VIEWER,

    // Product related
    ROLE_PRODUCT_EDITOR,
    ROLE_PRODUCT_REVIEWER,
    ROLE_PRODUCT_VIEWER,
    ROLE_RESTRICTED_PRODUCT_HANDLER,

    // Admin Roles
    ROLE_MAIN_CATEGORY_ADMIN,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN,
    ROLE_MODERATOR,
    ROLE_ROOT,

    // Base user
    ROLE_USER

    // Removed the toAuthority() method as it was redundant and incorrect.
    // Spring Security can use enum.name() directly when the ROLE_ prefix is included in the name.
    /*
    public String toAuthority() {
        return "ROLE_" + this.name(); // Incorrect: would produce ROLE_ROLE_...
    }
    */
}

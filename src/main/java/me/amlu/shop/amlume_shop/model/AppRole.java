/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.model;

public enum AppRole {
    ROLE_USER,
    ROLE_CUSTOMER,
    ROLE_CUSTOMER_MANAGER,
    ROLE_CUSTOMER_STAFF,
    ROLE_ORDER_OWNER,
    ROLE_SELLER,
    ROLE_SELLER_MANAGER,
    ROLE_SELLER_STAFF,
    ROLE_SELLER_DELIVERY_PERSON,
    ROLE_ROOT,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN,
    ROLE_MANAGER,
    ROLE_CATEGORY_MANAGER,
    ROLE_PRODUCT_SELLER,
    ROLE_PRODUCT_EDITOR,
    ROLE_PRODUCT_MANAGER,
    ROLE_PRODUCT_REVIEWER,
    ROLE_PRODUCT_REVIEWER_MANAGER,
    ROLE_PRODUCT_REVIEWER_STAFF,
    ROLE_CATEGORY_MANAGER_MANAGER,
    ROLE_CATEGORY_MANAGER_STAFF,
    ROLE_PRODUCT_MANAGER_MANAGER,
    ROLE_PRODUCT_MANAGER_STAFF,
    ROLE_PRODUCT_MANAGER_DELIVERY_PERSON,
    ROLE_MODERATOR;


    // Helper method to get role with ROLE_ prefix
    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}

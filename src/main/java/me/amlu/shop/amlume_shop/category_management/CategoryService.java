/*
 * Copyright (c) 2024-2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.category_management;

import me.amlu.shop.amlume_shop.payload.CreateCategoryRequest;
import me.amlu.shop.amlume_shop.payload.GetCategoryResponse;

import java.util.List;

public interface CategoryService {

    GetCategoryResponse getAllCategories(int pageNumber, int pageSize, String sortBy, String sortDir);

    List<Category> findAllSubcategories(Category category);

    List<Category> findAncestors(Category category);

    boolean isDescendantOf(Category possibleDescendant, Category possibleAncestor);

    int getDepth(Category category);

    // Prevent circular references
    void validateHierarchy(Category category, Category newParent);

    CreateCategoryRequest createCategory(CreateCategoryRequest createCategoryRequest);


    CreateCategoryRequest deleteCategory(Long categoryId);

    CreateCategoryRequest updateCategory(Long categoryId, CreateCategoryRequest category);
}

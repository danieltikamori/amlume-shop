/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at fuiwzchps@mozmail.com for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.payload.CategoryDTO;
import me.amlu.shop.amlume_shop.payload.CategoryResponse;

public interface CategoryService {

    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortDir);

    CategoryDTO createCategory(CategoryDTO categoryDTO);


    CategoryDTO deleteCategory(Long categoryId);

    CategoryDTO updateCategory(Long categoryId, CategoryDTO category);
}

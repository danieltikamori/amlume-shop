/*
 * Copyright (c) 2024 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlume.shop.amlume_shop.service;

import me.amlume.shop.amlume_shop.model.Category;

import java.util.concurrent.CopyOnWriteArrayList;

public interface CategoryService {

    CopyOnWriteArrayList<Category> getAllCategories();

    void createCategory(Category category);


    String deleteCategory(Long category_id);

    Category updateCategory(Long categoryId, Category category);
}

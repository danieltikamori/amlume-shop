/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.product_management;

import me.amlu.shop.amlume_shop.exceptions.ProductAlreadyExistsException;
import me.amlu.shop.amlume_shop.payload.ProductDTO;
import me.amlu.shop.amlume_shop.payload.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductService {
    ProductDTO addProduct(ProductDTO product, Long categoryId) throws ProductAlreadyExistsException;

    ProductResponse getAllProducts(int pageNumber, int pageSize, String sortBy, String sortDir);

    ProductResponse searchByCategory(Long categoryId, int pageNumber, int pageSize, String sortBy, String sortDir);

    ProductResponse searchProductByKeyword(String keyword, int pageNumber, int pageSize, String sortBy, String sortDir);

    ProductDTO updateProduct(ProductDTO product, Long productId);

    ProductDTO deleteProduct(Long productId);

    ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;

    boolean isValidProduct(Product product);
}

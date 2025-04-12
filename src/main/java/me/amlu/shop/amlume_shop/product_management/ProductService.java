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

import me.amlu.shop.amlume_shop.exceptions.APIException;
import me.amlu.shop.amlume_shop.exceptions.NotFoundException;
import me.amlu.shop.amlume_shop.exceptions.ProductAlreadyExistsException;
import me.amlu.shop.amlume_shop.exceptions.ProductDataValidationException;
import me.amlu.shop.amlume_shop.exceptions.ResourceNotFoundException;
import me.amlu.shop.amlume_shop.payload.ProductDTO;
import me.amlu.shop.amlume_shop.payload.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * Service interface for managing Products.
 * Defines operations for creating, retrieving, updating, and deleting products.
 */
public interface ProductService {

    /**
     * Adds a new product to a specified category.
     *
     * @param productDTO DTO containing the details of the product to add.
     * @param categoryId The ID of the category to associate the product with.
     * @return A DTO representing the newly created product.
     * @throws IllegalArgumentException       if productDTO or categoryId is null.
     * @throws ResourceNotFoundException      if the category with the given ID is not found.
     * @throws ProductAlreadyExistsException  if a product with the same name already exists.
     * @throws APIException                   if there's a data integrity issue during saving.
     * @throws ProductDataValidationException if the productDTO fails basic validation (added for clarity).
     */
    ProductDTO addProduct(ProductDTO productDTO, Long categoryId) throws ProductAlreadyExistsException, ResourceNotFoundException, APIException, ProductDataValidationException; // Added exceptions from impl

    /**
     * Retrieves a paginated list of all products.
     *
     * @param pageNumber The page number to retrieve (0-indexed).
     * @param pageSize   The number of products per page.
     * @param sortBy     The field to sort by.
     * @param sortDir    The sort direction ("asc" or "desc").
     * @return A ProductResponse containing the paginated list of products and pagination details.
     */
    ProductResponse getAllProducts(int pageNumber, int pageSize, String sortBy, String sortDir);

    /**
     * Retrieves a paginated list of products belonging to a specific category.
     *
     * @param categoryId The ID of the category.
     * @param pageNumber The page number to retrieve (0-indexed).
     * @param pageSize   The number of products per page.
     * @param sortBy     The field to sort by.
     * @param sortDir    The sort direction ("asc" or "desc").
     * @return A ProductResponse containing the paginated list of products and pagination details.
     * @throws ResourceNotFoundException if the category with the given ID is not found.
     * @throws NotFoundException         if the category exists but has no products (as per impl).
     */
    ProductResponse searchByCategory(Long categoryId, int pageNumber, int pageSize, String sortBy, String sortDir) throws ResourceNotFoundException, NotFoundException; // Added exceptions from impl

    /**
     * Retrieves a paginated list of products matching a keyword in their name.
     *
     * @param keyword    The keyword to search for in product names.
     * @param pageNumber The page number to retrieve (0-indexed).
     * @param pageSize   The number of products per page.
     * @param sortBy     The field to sort by.
     * @param sortDir    The sort direction ("asc" or "desc").
     * @return A ProductResponse containing the paginated list of products and pagination details.
     * @throws ResourceNotFoundException if no products match the keyword (as per impl).
     */
    ProductResponse searchProductByKeyword(String keyword, int pageNumber, int pageSize, String sortBy, String sortDir) throws ResourceNotFoundException; // Added exception from impl

    /**
     * Updates an existing product.
     * Note: The implementation needs careful handling of Value Objects.
     *
     * @param productDTO DTO containing the updated product details.
     * @param productId  The ID of the product to update.
     * @return A DTO representing the updated product.
     * @throws ResourceNotFoundException      if the product with the given ID is not found.
     * @throws ProductDataValidationException if the productDTO fails basic validation (should be added to impl).
     */
    ProductDTO updateProduct(ProductDTO productDTO, Long productId) throws ResourceNotFoundException, ProductDataValidationException; // Added exceptions

    /**
     * Deletes (soft deletes) a product by its ID.
     *
     * @param productId The ID of the product to delete.
     * @return A DTO representing the product before deletion.
     * @throws ResourceNotFoundException if the product with the given ID is not found.
     */
    ProductDTO deleteProduct(Long productId) throws ResourceNotFoundException; // Added exception

    /**
     * Updates the image associated with a product.
     *
     * @param productId The ID of the product to update.
     * @param image     The new image file.
     * @return A DTO representing the product with the updated image information.
     * @throws ResourceNotFoundException if the product with the given ID is not found.
     * @throws IOException               if there's an error during file upload.
     */
    ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException, ResourceNotFoundException; // Added exception

    // Removed isValidProduct method as it's not appropriate for the service interface contract.
    // boolean isValidProduct(Product product);
}

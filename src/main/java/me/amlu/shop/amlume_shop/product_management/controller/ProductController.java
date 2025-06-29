/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.product_management.controller;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.ProductAlreadyExistsException;
import me.amlu.shop.amlume_shop.product_management.dto.CreateProductRequest;
import me.amlu.shop.amlume_shop.product_management.dto.GetProductResponse;
import me.amlu.shop.amlume_shop.product_management.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @PostMapping("admin/categories/{categoryId}/product")
    public ResponseEntity<CreateProductRequest> addProduct(@Valid @RequestBody CreateProductRequest productDTO, @PathVariable Long categoryId) throws ProductAlreadyExistsException {

        CreateProductRequest savedProductDTO = productService.addProduct(productDTO, categoryId);
        return new ResponseEntity<>(savedProductDTO, null, HttpStatus.CREATED);

    }

    @GetMapping("public/products")
    public ResponseEntity<GetProductResponse> getAllProducts(
            @RequestParam(value = "pageNumber", defaultValue = Constants.PAGE_NUMBER, required = false) int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = Constants.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = Constants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = Constants.SORT_DIR, required = false) String sortDir
    ) {

        GetProductResponse getProductResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortDir);
        return new ResponseEntity<>(getProductResponse, null, HttpStatus.OK);

    }

    @GetMapping("public/categories/{categoryId}/products")
    public ResponseEntity<GetProductResponse> getProductsByCategory(@PathVariable Long categoryId,
                                                                    @RequestParam(value = "pageNumber", defaultValue = Constants.PAGE_NUMBER, required = false) int pageNumber,
                                                                    @RequestParam(value = "pageSize", defaultValue = Constants.PAGE_SIZE, required = false) int pageSize,
                                                                    @RequestParam(value = "sortBy", defaultValue = Constants.SORT_PRODUCTS_BY, required = false) String sortBy,
                                                                    @RequestParam(value = "sortDir", defaultValue = Constants.SORT_DIR, required = false) String sortDir) {

        GetProductResponse getProductResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy, sortDir);
        return new ResponseEntity<>(getProductResponse, null, HttpStatus.OK);

    }

    @GetMapping("public/products/keyword/{keyword}")
    public ResponseEntity<GetProductResponse> getProductsByKeyword(@PathVariable String keyword,
                                                                   @RequestParam(value = "pageNumber", defaultValue = Constants.PAGE_NUMBER, required = false) int pageNumber,
                                                                   @RequestParam(value = "pageSize", defaultValue = Constants.PAGE_SIZE, required = false) int pageSize,
                                                                   @RequestParam(value = "sortBy", defaultValue = Constants.SORT_PRODUCTS_BY, required = false) String sortBy,
                                                                   @RequestParam(value = "sortDir", defaultValue = Constants.SORT_DIR, required = false) String sortDir) {

        GetProductResponse getProductResponse = productService.searchProductByKeyword(keyword, pageNumber, pageSize, sortBy, sortDir);
        return new ResponseEntity<>(getProductResponse, null, HttpStatus.FOUND);
    }

    @PutMapping("admin/products/{productId}")
    public ResponseEntity<CreateProductRequest> updateProduct(@Valid @RequestBody CreateProductRequest productDTO, @PathVariable Long productId) {

        CreateProductRequest updatedProductDTO = productService.updateProduct(productDTO, productId);
        return new ResponseEntity<>(updatedProductDTO, null, HttpStatus.OK);
    }

    @DeleteMapping("admin/products/{productId}")
    public ResponseEntity<CreateProductRequest> deleteProduct(@PathVariable Long productId) {

        CreateProductRequest deletedProduct = productService.deleteProduct(productId);
        return new ResponseEntity<>(deletedProduct, null, HttpStatus.OK);
    }

    @PutMapping("admin/products/{productId}/image")
    public ResponseEntity<CreateProductRequest> updateProductImage(@PathVariable Long productId, @RequestParam("image") MultipartFile image) throws IOException {

        CreateProductRequest updatedProductDTO = productService.updateProductImage(productId, image);
        return new ResponseEntity<>(updatedProductDTO, null, HttpStatus.OK);
    }
}

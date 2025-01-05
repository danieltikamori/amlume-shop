/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.service;

import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.Category;
import me.amlu.shop.amlume_shop.model.Product;
import me.amlu.shop.amlume_shop.payload.ProductDTO;
import me.amlu.shop.amlume_shop.payload.ProductResponse;
import me.amlu.shop.amlume_shop.repositories.CategoryRepository;
import me.amlu.shop.amlume_shop.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
public class ProductServiceImpl implements ProductService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final FileService fileService;

    @Value("${project.image}")
    private String path;


    public ProductServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository, ModelMapper modelMapper, FileService fileService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
        this.fileService = fileService;
    }

    @Override
    @Transactional
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId) throws IllegalArgumentException, ResourceNotFoundException, ProductAlreadyExistsException, APIException {
        try {
            if (productDTO == null) {
                throw new IllegalArgumentException("ProductDTO cannot be null");
            }
            if (categoryId == null) {
                throw new IllegalArgumentException("Category ID cannot be null");
            }

            validateProductData(productDTO); // Call the product data validation method

//            if (productRepository.existsByProductNameIgnoreCase(productDTO.getProductName())) {
//                throw new ProductAlreadyExistsException(productDTO.getProductName());
//            }

            // Check if product name exists
            if (productRepository.existsByProductNameIgnoreCase(
                    Objects.requireNonNull(productDTO.getProductName()))) {
                throw new ProductAlreadyExistsException(productDTO.getProductName());
            }

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

            Product product = modelMapper.map(productDTO, Product.class);
            product.setProductImage("default.png");
            product.setCategory(category);

            calculateSpecialPrice(product);

            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct, ProductDTO.class);

        } catch (
                DataIntegrityViolationException e) {
            throw new APIException("Error saving product: " + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public ProductResponse getAllProducts(int pageNumber, int pageSize, String sortBy, String sortDir) {

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Product> pageProducts = productRepository.findAll(pageDetails);

        List<Product> products = pageProducts.getContent();
        if (!pageProducts.getContent().isEmpty()) {
            List<ProductDTO> productDTOList = products.stream()
                    .map(product -> modelMapper.map(product, ProductDTO.class))
                    .toList();

            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOList);
            productResponse.setPageNumber(pageProducts.getNumber());
            productResponse.setPageSize(pageProducts.getSize());
            productResponse.setTotalElements(pageProducts.getTotalElements());
            productResponse.setTotalPages(pageProducts.getTotalPages());
            productResponse.setLastPage(pageProducts.isLast());
            return productResponse;
        } else {
            throw new NotFoundException("No products found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse searchByCategory(Long categoryId, int pageNumber, int pageSize, String sortBy, String sortDir) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Product> productPage = productRepository.findByCategory(category, pageDetails);

        List<Product> products = productPage.getContent();
        if (!productPage.getContent().isEmpty()) {
            List<ProductDTO> productDTOList = products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOList);
            productResponse.setPageNumber(productPage.getNumber());
            productResponse.setPageSize(productPage.getSize());
            productResponse.setTotalElements(productPage.getTotalElements());
            productResponse.setTotalPages(productPage.getTotalPages());
            productResponse.setLastPage(productPage.isLast());
            return productResponse;
        } else {
            throw new NotFoundException("Category " + category.getCategoryName() + " has no products");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse searchProductByKeyword(String keyword, int pageNumber, int pageSize, String
            sortBy, String sortDir) {

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Product> productPage = productRepository.findByProductNameContainingIgnoreCase(keyword, pageDetails);

        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOList = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        if (products.isEmpty()) throw new ResourceNotFoundException("Product", "keyword", keyword);

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOList);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(ProductDTO productDTO, Long productId) {

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", String.valueOf(productId)));

        Product product = modelMapper.map(productDTO, Product.class);

        productFromDB.setProductName(product.getProductName());
        productFromDB.setProductDescription(product.getProductDescription());
        productFromDB.setProductQuantity(product.getProductQuantity());
        productFromDB.setProductPrice(product.getProductPrice());
        productFromDB.setProductDiscountPercentage(product.getProductDiscountPercentage());
        productFromDB.setProductSpecialPrice(product.getProductSpecialPrice());

        Product updatedProduct = productRepository.save(productFromDB);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    @Override
    @Transactional
    public ProductDTO deleteProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", String.valueOf(productId)));

        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    @Transactional
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        // Get the product from the database
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", String.valueOf(productId)));

        // Upload the product image to server
        // Get the file name of the new image
        String fileName = fileService.uploadImage(path, image);

        // Update the product image
        productFromDB.setProductImage(fileName);

        // Save the updated product
        Product updatedProduct = productRepository.save(productFromDB);

        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    private void validateProductData(ProductDTO productDTO) throws ProductDataValidationException {
        if (productDTO.getProductName() == null || productDTO.getProductName().isEmpty()) {
            throw new ProductDataValidationException("Product name is required");
        }
        if (productDTO.getProductDescription() == null || productDTO.getProductDescription().isEmpty()) {
            throw new ProductDataValidationException("Product description is required");
        }
        if (productDTO.getProductPrice() == null) {
            throw new ProductDataValidationException("Product price is required");
        }
        if (productDTO.getProductQuantity() == null) {
            throw new ProductDataValidationException("Product quantity is required");
        }
    }

    private void calculateSpecialPrice(Product product) {
        if (product.getProductPrice() != null && product.getProductDiscountPercentage() != null) {
            if (product.getProductDiscountPercentage().compareTo(BigDecimal.ZERO) >= 0) {
                BigDecimal discount = product.getProductPrice()
                        .multiply(product.getProductDiscountPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                BigDecimal specialPrice = product.getProductPrice().subtract(discount);
                product.setProductSpecialPrice(specialPrice);
            } else {
                throw new IllegalArgumentException("Product discount percentage must be positive.");
            }
        }
    }

}


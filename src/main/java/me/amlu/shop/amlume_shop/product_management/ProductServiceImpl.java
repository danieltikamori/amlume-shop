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

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.category_management.CategoryRepository;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.payload.FileUploadResult;
import me.amlu.shop.amlume_shop.payload.ProductDTO;
import me.amlu.shop.amlume_shop.payload.ProductResponse;
import me.amlu.shop.amlume_shop.resilience.ExponentialBackoffRateLimiter;
import me.amlu.shop.amlume_shop.service.FileService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.PRODUCT_LIST_CACHE;

/**
 * @author Daniel Itiro Tikamori
 * @version 1.0
 * <p>
 * CacheEvict on all modification methods, here is why:
 * 1. Addresses Stale List Caches
 * -•Problem with key = "#productId":
 * The @Cacheable methods (getAllProducts, searchByCategory, searchProductByKeyword)
 * cache ProductResponse objects (which contain lists) using keys based on pagination,
 * category, keywords, etc.
 * (e.g., 'all_p' + #pageNumber + ...).
 * Evicting only the entry for a single product ID (key = "#productId") does not remove these cached lists.
 * This means users could see stale lists (e.g., a deleted product still appearing, an updated price not reflected) until the list cache entries expire naturally.•Solution with allEntries = true: By evicting all entries from the PRODUCT_LIST_CACHE (which you've correctly identified as "product"), you guarantee that any potentially stale list is removed whenever a product is added, updated, or deleted.
 * The next request for any product list will be a cache miss, forcing a fetch of fresh data from the database.
 * <p>
 * 2. Simplicity vs. Granularity
 * <p>
 * 3. Correctness
 * <p>
 * Trade-off:
 * - The main trade-off is performance vs. simplicity/correctness
 * <p>
 * This implementation prioritizes data consistency over fine-grained eviction complexity.
 */

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Value("${project.image.path}")
    private String path;

    private final ExponentialBackoffRateLimiter backoffRateLimiter;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final FileService fileService;
    // private final CacheService cacheService; // Removed, using @CacheEvict
    private final UserService userService; // Inject UserService to get current user

    public ProductServiceImpl(ExponentialBackoffRateLimiter backoffRateLimiter,
                              CategoryRepository categoryRepository,
                              ProductRepository productRepository,
                              FileService fileService,
                              UserService userService) {
        this.backoffRateLimiter = backoffRateLimiter;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.fileService = fileService;
        this.userService = userService;
    }

    @Override
    @Transactional
    // Evict all entries from the product list cache when a new product is added.
    @CacheEvict(value = PRODUCT_LIST_CACHE, allEntries = true)
    // Evict relevant list caches if applicable (e.g., all products, category products)
//     @CacheEvict(value = {"allProductsCache", "categoryProductsCache"}, allEntries = true)
    // Use Spring Security's @PreAuthorize for combined check
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_SELLER_MANAGER', 'ROLE_SELLER_STAFF', 'ROLE_SUPER_ADMIN')")
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId)
            throws ResourceNotFoundException, ProductAlreadyExistsException, APIException, ProductDataValidationException {

        // --- Preconditions ---
        Assert.notNull(productDTO, "ProductDTO cannot be null");
        Assert.notNull(categoryId, "Category ID cannot be null");
        validateProductData(productDTO); // Basic DTO validation

        log.debug("Attempting to add product '{}' to category ID {}", productDTO.productName(), categoryId);

        // --- Check Existence ---
        if (productRepository.existsByProductName_NameIgnoreCase(productDTO.productName())) {
            log.warn("Product already exists with name: {}", productDTO.productName());
            throw new ProductAlreadyExistsException(productDTO.productName());
        }

        // --- Fetch Dependencies ---
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", categoryId);
                    return new ResourceNotFoundException("Category", "categoryId", categoryId);
                });

        User currentSeller = userService.getCurrentUser(); // Get the seller

        try {
            // --- Create Value Objects (Leverages validation in constructors) ---
            Product product = getProduct(productDTO, category, currentSeller);

            // --- Save ---
            Product savedProduct = productRepository.save(product);
            log.info("Successfully added product ID: {} with name '{}'", savedProduct.getProductId(), savedProduct.getProductName().getName());

            // --- Map to DTO (Manual) ---
            return mapEntityToDto(savedProduct);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving product: {}", e.getMessage());
            throw new APIException("Error saving product due to data integrity issue: " + e.getMessage(), e);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Catch exceptions from Value Object constructors or Assert checks
            log.error("Validation error creating product: {}", e.getMessage());
            throw new ProductDataValidationException("Invalid product data: " + e.getMessage(), e);
        }
    }

    @NotNull
    private static Product getProduct(ProductDTO productDTO, Category category, User currentSeller) {
        ProductName productName = new ProductName(productDTO.productName());
        ProductDescription productDescription = new ProductDescription(productDTO.productDescription());
        Money productPrice = new Money(productDTO.productPrice());
        // Handle potentially null discount, default to 0? Or require it in DTO? Assuming required for now.
        DiscountPercentage discountPercentage = new DiscountPercentage(Objects.requireNonNullElse(productDTO.productDiscountPercentage(), BigDecimal.ZERO));

        // --- Create Product Entity ---
        Product product = new Product(); // Use no-arg constructor
        product.setProductName(productName);
        product.setProductDescription(productDescription);
        product.setProductPrice(productPrice);
        product.setProductDiscountPercentage(discountPercentage);
        product.setProductQuantity(Objects.requireNonNullElse(productDTO.productQuantity(), 0));
        product.setProductImage("default.png"); // Set default image
        product.setCategory(category);
        product.setSeller(currentSeller); // Set the seller

        // --- Calculate Special Price ---
        product.recalculateSpecialPrice(); // Use the method on the entity
        return product;
    }

    @Override
    // Consider a more specific cache name if needed, e.g., "productsPage"
    @Cacheable(value = "product", key = "'all_p' + #pageNumber + '_' + #pageSize + '_' + #sortBy + '_' + #sortDir")
    // More specific key
    @RateLimiter(name = "defaultRateLimiter")
    @Transactional(readOnly = true)
    public ProductResponse getAllProducts(int pageNumber, int pageSize, String sortBy, String sortDir) {
        log.debug("Fetching all products page: {}, size: {}, sort: {}, dir: {}", pageNumber, pageSize, sortBy, sortDir);
        try {
            return backoffRateLimiter.executeWithBackoff(() -> {
                Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
                Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
                Page<Product> pageProducts = productRepository.findAll(pageDetails);

                List<ProductDTO> productDTOList = pageProducts.getContent().stream()
                        .map(this::mapEntityToDto) // Use manual mapping
                        .collect(Collectors.toList());

                return createProductResponse(pageProducts, productDTOList);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted while getting all products", e);
            throw new RequestInterruptionException("Request interrupted while getting products", e);
        }
    }

    @Override
    // Cache pages of products by category
    // Cache key includes category ID and pagination
    @Cacheable(value = "product", key = "'cat_' + #categoryId + '_p' + #pageNumber + '_' + #pageSize + '_' + #sortBy + '_' + #sortDir")
    @Transactional(readOnly = true)
    public ProductResponse searchByCategory(Long categoryId, int pageNumber, int pageSize, String sortBy, String sortDir)
            throws ResourceNotFoundException, NotFoundException { // Added NotFoundException

        Assert.notNull(categoryId, "Category ID cannot be null");
        log.debug("Searching products by category ID: {}, page: {}, size: {}", categoryId, pageNumber, pageSize);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found for search: {}", categoryId);
                    return new ResourceNotFoundException("Category", "categoryId", categoryId);
                });

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);
        Page<Product> productPage = productRepository.findByCategory(category, pageDetails);

        List<ProductDTO> productDTOList = productPage.getContent().stream()
                .map(this::mapEntityToDto) // Use manual mapping
                .collect(Collectors.toList());

        if (productDTOList.isEmpty() && productPage.getTotalElements() == 0) {
            // Distinguish between category not found (handled above) and category having no products
            log.info("No products found for category ID: {}", categoryId);
            // Throwing NotFoundException as per original logic, we could also return empty response
//            throw new NotFoundException("Category " + category.getCategoryName() + " has no products");
        }

        return createProductResponse(productPage, productDTOList);
    }

    @Override
    // Cache pages of products by keyword
    // Cache key includes keyword and pagination
    @Cacheable(value = "product", key = "'kw_' + #keyword + '_p' + #pageNumber + '_' + #pageSize + '_' + #sortBy + '_' + #sortDir")
    @Transactional(readOnly = true)
    public ProductResponse searchProductByKeyword(String keyword, int pageNumber, int pageSize, String sortBy, String sortDir)
            throws ResourceNotFoundException {

        Assert.hasText(keyword, "Keyword cannot be empty");
        log.debug("Searching products by keyword: '{}', page: {}, size: {}", keyword, pageNumber, pageSize);

        Sort sortByAndDirection = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndDirection);

        Page<Product> productPage = productRepository.findByProductName_NameContainingIgnoreCase(keyword, pageDetails);

        List<ProductDTO> productDTOList = productPage.getContent().stream()
                .map(this::mapEntityToDto) // Use manual mapping
                .collect(Collectors.toList());

        if (productDTOList.isEmpty() && productPage.getTotalElements() == 0) {
            log.info("No products found matching keyword: {}", keyword);
            throw new ResourceNotFoundException(Constants.PRODUCT, "keyword", keyword);
        }

        return createProductResponse(productPage, productDTOList);
    }

    @Override
    @Transactional
    // Evict specific product cache and potentially list caches
//    @CacheEvict(value = "product", key = "#productId")
    @CacheEvict(value = PRODUCT_LIST_CACHE, allEntries = true)
    // @CacheEvict(value = {"allProductsCache", "categoryProductsCache"}, allEntries = true) // Consider broader eviction
    @RateLimiter(name = "defaultRateLimiter")
    // Use Spring Security's @PreAuthorize for combined check
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_SELLER_MANAGER', 'ROLE_SELLER_STAFF', 'ROLE_SUPER_ADMIN') or @productRepository.findById(#productId).orElse(null)?.seller?.userId == authentication.principal.id")
    // Assumes:
    // 1. 'ADMIN' is the role name Spring Security expects (might need 'ROLE_ADMIN').
    // 2. productRepository bean is accessible via '@'.
    // 3. authentication.principal has an 'id' field matching the User ID type (adjust if using UserDetails differently).
    // 4. The User entity linked from Principal has an 'id' field/getter.
    public ProductDTO updateProduct(ProductDTO productDTO, Long productId)
            throws ResourceNotFoundException, ProductDataValidationException {

        // --- Preconditions ---
        Assert.notNull(productDTO, "ProductDTO cannot be null");
        Assert.notNull(productId, "Product ID cannot be null");
        validateProductData(productDTO); // Basic DTO validation

        log.debug("Attempting to update product ID: {}", productId);

        // --- Fetch Existing Entity ---
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found for update: {}", productId);
                    return new ResourceNotFoundException(Constants.PRODUCT, Constants.PRODUCT_ID, productId);
                });

        // --- Authorization Check (Example: Only seller or admin can update?) ---
        // User currentUser = userService.getCurrentUser();
        // if (!currentUser.getUserId().equals(productFromDB.getSeller().getUserId()) && !currentUser.hasRole("ADMIN")) {
        //     log.warn("User {} unauthorized to update product ID {}", currentUser.getUsername(), productId);
        //     throw new UnauthorizedException("User not authorized to update this product");
        // }

        try {
            // --- Create *New* Value Objects from DTO (Leverages validation) ---
            ProductName newProductName = new ProductName(productDTO.productName());
            ProductDescription newProductDescription = new ProductDescription(productDTO.productDescription());
            Money newProductPrice = new Money(productDTO.productPrice());
            DiscountPercentage newDiscountPercentage = new DiscountPercentage(Objects.requireNonNullElse(productDTO.productDiscountPercentage(), java.math.BigDecimal.ZERO));

            // --- Check if Name Changed and Conflicts ---
            if (!productFromDB.getProductName().equals(newProductName) &&
                    productRepository.existsByProductName_NameIgnoreCase(newProductName.getName())) {
                log.warn("Update failed: New product name '{}' already exists.", newProductName.getName());
                throw new ProductAlreadyExistsException(newProductName.getName());
            }

            // --- Update Entity Fields with *New* Value Objects ---
            productFromDB.setProductName(newProductName);
            productFromDB.setProductDescription(newProductDescription);
            productFromDB.setProductPrice(newProductPrice);
            productFromDB.setProductDiscountPercentage(newDiscountPercentage);
            productFromDB.setProductQuantity(Objects.requireNonNullElse(productDTO.productQuantity(), 0));
            // Note: Category and Seller are generally not updated here unless specifically intended

            // --- Recalculate Special Price ---
            productFromDB.recalculateSpecialPrice();

            // --- Save ---
            Product updatedProduct = productRepository.save(productFromDB);
            log.info("Successfully updated product ID: {}", updatedProduct.getProductId());

            // --- Map to DTO (Manual) ---
            return mapEntityToDto(updatedProduct);

        } catch (IllegalArgumentException | NullPointerException e) {
            // Catch exceptions from Value Object constructors
            log.error("Validation error updating product ID {}: {}", productId, e.getMessage());
            throw new ProductDataValidationException("Invalid product data for update: " + e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating product ID {}: {}", productId, e.getMessage());
            throw new APIException("Error updating product due to data integrity issue: " + e.getMessage(), e);
        }
    }

    // Removed updateProductPrice method as the repository method was removed.
    // Price updates should go through the main updateProduct method.
    /*
    @Transactional
    public void updateProductPrice(String productId, BigDecimal newPrice) {
        // This logic is flawed due to removed repository method and incorrect cache key
        // productRepository.updatePrice(productId, newPrice);
        // cacheService.invalidate("productCache", "product:" + productId);

        // Correct approach (if a dedicated method is needed):
        Long prodId = Long.parseLong(productId); // Convert String ID
        Product product = productRepository.findById(prodId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.PRODUCT, Constants.PRODUCT_ID, prodId));
        product.setProductPrice(new Money(newPrice));
        product.recalculateSpecialPrice();
        productRepository.save(product);
        // Invalidate using @CacheEvict on the method instead
    }
    */

    @Override
    @Transactional
    @RateLimiter(name = "defaultRateLimiter")
    @CacheEvict(value = PRODUCT_LIST_CACHE, allEntries = true)
//    @CacheEvict(value = "product", key = "#productId")
    // Use Spring Security's @PreAuthorize for combined check
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_SELLER_MANAGER', 'ROLE_SELLER_STAFF', 'ROLE_SUPER_ADMIN') or @productRepository.findById(#productId).orElse(null)?.seller?.userId == authentication.principal.id")
    // Assumes:
    // 1. 'ADMIN' is the role name Spring Security expects (might need 'ROLE_ADMIN').
    // 2. productRepository bean is accessible via '@'.
    // 3. authentication.principal has an 'id' field matching the User ID type (adjust if using UserDetails differently).
    // 4. The User entity linked from Principal has an 'id' field/getter.
    public ProductDTO deleteProduct(Long productId) throws ResourceNotFoundException { // No UnauthorizedException needed here, PreAuthorize handles it
        Assert.notNull(productId, "Product ID cannot be null");
        log.debug("Attempting to delete product ID: {}", productId);

        // No manual authorization check needed here - @PreAuthorize handles it BEFORE the method runs

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found for deletion: {}", productId);
                    return new ResourceNotFoundException(Constants.PRODUCT, Constants.PRODUCT_ID, productId);
                });

        ProductDTO deletedProductDTO = mapEntityToDto(product); // Map before deleting

        productRepository.delete(product); // Performs soft delete via BaseEntity/Hibernate listener
        log.info("Successfully soft-deleted product ID: {}", productId);

        return deletedProductDTO;
    }

    @Override
    @Transactional
    // Evict specific product cache and potentially list caches
//    @CacheEvict(value = "product", key = "#productId")
    @CacheEvict(value = PRODUCT_LIST_CACHE, allEntries = true)
    // @CacheEvict(value = {"allProductsCache", "categoryProductsCache"}, allEntries = true) // Consider broader eviction
    // Use Spring Security's @PreAuthorize for combined check
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_SELLER_MANAGER', 'ROLE_SELLER_STAFF', 'ROLE_SUPER_ADMIN') or @productRepository.findById(#productId).orElse(null)?.seller?.userId == authentication.principal.id")
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException, ResourceNotFoundException {
        Assert.notNull(productId, "Product ID cannot be null");
        Assert.notNull(image, "Image file cannot be null");
        Assert.isTrue(!image.isEmpty(), "Image file cannot be empty");

        log.debug("Attempting to update image for product ID: {}", productId);

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found for image update: {}", productId);
                    return new ResourceNotFoundException(Constants.PRODUCT, Constants.PRODUCT_ID, productId);
                });

        // --- Authorization Check (Example) ---
        // User currentUser = userService.getCurrentUser();
        // if (!currentUser.getUserId().equals(productFromDB.getSeller().getUserId()) && !currentUser.hasRole("ADMIN")) {
        //     log.warn("User {} unauthorized to update image for product ID {}", currentUser.getUsername(), productId);
        //     throw new UnauthorizedException("User not authorized to update image for this product");
        // }

        // --- Delete Old Image (Optional but recommended) ---
        String oldImageName = productFromDB.getProductImage(); // Get the old GENERATED name
        if (oldImageName != null && !oldImageName.equals("default.png")) {
            try {
                fileService.deleteImage(path, oldImageName);
                log.debug("Deleted old image '{}' for product ID {}", oldImageName, productId);
            } catch (IOException e) {
                // Log the error but proceed with uploading the new image
                log.error("Failed to delete old image '{}' for product ID {}: {}", oldImageName, productId, e.getMessage());
            }
        }

        // --- Upload New Image ---
        FileUploadResult uploadResult = fileService.uploadImage(path, image); // GET RESULT OBJECT
        String generatedFileName = uploadResult.generatedFilename();
        String originalFileName = uploadResult.originalFilename();
        log.debug("Uploaded new image '{}' (original: '{}') for product ID {}", generatedFileName, originalFileName, productId);

        // --- Update Entity ---
        productFromDB.setProductImage(generatedFileName);         // Store the GENERATED name
        productFromDB.setOriginalImageFilename(originalFileName); // Store the ORIGINAL name
        Product updatedProduct = productRepository.save(productFromDB);
        log.info("Successfully updated image for product ID: {}", updatedProduct.getProductId());

        // --- Map to DTO (Manual) ---
        return mapEntityToDto(updatedProduct);
    }

    // Removed isValidProduct method as it's no longer in the interface
    /*
    @Override
    public boolean isValidProduct(Product product) {
        // This logic is too basic and was removed from the interface.
        // Validation happens via DTO constraints and Value Object constructors.
        return product != null && product.getProductName() != null && product.getProductName().getName() != null && !product.getProductName().getName().isEmpty();
    }
    */

    // --- Helper Methods ---

    /**
     * Performs basic validation on the incoming ProductDTO.
     * More complex validation is handled by Value Object constructors.
     */
    private void validateProductData(ProductDTO productDTO) throws ProductDataValidationException {
        // These checks are somewhat redundant if VOs are used correctly, but act as early fail

        // Use record accessors (fieldName() instead of getFieldName())
        if (productDTO.productName() == null || productDTO.productName().trim().isEmpty()) {
            throw new ProductDataValidationException("Product name is required");
        }
        if (productDTO.productDescription() == null || productDTO.productDescription().trim().isEmpty()) {
            throw new ProductDataValidationException("Product description is required");
        }
        if (productDTO.productPrice() == null) {
            throw new ProductDataValidationException("Product price is required");
        }
        // Quantity and Discount are handled by Objects.requireNonNullElse or VO constructors
    }

    // Removed calculateSpecialPrice as logic moved to Product.recalculateSpecialPrice()
    /*
    private void calculateSpecialPrice(Product product) { ... }
    */

    /**
     * Manually maps a Product entity to a ProductDTO.
     * Handles extraction of values from Value Objects.
     */
    private ProductDTO mapEntityToDto(Product product) {
        if (product == null) {
            return null;
        }

        // Extract values safely
        Long productId = product.getProductId();
        String productName = (product.getProductName() != null) ? product.getProductName().getName() : null;
        String productImage = product.getProductImage(); // Generated filename
        String originalImageFilename = product.getOriginalImageFilename(); // Original filename
        String productDescription = (product.getProductDescription() != null) ? product.getProductDescription().getDescription() : null;
        Integer productQuantity = product.getProductQuantity();
        BigDecimal productPrice = (product.getProductPrice() != null) ? product.getProductPrice().getAmount() : null;
        BigDecimal discountPercentage = (product.getProductDiscountPercentage() != null) ? product.getProductDiscountPercentage().getPercentage() : null;
        BigDecimal specialPrice = (product.getProductSpecialPrice() != null) ? product.getProductSpecialPrice().getAmount() : null;
        Long categoryId = (product.getCategory() != null) ? product.getCategory().getCategoryId() : null;
        String categoryName = (product.getCategory() != null && product.getCategory().getCategoryName() != null)
                ? product.getCategory().getCategoryName().getName() // Call .getName() on the CategoryName object
                : null;
        Long sellerId = (product.getSeller() != null) ? product.getSeller().getUserId() : null;
        String sellerName = (product.getSeller() != null) ? product.getSeller().getUsername() : null;

        // Use the record's canonical constructor
        return new ProductDTO(
                productId,
                productName,
                productImage,
                originalImageFilename, // ADDED
                productDescription,
                productQuantity,
                productPrice,
                discountPercentage,
                specialPrice,
                categoryId,
                categoryName,
                sellerId,
                sellerName
        );
    }

    /**
     * Helper to create ProductResponse from Page and DTO list.
     */
    private ProductResponse createProductResponse(Page<Product> page, List<ProductDTO> dtoList) {
        // Use the record's canonical constructor
        return new ProductResponse(
                dtoList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.config.InputValidator;
import me.amlu.shop.amlume_shop.config.RoleHierarchyValidator;
import me.amlu.shop.amlume_shop.config.SecureAppRoleValidator;
import me.amlu.shop.amlume_shop.config.SecurityValidator;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.user_management.AppRole;
import me.amlu.shop.amlume_shop.order_management.Order;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.product_management.ProductService;
import me.amlu.shop.amlume_shop.resilience.service.ResilienceService;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "roles")
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;
    private final ProductService productService;

    private final AlertService alertService;
    private final SecurityValidator securityValidator;
    private final InputValidator inputValidator;
    private final RoleHierarchyValidator roleHierarchyValidator;
    private final SecurityAuditService securityAuditService;
    private final ResilienceService resilienceService;

    public RoleServiceImpl(UserRepository userRepository, ProductService productService, AlertService alertService, SecurityValidator securityValidator, InputValidator inputValidator, RoleHierarchyValidator roleHierarchyValidator, ResilienceService resilienceService, SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.productService = productService;
        this.alertService = alertService;
        this.securityValidator = securityValidator;
        this.inputValidator = inputValidator;
        this.roleHierarchyValidator = roleHierarchyValidator;
        this.resilienceService = resilienceService;
        this.securityAuditService = securityAuditService;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#username + '_' + #resource.hashCode()")
    public Set<UserRole> getDynamicRolesForResource(Object resource) {
        Set<UserRole> dynamicRoles = new HashSet<>();

        String userId = null;
        try {
            // --- Get current user ---
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : null;
            userId = String.valueOf(userRepository.findByUsername(username).map(User::getUserId).orElse(null));

            if (!securityValidator.validateAuthentication(authentication)) {
                securityAuditService.logFailedAttempt(username, "Authentication validation failed");
                return Collections.emptySet();
            }

            // --- Input validation ---
            if (!inputValidator.validateResource(resource)) {
                securityAuditService.logFailedAttempt(username, "Resource validation failed");
                return Collections.emptySet();
            }

            log.debug("Determining dynamic roles for user {} and resource type: {}",
                    username, resource.getClass().getSimpleName());

            // --- Rate Limiting Check ---
            try {
                // Call the method that throws on exceedance
                resilienceService.allowRequestByUsername(username); // Use the correct method
                log.trace("Rate limit check passed for user {}", username);
            } catch (RateLimitExceededException e) {
                // Catch the exception if the limit is exceeded
                log.warn("Rate limit exceeded for user {} while determining dynamic roles.", username);
                securityAuditService.logFailedAttempt(username, "Rate limit exceeded");
                return Collections.emptySet(); // Return empty set if rate limited
            } catch (Exception e) { // Catch potential Redis errors during rate limiting
                log.error("Error during rate limit check for user {}: {}", username, e.getMessage());
                // Decide how to handle Redis errors - fail open (allow) or fail closed (deny)?
                // For security, failing closed might be better here.
                securityAuditService.logFailedAttempt(username, "Rate limit check error");
                return Collections.emptySet();
            }

            // --- Role Determination Logic (proceed if not rate-limited) ---
            // Role hierarchy validation
            Set<UserRole> roles = determineRoles(resource); // This line was moved down
            if (roleHierarchyValidator.isRoleAssignmentInvalid(roles, authentication)) {
                securityAuditService.logFailedAttempt(username, "Role validation failed");
                return Collections.emptySet();
            }
//            validatAppRoleHierarchy(dynamicRoles);

            // --- Resource-specific role determination ---
            switch (resource) {
                case Product product -> addProductRoles(product, dynamicRoles, username);
                case Order order -> addOrderRoles(order, dynamicRoles, username);

                // Add more resource types as needed
                case Category category ->
                    // Category manager role
//                if (isCategoryManager(username, category.getCategoryManager().getUserId())) {
//                    dynamicRoles.add("ROLE_CATEGORY_MANAGER");
//                }
                        addCategoryRoles(category, dynamicRoles, username);
                default -> {
                    log.error("Unsupported resource type: {}", resource.getClass().getSimpleName());
                    throw new IllegalStateException("Unexpected value: " + resource);
                }
            }

            // --- Audit logging ---
            securityAuditService.logRoleAssignment(username, dynamicRoles, resource);

            log.debug("Assigned roles {} to user {} for resource {}",
                    dynamicRoles.stream().map(UserRole::getRoleName).collect(Collectors.toSet()),
                    username,
                    resource.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Failed to determine dynamic roles for resource", e);
            alertService.alertSecurityTeam((new SecurityAlert(
                    userId,
                    "Failed to determine dynamic roles for resource",
                    Map.of(
                            "resourceType", resource.getClass().getSimpleName(),
                            "resourceId", resource.hashCode()
                    )
            )));
            return Collections.emptySet(); // Fail secure
        }

        return Collections.unmodifiableSet(dynamicRoles); // Immutable return
    }

    @CacheEvict(allEntries = true)
    public void clearAllRoles() {
        // method to clear all cached roles
        // The @CacheEvict annotation handles the cache clearing
        log.info("Clearing all role caches");
        // Can add audit logging or additional cleanup if needed
        securityAuditService.logCacheCleared("All roles cache cleared");
    }

    @CacheEvict(key = "#username + '_' + #resource.hashCode()")
    public void clearUserRoles(String username, Object resource) {
        // method to clear specific user's cached roles
        // The @CacheEvict annotation handles the specific cache entry clearing
        log.info("Clearing role cache for user: {} and resource: {}",
                username, resource.getClass().getSimpleName());
        // Can add audit logging or additional cleanup if needed
        securityAuditService.logCacheCleared(
                String.format("Role cache cleared for user %s and resource %s",
                        username, resource.getClass().getSimpleName())
        );
    }

    public boolean assignRoles(User user, Set<UserRole> newRoles) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Validate role assignment
            if (roleHierarchyValidator.isRoleAssignmentInvalid(newRoles, authentication)) {
                log.warn("Role assignment validation failed for user: {}", user.getUsername());
                return false;
            }

            // If validation passes, proceed with role assignment
            user.setRoles(newRoles);
            return true;

        } catch (Exception e) {
            log.error("Error assigning roles to user: {}", user.getUsername(), e);
            return false;
        }
    }

    // TOCHECK and finish
    private Set<UserRole> determineRoles(Object resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }

        return switch (resource) {
            case Product p -> determineProductRoles(p);
            case Order o -> determineOrderRoles(o);
            case Category c -> determineCategoryRoles(c);
            default -> throw new UnsupportedOperationException(
                    "Unsupported resource type: " + resource.getClass().getSimpleName()
            );
        };
    }

    private Set<UserRole> determineProductRoles(Product product) {
        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(AppRole.ROLE_PRODUCT_EDITOR));
        roles.add(new UserRole(AppRole.ROLE_PRODUCT_VIEWER));

        if (product.isHighValue()) {
            roles.add(new UserRole(AppRole.ROLE_PREMIUM_PRODUCT_MANAGER));
        }

        if (product.isRestricted()) {
            roles.add(new UserRole(AppRole.ROLE_RESTRICTED_PRODUCT_HANDLER));
        }

        return roles;
    }

    private Set<UserRole> determineOrderRoles(Order order) {
        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(AppRole.ROLE_ORDER_OWNER));
        roles.add(new UserRole(AppRole.ROLE_ORDER_VIEWER));

        switch (order.getOrderStatus()) {
            case PENDING_APPROVAL -> roles.add(new UserRole(AppRole.ROLE_ORDER_APPROVER));
            case SHIPPING -> roles.add(new UserRole(AppRole.ROLE_SHIPPING_MANAGER));
            case DISPUTED -> roles.add(new UserRole(AppRole.ROLE_DISPUTE_HANDLER));
        }

        return roles;
    }

    private Set<UserRole> determineCategoryRoles(Category category) {
        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(AppRole.ROLE_CATEGORY_MANAGER));
        roles.add(new UserRole(AppRole.ROLE_CATEGORY_VIEWER));

        if (category.isMainCategory()) {
            roles.add(new UserRole(AppRole.ROLE_MAIN_CATEGORY_ADMIN));
        }

        if (category.hasSpecialRestrictions()) {
            roles.add(new UserRole(AppRole.ROLE_RESTRICTED_CATEGORY_HANDLER));
        }

        return roles;
    }

    private void addProductRoles(Product product, Set<UserRole> roles, String username) {
        try {

            // Input sanitization
            SecureAppRoleValidator validator = new SecureAppRoleValidator();
            String sanitizedUsername = validator.sanitizeAppRole(username);
//            String sanitizedUsername = sanitizeInput(username);

            // Depth check for nested objects
            if (inputValidator.validateResource(product)) {
                log.warn("Security check failed: Max depth exceeded");
                return;
            }

//            // Business logic validation
//            if (!productService.isValidProduct(product)) {
//                log.warn("Security check failed: Invalid product state");
//                return;
//            }

            // Role assignment with proper checks

            // Product manager for specific category
            // Category Manager role
            if (isCategoryManager(username, product.getCategoryManager().getUserId())) {
                roles.add(new UserRole(AppRole.ROLE_CATEGORY_MANAGER));
                roles.add(new UserRole(AppRole.ROLE_PRODUCT_EDITOR));
            }

            // Seller role
            if (isAuthorizedSeller(sanitizedUsername, product)) {
                roles.add(new UserRole(AppRole.ROLE_SELLER));
            }

//            // Department Manager role
//            if (isDepartmentManager(username, product.getDepartment())) {
//                roles.add("ROLE_DEPARTMENT_MANAGER");
//            }

//            // Product editor for specific department
//            if (isUserInDepartment(username, product.getDepartment())) {
//                roles.add("ROLE_PRODUCT_EDITOR");
//            }
//
//            // Regional product viewer
//            if (isUserInRegion(username, product.getProductRegion())) {
//                roles.add("ROLE_PRODUCT_VIEWER");
//            }
        } catch (Exception e) {
            String userId = String.valueOf(product.getSeller().getUserId());
            log.error("Error adding product roles for user ID: {} and product ID: {}",
                    userId, product.getProductId(), e);
            throw new SecurityException("Role assignment failed", e);
        }
    }

    private void addOrderRoles(Order order, Set<UserRole> roles, String username) {
        // Order owner
        if (order.getCustomerName().equals(username)) {
            roles.add(new UserRole(AppRole.ROLE_ORDER_OWNER));
            String userId = order.getCustomerId();
            log.info("User {} with id {} is the owner of order {}", username, userId, order.getOrderId());
        }

//        // Order processor
//        if (isOrderProcessor(username, order.getDepartment())) {
//            roles.add("ROLE_ORDER_PROCESSOR");
//        }
//
//        // Regional order manager
//        if (isRegionalManager(username, order.getRegion())) {
//            roles.add("ROLE_ORDER_MANAGER");
//        }
    }

    private void addCategoryRoles(Category category, Set<UserRole> roles, String username) {
        // Category manager
        if (isCategoryManager(username, category.getCategoryManager().getUserId())) {
            roles.add(new UserRole(AppRole.ROLE_CATEGORY_MANAGER));
        }

//        // Department manager
//        if (isDepartmentManager(username, category.getDepartment())) {
//            roles.add("ROLE_DEPARTMENT_MANAGER");
//        }
//
//        // Regional category manager
//        if (isRegionalManager(username, category.getRegion())) {
//            roles.add("ROLE_CATEGORY_MANAGER");
//        }
    }


//    // Helper methods for security checks
//
//    // Role hierarchy validation
//    private void validatAppRoleHierarchy(Set<UserRole> roles) {
//        RoleHierarchy hierarchy = new RoleHierarchyImpl();
//        // Implement role hierarchy validation
//    }
//
//    // Rate limiting
//    @Cacheable(value = "roleCheckRateLimit", key = "#username")
//    private boolean isRateLimitExceeded(String username) {
//
//        return false;
//    }
//
//    // Audit logging
//    private void auditLog(String username, Set<UserRole> dynamicRoles, Object resource) {
//        securityAuditService.logRoleAssignment(username, dynamicRoles, resource);
//    }
//
//    // Security alerting
//    private void alertSecurityTeam(Exception e) {
//        // Implement security alerting
//    }

    // Helper methods for role determination

    public boolean canManageProduct(User user, Product product) {
        Set<UserRole> userRoles = user.getRoles();

        return userRoles.stream()
                .map(UserRole::getRoleName)
                .anyMatch(role ->
                        roleHierarchyValidator.hasAuthorityOver(role, AppRole.ROLE_PRODUCT_EDITOR) ||
                                (Objects.equals(role, AppRole.ROLE_SELLER) && product.getSeller().getUserId().equals(user.getUserId()))
                );
    }

    private boolean isCategoryManager(String username, Long categoryManagerId) {
        try {
            Optional<User> user = userRepository.findByUsername(username);
            return user.isPresent() && user.get().getUserId().equals(categoryManagerId);
        } catch (Exception e) {
            log.error("Error checking category manager status", e);
            return false;
        }
    }

    private boolean isProductSeller(String username, User seller) {
        try {
            return seller != null && seller.getUsername().equals(username);
        } catch (Exception e) {
            log.error("Error checking seller status", e);
            return false;
        }
    }

    private boolean isAuthorizedSeller(String username, Product product) {
        try {
            User seller = product.getSeller();
            return seller != null && seller.getUsername().equals(username);
        } catch (Exception e) {
            log.error("Error checking seller status", e);
            return false;
        }
    }

//    private boolean isOrderProcessor(String username, String department) {
//        try {
//            Optional<User> user = userRepository.findByUsername(username);
//            return user != null &&
//                    user.getDepartment().equals(department) &&
//                    user.hasRole("ORDER_PROCESSOR");
//        } catch (Exception e) {
//            log.error("Error checking order processor status", e);
//            return false;
//        }
//    }

//    private boolean isUserInDepartment(String username, String department) {
//        try {
//            Optional<User> user = userRepository.findByUsername(username);
//            return user != null && user.getDepartment().equals(department);
//        } catch (Exception e) {
//            log.error("Error checking user department", e);
//            return false;
//        }
//    }

//    private boolean isUserInRegion(String username, String region) {
//        try {
//            Optional<User> user = userRepository.findByUsername(username);
//            return user != null && user.getRegion().equals(region);
//        } catch (Exception e) {
//            log.error("Error checking user productRegion", e);
//            return false;
//        }
//    }

//    private boolean isDepartmentManager(String username, String department) {
//        try {
//            Optional<User> user = userRepository.findByUsername(username);
//            return user.isPresent() &&
//                    user.getDepartment().equals(department) &&
//                    user.hasRole("DEPARTMENT_MANAGER");
//        } catch (Exception e) {
//            log.error("Error checking department manager status", e);
//            return false;
//        }
//    }

//    private boolean isRegionalManager(String username, String region) {
//        try {
//            Optional<User> user = userRepository.findByUsername(username);
//            return user != null &&
//                    user.getRegion().equals(region) &&
//                    user.hasRole("REGIONAL_MANAGER");
//        } catch (Exception e) {
//            log.error("Error checking regional manager status", e);
//            return false;
//        }
//    }

}

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

import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.config.InputValidator;
import me.amlu.shop.amlume_shop.config.RoleHierarchyValidator;
import me.amlu.shop.amlume_shop.config.SecurityValidator;
import me.amlu.shop.amlume_shop.exceptions.RoleAssignmentException;
import me.amlu.shop.amlume_shop.exceptions.SecurityValidationException;
import me.amlu.shop.amlume_shop.exceptions.UserNotFoundException;
import me.amlu.shop.amlume_shop.order_management.Order;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import me.amlu.shop.amlume_shop.security.enums.AlertSeverityEnum;
import me.amlu.shop.amlume_shop.security.model.SecurityAlert;
import me.amlu.shop.amlume_shop.user_management.AppRole;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserRole;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = Constants.ROLES_CACHE)
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final UserRepository userRepository;
    private final AlertService alertService;
    private final SecurityValidator securityValidator;
    private final InputValidator inputValidator;
    private final RoleHierarchyValidator roleHierarchyValidator;
    private final SecurityAuditService securityAuditService;
    private final RateLimiter rateLimiter;
    // private final SecureAppRoleValidator secureAppRoleValidator; // Removed if not used

    public RoleServiceImpl(UserRepository userRepository,
                           AlertService alertService,
                           SecurityValidator securityValidator,
                           InputValidator inputValidator,
                           RoleHierarchyValidator roleHierarchyValidator,
                           SecurityAuditService securityAuditService,
                           @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter
            /*, SecureAppRoleValidator secureAppRoleValidator */) {
        this.userRepository = userRepository;
        this.alertService = alertService;
        this.securityValidator = securityValidator;
        this.inputValidator = inputValidator;
        this.roleHierarchyValidator = roleHierarchyValidator;
        this.securityAuditService = securityAuditService;
        this.rateLimiter = rateLimiter;
        // this.secureAppRoleValidator = secureAppRoleValidator;
    }

    /**
     * Determines the dynamic roles for a user based on a given resource.
     * Applies security checks, rate limiting, and validation before determining roles.
     * Results are cached based on the user's authentication name and the resource's hashcode.
     *
     * @param resource The resource (e.g., Product, Order, Category) to determine roles for.
     * @return An unmodifiable set of UserRole objects applicable to the user for the resource. Returns an empty set if validation fails or an error occurs.
     */
    @Override
    @Transactional(readOnly = true)
    // Cache key uses authentication name and resource hashcode.
    // Note: Relies on SecurityContextHolder, making testing slightly harder.
    // Ensure resource.hashCode() is stable and meaningful.
    @Cacheable(key = "authentication.name + '_' + #resource.hashCode()")
    public Set<UserRole> getDynamicRolesForResource(@NotNull Object resource) {
        Assert.notNull(resource, "Resource cannot be null for role determination.");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous"; // Handle anonymous case
        User user = null;
        String userIdForAlert = "unknown";

        try {
            // 1. Initial Security Validations (Auth, Rate Limit, Input)
            user = performInitialValidations(authentication, username, resource);
            userIdForAlert = String.valueOf(user.getUserId()); // Get user ID for potential alerts

            // 2. Determine Roles based on Resource Type
            Set<UserRole> dynamicRoles = determineAndValidateRoles(resource, user, authentication);

            // 3. Audit Success
            securityAuditService.logRoleAssignment(username, dynamicRoles, resource);
            log.debug("Assigned roles {} to user {} for resource {}",
                    dynamicRoles.stream().map(UserRole::getRoleName).collect(Collectors.toSet()),
                    username,
                    resource.getClass().getSimpleName());

            return Collections.unmodifiableSet(dynamicRoles); // Return immutable set

        } catch (SecurityValidationException e) {
            // Handled specific security validation failures (logged within performInitialValidations)
            return Collections.emptySet(); // Fail secure
        } catch (Exception e) {
            // Catch unexpected errors during role determination or validation
            log.error("Failed to determine dynamic roles for user {} and resource {}: {}",
                    username, resource.getClass().getSimpleName(), e.getMessage(), e);
            alertService.alertSecurityTeam(new SecurityAlert(
                    userIdForAlert, // Use fetched userId if available
                    "Failed to determine dynamic roles for resource",
                    Map.of(
                            "resourceType", resource.getClass().getSimpleName(),
                            "resourceId", resource.hashCode(), // Use hashcode as identifier
                            "error", e.getMessage()
                    ), AlertSeverityEnum.HIGH, Instant.now(), "production")
            );
            return Collections.emptySet(); // Fail secure
        }
    }

    /**
     * Performs initial security checks: authentication, rate limiting, and input validation.
     * Fetches the User object.
     *
     * @param authentication The current authentication object.
     * @param username       The username from authentication.
     * @param resource       The resource being accessed.
     * @return The validated User object.
     * @throws SecurityValidationException if any validation fails.
     * @throws UserNotFoundException       if the user cannot be found.
     */
    private User performInitialValidations(Authentication authentication, String username, Object resource)
            throws SecurityValidationException, UserNotFoundException {

        // --- Authentication Check ---
        if (!securityValidator.validateAuthentication(authentication)) {
            log.warn("Authentication validation failed for user '{}'", username);
            securityAuditService.logFailedAttempt(username, "Authentication validation failed");
            throw new SecurityValidationException("Authentication validation failed");
        }

        // --- Fetch User ---
        // Fetch user early to use User object instead of username string
        User user = userRepository.findByAuthenticationInfoUsername_Username(username)
                .orElseThrow(() -> {
                    log.error("User not found in repository: {}", username);
                    securityAuditService.logFailedAttempt(username, "User not found");
                    return new UserNotFoundException("User not found: " + username);
                });

        // --- Rate Limiting Check ---
        try {
            if (!rateLimiter.tryAcquire(username)) { // Use tryAcquire which returns boolean
                log.warn("Rate limit exceeded for user {} while determining dynamic roles.", username);
                securityAuditService.logFailedAttempt(username, "Rate limit exceeded");
                throw new SecurityValidationException("Rate limit exceeded");
            }
            log.trace("Rate limit check passed for user {}", username);
        } catch (Exception e) { // Catch potential Redis errors
            log.error("Error during rate limit check for user {}: {}", username, e.getMessage());
            securityAuditService.logFailedAttempt(username, "Rate limit check error");
            // Fail closed for security
            throw new SecurityValidationException("Rate limit check error", e);
        }

        // --- Input Validation ---
        if (!inputValidator.validateResource(resource)) {
            log.warn("Resource validation failed for resource type: {}", resource.getClass().getSimpleName());
            securityAuditService.logFailedAttempt(username, "Resource validation failed");
            throw new SecurityValidationException("Resource validation failed");
        }

        return user;
    }

    /**
     * Determines roles based on resource type and validates them against hierarchy.
     *
     * @param resource       The resource object.
     * @param user           The authenticated user.
     * @param authentication The authentication object.
     * @return A mutable set of determined roles.
     * @throws SecurityValidationException if role hierarchy validation fails.
     * @throws IllegalStateException       if the resource type is unsupported.
     */
    private Set<UserRole> determineAndValidateRoles(Object resource, User user, Authentication authentication)
            throws SecurityValidationException, IllegalStateException {

        Set<UserRole> dynamicRoles = new HashSet<>();
        log.debug("Determining dynamic roles for user {} and resource type: {}",
                user.getUsername(), resource.getClass().getSimpleName());

        // --- Resource-specific role determination ---
        switch (resource) {
            case Product product -> addProductRoles(product, dynamicRoles, user);
            case Order order -> addOrderRoles(order, dynamicRoles, user);
            case Category category -> addCategoryRoles(category, dynamicRoles, user);
            // Add more resource types as needed
            default -> {
                log.error("Unsupported resource type for role determination: {}", resource.getClass().getSimpleName());
                // Throw specific exception or handle as appropriate
                throw new IllegalStateException("Unsupported resource type: " + resource.getClass().getSimpleName());
            }
        }

        // --- Role Hierarchy Validation ---
        if (roleHierarchyValidator.isRoleAssignmentInvalid(dynamicRoles, authentication)) {
            log.warn("Role hierarchy validation failed for user {} with roles {}", user.getUsername(), dynamicRoles);
            securityAuditService.logFailedAttempt(user.getUsername(), "Role hierarchy validation failed");
            throw new SecurityValidationException("Role hierarchy validation failed");
        }

        return dynamicRoles;
    }

    // --- Role Determination Helpers ---

    private void addProductRoles(Product product, Set<UserRole> roles, User user) {
        // Basic viewer/editor roles (adjust based on requirements)
        roles.add(new UserRole(AppRole.ROLE_PRODUCT_VIEWER));
        // roles.add(new UserRole(AppRole.ROLE_PRODUCT_EDITOR)); // Maybe conditional?

        // Category Manager role for the product's category
        if (isCategoryManager(user, product.getCategory())) {
            roles.add(new UserRole(AppRole.ROLE_CATEGORY_MANAGER));
            roles.add(new UserRole(AppRole.ROLE_PRODUCT_EDITOR)); // Category manager can edit products in their category
        }

        // Seller role if the user is the seller of this product
        if (isAuthorizedSeller(user, product)) {
            roles.add(new UserRole(AppRole.ROLE_SELLER));
            roles.add(new UserRole(AppRole.ROLE_PRODUCT_EDITOR)); // Seller can edit their own products
        }

        // Premium product manager role
        if (product.isHighValue()) {
            // Assuming a specific role grants this, check if user has it
            // if (user.hasRole(AppRole.ROLE_PREMIUM_PRODUCT_MANAGER)) { // Check existing static roles if needed
            roles.add(new UserRole(AppRole.ROLE_PREMIUM_PRODUCT_MANAGER));
            // }
        }

        // Restricted product handler role
        if (product.isRestricted()) {
            // Assuming a specific role grants this
            // if (user.hasRole(AppRole.ROLE_RESTRICTED_PRODUCT_HANDLER)) {
            roles.add(new UserRole(AppRole.ROLE_RESTRICTED_PRODUCT_HANDLER));
            // }
        }
    }

    private void addOrderRoles(Order order, Set<UserRole> roles, User user) {
        // Basic viewer role
        roles.add(new UserRole(AppRole.ROLE_ORDER_VIEWER));

        // Order owner role
        // Compare user IDs for reliability
        if (order.getCustomerId() != null && order.getCustomerId().equals(String.valueOf(user.getUserId()))) {
            roles.add(new UserRole(AppRole.ROLE_ORDER_OWNER));
            log.info("User {} is the owner of order {}", user.getUsername(), order.getOrderId());
        }

        // Roles based on order status
        switch (order.getOrderStatus()) {
            case PENDING_APPROVAL -> roles.add(new UserRole(AppRole.ROLE_ORDER_APPROVER));
            case SHIPPING -> roles.add(new UserRole(AppRole.ROLE_SHIPPING_MANAGER));
            case DISPUTED -> roles.add(new UserRole(AppRole.ROLE_DISPUTE_HANDLER));
            // Add other statuses if needed
            default -> log.trace("No specific dynamic role for order status: {}", order.getOrderStatus());
        }
    }

    private void addCategoryRoles(Category category, Set<UserRole> roles, User user) {
        // Basic viewer role
        roles.add(new UserRole(AppRole.ROLE_CATEGORY_VIEWER));

        // Category manager role
        if (isCategoryManager(user, category)) {
            roles.add(new UserRole(AppRole.ROLE_CATEGORY_MANAGER));
        }

        // Main category admin role
        if (category.isMainCategory()) {
            // Assuming a specific role grants this
            // if (user.hasRole(AppRole.ROLE_MAIN_CATEGORY_ADMIN)) {
            roles.add(new UserRole(AppRole.ROLE_MAIN_CATEGORY_ADMIN));
            // }
        }

        // Restricted category handler role
        if (category.hasSpecialRestrictions()) {
            // Assuming a specific role grants this
            // if (user.hasRole(AppRole.ROLE_RESTRICTED_CATEGORY_HANDLER)) {
            roles.add(new UserRole(AppRole.ROLE_RESTRICTED_CATEGORY_HANDLER));
            // }
        }
    }

    // --- Role Check Helpers (using User objects) ---

    private boolean isCategoryManager(User user, Category category) {
        if (user == null || category == null || category.getCategoryManager() == null) {
            return false;
        }
        // Compare user IDs
        return user.getUserId().equals(category.getCategoryManager().getUserId());
    }

    private boolean isAuthorizedSeller(User user, Product product) {
        if (user == null || product == null || product.getSeller() == null) {
            return false;
        }
        // Compare user IDs
        return user.getUserId().equals(product.getSeller().getUserId());
    }

    // --- Cache Management ---

    @CacheEvict(allEntries = true)
    @Override
    public void clearAllRoles() {
        log.info("Clearing all role caches via annotation.");
        securityAuditService.logCacheCleared("All roles cache cleared by clearAllRoles()");
    }

    @CacheEvict(key = "#username + '_' + #resource.hashCode()")
    @Override
    public void clearUserRoles(String username, @NotNull Object resource) {
        Assert.notNull(username, "Username cannot be null");
        Assert.notNull(resource, "Resource cannot be null");
        log.info("Clearing role cache for user: {} and resource: {}",
                username, resource.getClass().getSimpleName());
        securityAuditService.logCacheCleared(
                String.format("Role cache cleared for user %s and resource %s",
                        username, resource.getClass().getSimpleName())
        );
    }

    // --- Explicit Role Assignment ---

    /**
     * Assigns a new set of roles to a user, replacing existing ones.
     * Validates the assignment against the role hierarchy.
     *
     * @param user     The user to assign roles to.
     * @param newRoles The new set of roles.
     * @return true if roles were successfully assigned, false otherwise.
     */
    @Transactional
    @CacheEvict(cacheNames = {Constants.USERS_CACHE, Constants.CURRENT_USER_CACHE}, key = "#user.userId")
    @Override
    public boolean assignRoles(@NotNull User user, @NotNull Set<UserRole> newRoles) {
        Assert.notNull(user, "User cannot be null for role assignment.");
        Assert.notNull(newRoles, "New roles set cannot be null.");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // Get current context for validation

        try {
            // Validate role assignment based on hierarchy and current user's authority
            if (roleHierarchyValidator.isRoleAssignmentInvalid(newRoles, authentication)) {
                log.warn("Role assignment validation failed for user: {} attempting to assign roles: {}",
                        user.getUsername(), newRoles.stream().map(UserRole::getRoleName).collect(Collectors.toSet()));
                securityAuditService.logFailedAttempt(user.getUsername(), "Invalid role assignment attempt (hierarchy validation)");
                return false; // Indicate failure
            }

            // If validation passes, update the user's roles
            user.createRoleSet(new HashSet<>(newRoles)); // Use a mutable copy if needed, or ensure User handles it
            userRepository.save(user); // Persist changes

            log.info("Successfully assigned roles {} to user {}",
                    newRoles.stream().map(UserRole::getRoleName).collect(Collectors.toSet()), user.getUsername());
            // Audit successful assignment (consider adding details about the assigner)
            securityAuditService.logRoleAssignment(user.getUsername(), newRoles, user); // Audit assignment to the user resource itself

            // Clear dynamic role cache for this user (since static roles changed)
            // This requires iterating through potential resources or clearing all user entries.
            // Using allEntries = true on a user-specific cache eviction might be simpler if feasible.
            // For now, let's rely on the user cache eviction above and potential TTL expiry for dynamic roles.
            // clearUserRolesForAllResources(user.getUsername()); // Hypothetical method

            return true; // Indicate success

        } catch (Exception e) {
            log.error("Error assigning roles {} to user {}: {}",
                    newRoles.stream().map(UserRole::getRoleName).collect(Collectors.toSet()), user.getUsername(), e.getMessage(), e);
            // Audit failure
            securityAuditService.logFailedAttempt(user.getUsername(), "Error during role assignment: " + e.getMessage());
            // Optionally alert security team
            alertService.alertSecurityTeam(new SecurityAlert(
                    String.valueOf(user.getUserId()),
                    "Failed to assign roles",
                    Map.of("assignedRoles", newRoles.toString(), "error", e.getMessage()),
                    AlertSeverityEnum.HIGH, Instant.now(), "production")
            );
            // Consider throwing a specific exception instead of returning false
            throw new RoleAssignmentException("Failed to assign roles to user " + user.getUsername(), e);
            // return false; // Indicate failure
        }
    }

    /**
     * Checks if a user can manage a specific product based on roles and ownership.
     *
     * @param user    The user attempting the action.
     * @param product The product being managed.
     * @return true if the user has sufficient privileges, false otherwise.
     */
    @Override
    public boolean canManageProduct(@NotNull User user, @NotNull Product product) {
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(product, "Product cannot be null");

        Set<UserRole> userRoles = user.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            return false; // No roles, cannot manage
        }

        // Check if user has a role with authority over PRODUCT_EDITOR
        boolean hasManagingRole = userRoles.stream()
                .map(UserRole::getRoleName)
                .anyMatch(role -> roleHierarchyValidator.hasAuthorityOver(role, AppRole.ROLE_PRODUCT_EDITOR));

        if (hasManagingRole) {
            return true; // User has a role like ADMIN, MANAGER, etc.
        }

        // Check if the user is the SELLER of the product
        boolean isSeller = userRoles.stream().anyMatch(ur -> ur.getRoleName() == AppRole.ROLE_SELLER);
        return isSeller && product.getSeller() != null && user.getUserId().equals(product.getSeller().getUserId()); // User is the seller of this specific product
// No sufficient privileges found
    }

    /*
     * Removed determineRoles and its helpers (determineProductRoles, etc.)
     * as the logic is now integrated into add*Roles methods called from
     * determineAndValidateRoles.
     */
    // private Set<UserRole> determineRoles(Object resource) { ... }
    // private Set<UserRole> determineProductRoles(Product product) { ... }
    // private Set<UserRole> determineOrderRoles(Order order) { ... }
    // private Set<UserRole> determineCategoryRoles(Category category) { ... }

}
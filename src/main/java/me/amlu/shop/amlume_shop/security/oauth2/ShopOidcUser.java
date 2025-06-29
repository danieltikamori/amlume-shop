/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.oauth2;

import me.amlu.shop.amlume_shop.user_management.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ShopOidcUser implements OidcUser, UserDetails {

    private final User shopUser; // The amlume-shop User entity
    private final OidcUser oidcUserDelegate; // The original OidcUser from the provider

    public ShopOidcUser(User shopUser, OidcUser oidcUserDelegate) {
        Assert.notNull(shopUser, "shopUser cannot be null");
        Assert.notNull(oidcUserDelegate, "oidcUserDelegate cannot be null");
        this.shopUser = shopUser;
        this.oidcUserDelegate = oidcUserDelegate;
    }

    // --- OidcUser methods (delegate to oidcUserDelegate) ---
    @Override
    public Map<String, Object> getClaims() {
        return this.oidcUserDelegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return this.oidcUserDelegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return this.oidcUserDelegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.oidcUserDelegate.getAttributes();
    }

    // --- UserDetails methods (use amlume-shop.User) ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Authorities should come from the amlume-shop User entity,
        // which are mapped from the token roles by CustomOidcUserService
        return this.shopUser.getAuthorities();
    }

    @Override
    public String getPassword() {
        // The amlume-shop User's password.
        // For OAuth2/OIDC authenticated users, this will likely be null or irrelevant
        // if amlume-shop doesn't store passwords locally anymore.
        return this.shopUser.getPassword();
    }

    @Override
    public String getUsername() {
        // This should be the primary identifier used within amlume-shop's security context.
        // It should be the userEmail from the amlume-shop.User entity.
        // Ensure amlume-shop.User.getUsername() returns the userEmail.
        return this.shopUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        // Status from amlume-shop User entity.
        // This might be synced from authserver or managed locally.
        return this.shopUser.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.shopUser.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.shopUser.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.shopUser.isEnabled();
    }

    // --- OAuth2User method (name attribute from OIDC token, usually 'sub') ---
    @Override
    public String getName() {
        // The 'name' from OidcUser is typically the 'sub' (subject) claim from the ID token.
        // In the authserver, 'sub' is the user's userEmail.
        // The 'user_id_numeric' claim is what we use for stable linking.
        return this.oidcUserDelegate.getName();
    }

    // --- Custom getter for the shopUser ---
    public User getShopUser() {
        return this.shopUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopOidcUser that = (ShopOidcUser) o;
        // Equality based on the underlying shopUser's ID and the OIDC subject ('name')
        return Objects.equals(shopUser.getUserId(), that.shopUser.getUserId()) &&
                Objects.equals(this.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(shopUser.getUserId(), this.getName());
    }

    @Override
    public String toString() {
        return "ShopOidcUser{" +
                "shopUserId=" + (shopUser != null ? shopUser.getUserId() : "null") +
                ", oidcSubject='" + getName() + '\'' +
                ", shopUserEmail='" + (shopUser != null ? shopUser.getUsername() : "null") + '\'' +
                '}';
    }
}

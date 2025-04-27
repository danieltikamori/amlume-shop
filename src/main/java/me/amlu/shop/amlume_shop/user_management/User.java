/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.user_management;

import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.model.RefreshToken;
import me.amlu.shop.amlume_shop.user_management.address.Address;
import me.amlu.shop.amlume_shop.product_management.Product;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_name"),
        @UniqueConstraint(columnNames = "user_email")
}, indexes = {
        @Index(name = "idx_user_name", columnList = "user_name"),
        @Index(name = "idx_user_email", columnList = "user_email"),
//        @Index(name = "idx_user_orders", columnList = "user_orders")
})
public class User extends BaseEntity implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Embedded
    // Override the column name for the 'username' field within AuthenticationInfo
    @AttributeOverrides({
            @AttributeOverride(name = "username.username", column = @Column(name = "user_name", nullable = false, unique = true)), // Assuming Username VO has 'username' field
            // Add override for password if you need a specific column name, e.g.:
            @AttributeOverride(name = "password.password", column = @Column(name = "user_password", nullable = false, length = 128)) // Assuming UserPassword VO has 'password' field
    })
    private AuthenticationInfo authenticationInfo; // Set via constructor/builder

    // Override the column name for the 'userEmail' field within ContactInfo
    @AttributeOverrides({
            @AttributeOverride(name = "userEmail.userEmail", column = @Column(name = "user_email", nullable = false, unique = true)), // Assuming UserEmail VO has 'userEmail' field
            @AttributeOverride(name = "firstName", column = @Column(name = "first_name", nullable = false, length = 127)),
            @AttributeOverride(name = "lastName", column = @Column(name = "last_name", nullable = false, length = 127)),
            @AttributeOverride(name = "emailVerified", column = @Column(name = "email_verified", nullable = false)),
//            @AttributeOverride(name = "phoneNumber", column = @Column(name = "phone_number", length = 50)) // Assuming PhoneNumber VO has 'phoneNumber' field
            // Add override for phone number if you need a specific column name:
             @AttributeOverride(name = "phoneNumber.phoneNumber", column = @Column(name = "phone_number", length = 50))
            // Add overrides for other ContactInfo fields if needed
    })
    @Embedded
    private ContactInfo contactInfo; // Set via constructor/builder

    @AttributeOverrides({
            @AttributeOverride(name = "accountNonExpired", column = @Column(name = "account_non_expired", nullable = false)),
            @AttributeOverride(name = "accountNonLocked", column = @Column(name = "account_non_locked", nullable = false)),
            @AttributeOverride(name = "credentialsNonExpired", column = @Column(name = "credentials_non_expired", nullable = false)),
            @AttributeOverride(name = "enabled", column = @Column(name = "enabled", nullable = false)),
            @AttributeOverride(name = "lastLoginTime", column = @Column(name = "last_login_time")),
            @AttributeOverride(name = "failedLoginAttempts", column = @Column(name = "failed_login_attempts")),
            @AttributeOverride(name = "lockedAt", column = @Column(name = "locked_at"))
    })
    @Embedded
    private AccountStatus accountStatus; // Set via constructor/builder

    @AttributeOverrides({
            @AttributeOverride(name = "mfaEnabled", column = @Column(name = "mfa_enabled", nullable = false)),
            @AttributeOverride(name = "mfaMethod", column = @Column(name = "mfa_method")),
            @AttributeOverride(name = "mfaEnforced", column = @Column(name = "mfa_enforced")),
            @AttributeOverride(name = "mfaQrCodeUrl.mfaQrCodeUrl", column = @Column(name = "mfa_qr_code_url")),
            @AttributeOverride(name = "mfaSecret.mfaSecret", column = @Column(name = "mfa_secret"))
    })
    @Embedded
    private MfaInfo mfaInfo; // Set via constructor/builder

    @AttributeOverrides({
            @AttributeOverride(name = "deviceFingerprintingEnabled", column = @Column(name = "device_fingerprinting_enabled", nullable = false)),
            @AttributeOverride(name = "deviceFingerprintingMethod", column = @Column(name = "device_fingerprinting_method")),
//            @AttributeOverride(name = "deviceFingerprintingData.deviceFingerprintingData", column = @Column(name = "device_fingerprinting_data"))
    })
    @Embedded
    private DeviceFingerprintingInfo deviceFingerprintingInfo; // Set via constructor/builder

    @AttributeOverrides({
            @AttributeOverride(name = "department", column = @Column(name = "department")),
            @AttributeOverride(name = "region", column = @Column(name = "region")),
    })
    @Embedded
    private LocationInfo locationInfo; // Set via constructor/builder

    // Initialized collection - JPA will replace this instance upon load
    @ElementCollection(fetch = FetchType.EAGER) // EAGER fetch for roles is often acceptable
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<UserRole> roles = new HashSet<>();

    // Initialized collection - JPA will replace this instance upon load
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    // NOTE: @JoinTable is typically for @ManyToMany or unidirectional @OneToMany to a non-entity.
    // For mappedBy = "user" on the target entity, the join column should be defined in the target entity (Address).
    // If Address has a 'user' field:
    // @OneToMany(mappedBy = "user", ...)
    // private List<Address> addresses = new ArrayList<>();
    // If Address does NOT have a 'user' field and this is unidirectional from User to Address:
    // @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    // @JoinColumn(name = "user_id") // This maps the foreign key column in the address table back to the user
    // private List<Address> addresses = new ArrayList<>();
    // Assuming 'Address' has a 'user' field and this is the bidirectional side:
    private List<Address> addresses = new ArrayList<>();


    // Initialized collection - JPA will replace this instance upon load
    @OneToMany(mappedBy = "categoryManager", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Category> categories = new ArrayList<>();

    // Initialized collection - JPA will replace this instance upon load
    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Product> products = new HashSet<>();

    // Initialized collection - JPA will replace this instance upon load
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    // Protected constructor required by JPA
    protected User() {
    }

    // Private constructor used by the builder
    private User(UserBuilder<?, ?> b) {
        super(b); // Call superclass constructor
        this.userId = b.userId;
        this.authenticationInfo = b.authenticationInfo;
        this.contactInfo = b.contactInfo;
        this.accountStatus = b.accountStatus;
        this.mfaInfo = b.mfaInfo;
        this.deviceFingerprintingInfo = b.deviceFingerprintingInfo;
        this.locationInfo = b.locationInfo;
        // For collections, initialize even if builder provides null, though builder should ideally provide empty collections
        this.roles = Optional.ofNullable(b.roles).orElseGet(HashSet::new);
        this.addresses = Optional.ofNullable(b.addresses).orElseGet(ArrayList::new);
        this.categories = Optional.ofNullable(b.categories).orElseGet(ArrayList::new);
        this.products = Optional.ofNullable(b.products).orElseGet(HashSet::new);
        this.refreshTokens = Optional.ofNullable(b.refreshTokens).orElseGet(ArrayList::new);
    }

    // Static factory method for the builder
    public static UserBuilder<?, ?> builder() {
        return new UserBuilderImpl();
    }

    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Ensure roles is not null, although initialized, paranoid check
        return roles != null ? roles.stream()
                .map(role -> {
                    assert role.getRoleName() != null; // Assert enum is not null
                    return new SimpleGrantedAuthority(role.getRoleName().name()); // Convert enum name to string authority
                })
                .collect(Collectors.toSet()) // Collect into a Set
                : Collections.emptySet(); // Return an empty set if roles are null
    }

    @Override
    public String getPassword() {
        // Ensure authenticationInfo is not null for safety, though it shouldn't be with @Embedded
        return (authenticationInfo != null) ? authenticationInfo.getPassword() : null;
    }

    @Override
    public String getUsername() {
        // Ensure authenticationInfo is not null
        return (authenticationInfo != null) ? authenticationInfo.getUsername() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        // Ensure accountStatus is not null
        return (accountStatus != null) && accountStatus.isEnabled();
    }
    // --- End UserDetails ---

    // --- Auditable Implementation ---
    // Method required by BaseEntity's isNew() logic
    @Override
    @Transient
    public Long getAuditableId() {
        return this.userId;
    }
    // --- End Auditable ---


    // --- Getters and NO setters ---

    public Long getUserId() {
        return this.userId;
    }

    public AuthenticationInfo getAuthenticationInfo() {
        return this.authenticationInfo;
    }

    public ContactInfo getContactInfo() {
        return this.contactInfo;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public MfaInfo getMfaInfo() {
        return this.mfaInfo;
    }

    public DeviceFingerprintingInfo getDeviceFingerprintingInfo() {
        return this.deviceFingerprintingInfo;
    }

    public LocationInfo getLocationInfo() {
        return this.locationInfo;
    }

    // Getters for collections - return unmodifiable views is good practice for encapsulation
    // though JPA-managed collections might behave differently. Returning the collection directly
    // allows JPA to track changes when elements are added/removed via the add/remove methods below.
    public Set<UserRole> getRoles() {
        return this.roles; // Return JPA-managed collection
    }

    public List<Address> getAddresses() {
        return this.addresses; // Return JPA-managed collection
    }

    public List<Category> getCategories() {
        return this.categories; // Return JPA-managed collection
    }

    public Set<Product> getProducts() {
        return this.products; // Return JPA-managed collection
    }

    public List<RefreshToken> getRefreshTokens() {
        return this.refreshTokens; // Return JPA-managed collection
    }

    // --- End Getters ---

    // --- equals() and hashCode()

    // Proxy-aware implementation of equals()
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        // Gets the underlying class even if 'o' is a proxy
        HibernateProxy oHibernateProxy = o instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> oEffectiveClass = oHibernateProxy != null ? oHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        // Gets the underlying class even if 'this' is a proxy
        HibernateProxy thisHibernateProxy = this instanceof HibernateProxy hibernateProxy ? hibernateProxy : null;
        Class<?> thisEffectiveClass = thisHibernateProxy != null ? thisHibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        // Compares the effective classes
        if (thisEffectiveClass != oEffectiveClass) return false;
        // Now safe to cast (though instanceof check is slightly redundant now)
        if (!(o instanceof User user)) return false;
        // Finally, compare by ID
        return getUserId() != null && Objects.equals(getUserId(), user.getUserId());
    }

    // Proxy-awareness is needed for hashCode if it uses getClass()
    @Override
    public final int hashCode() {
        // Base the hashCode primarily on the unique identifier (userId)
        // Objects.hash() handles null correctly if the entity is new (ID not yet assigned)
        return Objects.hash(userId);

        // --- Alternative using a constant for null ID (also common) ---
        // return userId == null ? 31 : userId.hashCode();
    }

    // --- End equals() and hashCode() ---

    // --- toString() Updated to exclude collections for performance/cycles. (Old version (below) ---
    @Override
    public String toString() {
        // Exclude collections and embedded objects from toString to avoid lazy loading issues or cycles
        return "User(" +
                "userId=" + this.userId +
                ", authenticationInfo=" + (this.authenticationInfo != null ? "present" : "null") + // Indicate presence
                ", contactInfo=" + (this.contactInfo != null ? "present" : "null") +
                ", accountStatus=" + (this.accountStatus != null ? "present" : "null") +
                ", mfaInfo=" + (this.mfaInfo != null ? "present" : "null") +
                ", deviceFingerprintingInfo=" + (this.deviceFingerprintingInfo != null ? "present" : "null") +
                ", locationInfo=" + (this.locationInfo != null ? "present" : "null") +
                ", rolesSize=" + (this.roles != null ? this.roles.size() : 0) + // Indicate size
                ", addressesSize=" + (this.addresses != null ? this.addresses.size() : 0) +
                ", categoriesSize=" + (this.categories != null ? this.categories.size() : 0) +
                ", productsSize=" + (this.products != null ? this.products.size() : 0) +
                ", refreshTokensSize=" + (this.refreshTokens != null ? this.refreshTokens.size() : 0) +
                ')';
    }

    //    @Override
//    public String toString() {
//        return "User(userId=" + this.getUserId() + ", authenticationInfo=" + this.getAuthenticationInfo() + ", contactInfo=" + this.getContactInfo() + ", accountStatus=" + this.getAccountStatus() + ", mfaInfo=" + this.getMfaInfo() + ", deviceFingerprintingInfo=" + this.getDeviceFingerprintingInfo() + ", locationInfo=" + this.getLocationInfo() + ", roles=" + this.getRoles() + ")";
//    }
    // --- End toString() ---

    // --- Methods to Modify Collections (New/Modified) ---

    public void addRole(UserRole role) {
        if (this.roles == null) { // Paranoid check, should be initialized
            this.roles = new HashSet<>();
        }
        if (role != null && !this.roles.contains(role)) {
            this.roles.add(role);
            // If UserRole had a back-reference to User, set it here: role.setUser(this);
        }
    }

    public void removeRole(UserRole role) {
        if (this.roles != null && role != null) {
            this.roles.remove(role);
            // If UserRole had a back-reference to User, clear it here: role.setUser(null);
        }
    }

    public void createRoleSet(Set<UserRole> roles) {
        this.roles = roles;
    }

    public void addAddress(Address address) {
        if (this.addresses == null) { // Paranoid check
            this.addresses = new ArrayList<>();
        }
        if (address != null && !this.addresses.contains(address)) {
            this.addresses.add(address);
            // Ensure bidirectional relationship is set if Address has a 'user' field
            address.setUser(this);
        }
    }

    public void removeAddress(Address address) {
        if (this.addresses != null && address != null && this.addresses.remove(address)) {
            // Clear bidirectional relationship if Address has a 'user' field
            address.setUser(null);
        }
    }

    public void addCategory(Category category) {
        if (this.categories == null) { // Paranoid check
            this.categories = new ArrayList<>();
        }
        if (category != null && !this.categories.contains(category)) {
            this.categories.add(category);
            // Ensure bidirectional relationship is set if Category has 'categoryManager' field
            category.setCategoryManager(this);
        }
    }

    public void removeCategory(Category category) {
        if (this.categories != null && category != null && this.categories.remove(category)) {
            // Clear bidirectional relationship if Category has 'categoryManager' field
            category.setCategoryManager(null);
        }
    }

    public void addProduct(Product product) {
        if (this.products == null) { // Paranoid check
            this.products = new HashSet<>();
        }
        if (product != null && !this.products.contains(product)) {
            this.products.add(product);
            // Ensure bidirectional relationship is set if Product has a 'seller' field
            product.setSeller(this);
        }
    }

    public void removeProduct(Product product) {
        if (this.products != null && product != null && this.products.remove(product)) {
            // Clear bidirectional relationship if Product has a 'seller' field
            product.setSeller(null);
        }
    }

    public void addRefreshToken(RefreshToken refreshToken) {
        if (this.refreshTokens == null) { // Paranoid check
            this.refreshTokens = new ArrayList<>();
        }
        if (refreshToken != null && !this.refreshTokens.contains(refreshToken)) {
            this.refreshTokens.add(refreshToken);
            // Ensure bidirectional relationship is set if RefreshToken has a 'user' field
            refreshToken.setUser(this);
        }
    }

    public void removeRefreshToken(RefreshToken refreshToken) {
        if (this.refreshTokens != null && refreshToken != null && this.refreshTokens.remove(refreshToken)) {
            // Clear bidirectional relationship if RefreshToken has a 'user' field
            refreshToken.setUser(null);
        }
    }

    // --- Methods to update embedded objects ---
    public void updateAuthentication(AuthenticationInfo newAuthInfo) {

        this.authenticationInfo = newAuthInfo;
    }

    public void updateContactInfo(ContactInfo newContactInfo) {
        this.contactInfo = newContactInfo;
    }

    public void updateAccountStatus(AccountStatus newAccountStatus) {
        this.accountStatus = newAccountStatus;
    }

    public void updateMfaInfo(MfaInfo newMfaInfo) {
        this.mfaInfo = newMfaInfo;
    }

    public void updateDeviceFingerprintingInfo(DeviceFingerprintingInfo newDeviceFingerprintingInfo) {
        this.deviceFingerprintingInfo = newDeviceFingerprintingInfo;
    }

    public void updateLocationInfo(LocationInfo newLocationInfo) {
        this.locationInfo = newLocationInfo;
    }

    // --- End Methods to update embedded objects ---

    // --- Methods to update specific fields within embedded objects ---
    public void updatePassword(String encodedPassword) {
        if (this.authenticationInfo != null) {
            this.authenticationInfo.updatePassword(encodedPassword); // Requires updatePassword method in AuthenticationInfo
        }
    }

    public void updateEmailAddress(String newEmailAddress) {
        if (this.contactInfo != null) {
            this.contactInfo.updateEmailAddress(newEmailAddress);
        }
    }

    public void updateLastLoginTime(Instant now) {
        if (this.accountStatus != null) {
            this.accountStatus.updateLastLoginTime(now);
        }
    }

    public void updateFailedLoginAttempts(int i) {
        accountStatus.updateFailedLoginAttempts(i);
    }

    public void updateLockTime(Instant lockedAt) {
        accountStatus.updateLockTime(lockedAt);
    }

    public void updateAccountNonLocked(boolean isAccountNonLocked) {
        accountStatus.updateAccountNonLocked(isAccountNonLocked);
    }

    // --- End Methods to update specific fields within embedded objects ---


    // --- Other Methods ---

    // Optional convenience getter for ID
    public Long getId() {
        return this.userId;
    }

    // Simple role check helper
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    // Check device fingerprinting status
    public boolean isDeviceFingerprintingEnabled() {
        return deviceFingerprintingInfo != null && deviceFingerprintingInfo.isDeviceFingerprintingEnabled();
    }

    // Check MFA status
    public boolean isMfaEnabled() {
        return mfaInfo != null && mfaInfo.isMfaEnabled();
    }

    // Account status getters
    public int getFailedLoginAttempts() {
        return accountStatus.getFailedLoginAttempts();
    }

    public Instant getLockTime() {
        return accountStatus.getLockTime();
    }

    // --- End Methods ---

    // --- Builder Class (Adjusted to reflect private constructor and removed setters) ---
    public static abstract class UserBuilder<C extends User, B extends UserBuilder<C, B>> extends BaseEntityBuilder<C, B> {
        // Fields remain in the builder to hold values during construction
        private Long userId;
        private AuthenticationInfo authenticationInfo;
        private ContactInfo contactInfo;
        private AccountStatus accountStatus;
        private MfaInfo mfaInfo;
        private DeviceFingerprintingInfo deviceFingerprintingInfo;
        private LocationInfo locationInfo;
        private Set<UserRole> roles; // Builder can take the initial set/list
        private List<Address> addresses;
        private List<Category> categories;
        private Set<Product> products;
        private List<RefreshToken> refreshTokens;


        // Builder methods (Keep as is - these set values *on the builder*)
        public B userId(Long userId) {
            this.userId = userId;
            return self();
        }

        public B authenticationInfo(AuthenticationInfo authenticationInfo) {
            this.authenticationInfo = authenticationInfo;
            return self();
        }

        public B contactInfo(ContactInfo contactInfo) {
            this.contactInfo = contactInfo;
            return self();
        }

        public B accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return self();
        }

        public B mfaInfo(MfaInfo mfaInfo) {
            this.mfaInfo = mfaInfo;
            return self();
        }

        public B deviceFingerprintingInfo(DeviceFingerprintingInfo deviceFingerprintingInfo) {
            this.deviceFingerprintingInfo = deviceFingerprintingInfo;
            return self();
        }

        public B locationInfo(LocationInfo locationInfo) {
            this.locationInfo = locationInfo;
            return self();
        }

        public B roles(Set<UserRole> roles) {
            // Accept the collection in builder - the entity constructor will copy/handle null
            this.roles = roles;
            return self();
        }

        public B addresses(List<Address> addresses) {
            // Accept the collection in builder
            this.addresses = addresses;
            return self();
        }

        public B categories(List<Category> categories) {
            // Accept the collection in builder
            this.categories = categories;
            return self();
        }

        public B products(Set<Product> products) {
            // Accept the collection in builder
            this.products = products;
            return self();
        }

        public B refreshTokens(List<RefreshToken> refreshTokens) {
            // Accept the collection in builder
            this.refreshTokens = refreshTokens;
            return self();
        }


        protected abstract B self();

        public abstract C build();

        @Override
        public String toString() {
            // toString for builder is okay to show contained values
            return "User.UserBuilder(super=" + super.toString() + ", userId=" + this.userId + ", authenticationInfo=" + this.authenticationInfo + ", contactInfo=" + this.contactInfo + ", accountStatus=" + this.accountStatus + ", mfaInfo=" + this.mfaInfo + ", deviceFingerprintingInfo=" + this.deviceFingerprintingInfo + ", locationInfo=" + this.locationInfo + ", roles=" + this.roles + ", addresses=" + this.addresses + ", categories=" + this.categories + ", products=" + this.products + ", refreshTokens=" + this.refreshTokens + ")";
        }
    }

    private static final class UserBuilderImpl extends UserBuilder<User, UserBuilderImpl> {
        private UserBuilderImpl() {
        }

        @Override
        protected UserBuilderImpl self() {
            return this;
        }

        @Override
        public User build() {
            return new User(this);
        }
    }
}

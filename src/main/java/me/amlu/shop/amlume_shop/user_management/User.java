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

import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.persistence.*;
import me.amlu.shop.amlume_shop.category_management.Category;
import me.amlu.shop.amlume_shop.model.BaseEntity;
import me.amlu.shop.amlume_shop.product_management.Product;
import me.amlu.shop.amlume_shop.security.model.RefreshToken;
import me.amlu.shop.amlume_shop.user_management.address.Address;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "auth_server_subject_id"),
        @UniqueConstraint(columnNames = "user_email")
}, indexes = {
        @Index(name = "idx_auth_server_subject_id", columnList = "auth_server_subject_id"),
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

    /**
     * Authorization Server (authserver) Subject ID
     */
    @Column(name = "auth_server_subject_id", unique = true, nullable = true)
    // Nullable until first login via authserver
    private String authServerSubjectId;

    // Override the column name for the 'userEmail' field within ContactInfo
    @AttributeOverrides({
            @AttributeOverride(name = "userEmail.userEmail", column = @Column(name = "user_email", nullable = false, unique = true)), // Assuming UserEmail VO has 'userEmail' field
            @AttributeOverride(name = "givenName", column = @Column(name = "given_name", nullable = false, length = 127)),
            @AttributeOverride(name = "surname", column = @Column(name = "surname", nullable = false, length = 127)),
            @AttributeOverride(name = "emailVerified", column = @Column(name = "email_verified", nullable = false)),
            // Phone number is handled by ContactInfo and stored as a string in E.164 format
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

    // REMOVED - Authserver concern
//    @AttributeOverrides({
//            @AttributeOverride(name = "deviceFingerprintingEnabled", column = @Column(name = "device_fingerprinting_enabled", nullable = false)),
//            @AttributeOverride(name = "deviceFingerprintingMethod", column = @Column(name = "device_fingerprinting_method")),
    /// /            @AttributeOverride(name = "deviceFingerprintingData.deviceFingerprintingData", column = @Column(name = "device_fingerprinting_data"))
//    })
//    @Embedded
//    private DeviceFingerprintingInfo deviceFingerprintingInfo; // Set via constructor/builder

    @AttributeOverrides({
            @AttributeOverride(name = "department", column = @Column(name = "department")),
            @AttributeOverride(name = "region", column = @Column(name = "region")),
    })
    @Embedded
    private LocationInfo locationInfo; // Set via constructor/builder

    // Initialized collection - JPA will replace this instance upon load
    @ElementCollection(fetch = FetchType.EAGER) // EAGER fetch for roles is often acceptable
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) // Store enum as string
    @Column(name = "role_name", nullable = false) // Name of the column in the user_roles table holding the role
    private Set<UserRole> roles = new HashSet<>();
//    private Set<UserRole.roleName> roles = new HashSet<>(); // Store roleName enum directly

    // REMOVED - Authserver concern
//    @OneToMany(
//            mappedBy = "user", // Matches the 'user' field in PasskeyCredential
//            cascade = CascadeType.ALL, // Persist/merge/remove Passkeys with User
//            fetch = FetchType.LAZY,    // Load Passkeys only when needed
//            orphanRemoval = true       // Delete Passkeys if removed from this list
//    )
//    private List<PasskeyCredential> passkeyCredentials = new ArrayList<>();

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
        this.authServerSubjectId = b.authServerSubjectId;
        this.contactInfo = b.contactInfo;
        this.accountStatus = b.accountStatus;
//        this.deviceFingerprintingInfo = b.deviceFingerprintingInfo;
        this.locationInfo = b.locationInfo;
        // For collections, initialize even if builder provides null, though builder should ideally provide empty collections
        this.roles = Optional.ofNullable(b.roles).orElseGet(HashSet::new);
        this.addresses = Optional.ofNullable(b.addresses).orElseGet(ArrayList::new);
        this.categories = Optional.ofNullable(b.categories).orElseGet(ArrayList::new);
        this.products = Optional.ofNullable(b.products).orElseGet(HashSet::new);
        this.refreshTokens = Optional.ofNullable(b.refreshTokens).orElseGet(ArrayList::new);
//        this.passkeyCredentials = Optional.ofNullable(b.passkeyCredentials).orElseGet(ArrayList::new);
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

    /**
     * Required for UserDetails interface
     *
     * @return empty string as password is handled by authserver
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * Required for UserDetails interface
     * Username is the user userEmail to simplify authentication
     * @return user userEmail
     */
    @Override
    public String getUsername() {
        return this.contactInfo.getEmail();
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

    public String getAuthServerSubjectId() {
        return this.authServerSubjectId;
    }


    public ContactInfo getContactInfo() {
        return this.contactInfo;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

//    public DeviceFingerprintingInfo getDeviceFingerprintingInfo() {
//        return this.deviceFingerprintingInfo;
//    }

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

//    public List<PasskeyCredential> getPasskeyCredentials() {
//        return this.passkeyCredentials; // Return JPA-managed collection
//    }

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
                ", authServerSubjectId=" + (this.authServerSubjectId != null ? "present" : "null") +
                ", contactInfo=" + (this.contactInfo != null ? "present" : "null") +
                ", accountStatus=" + (this.accountStatus != null ? "present" : "null") +
//                ", deviceFingerprintingInfo=" + (this.deviceFingerprintingInfo != null ? "present" : "null") +
                ", locationInfo=" + (this.locationInfo != null ? "present" : "null") +
                ", rolesSize=" + (this.roles != null ? this.roles.size() : 0) + // Indicate size
                ", addressesSize=" + (this.addresses != null ? this.addresses.size() : 0) +
                ", categoriesSize=" + (this.categories != null ? this.categories.size() : 0) +
                ", productsSize=" + (this.products != null ? this.products.size() : 0) +
                ", refreshTokensSize=" + (this.refreshTokens != null ? this.refreshTokens.size() : 0) +
//                ", passkeyCredentialsSize=" + (this.passkeyCredentials != null ? this.passkeyCredentials.size() : 0) +
                ')';
    }

    // --- End toString() ---

    // --- Methods to Modify Collections (New/Modified) ---

    public void addRole(UserRole role) {
        if (this.roles == null) { // Paranoid check, should be initialized
            this.roles = new HashSet<>();
        }
        if (role != null) {
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

//    public void addPasskeyCredential(PasskeyCredential credential) {
//        if (this.passkeyCredentials == null) { // Paranoid check
//            this.passkeyCredentials = new ArrayList<>();
//        }
//        if (credential != null && !this.passkeyCredentials.contains(credential)) {
//            this.passkeyCredentials.add(credential);
//            // Ensure bidirectional relationship is set
//            credential.setUser(this);
//        }
//    }
//
//    public void removePasskeyCredential(PasskeyCredential credential) {
//        if (this.passkeyCredentials != null && credential != null && this.passkeyCredentials.remove(credential)) {
//            // Clear bidirectional relationship
//            credential.setUser(null);
//        }
//    }

    // --- Methods to update embedded objects ---
    public void updateAuthentication(AuthenticationInfo newAuthInfo) {

    }

    public void updateContactInfo(ContactInfo newContactInfo) {
        this.contactInfo = newContactInfo;
    }

    public void updateAccountStatus(AccountStatus newAccountStatus) {
        this.accountStatus = newAccountStatus;
    }


//    public void updateDeviceFingerprintingInfo(DeviceFingerprintingInfo newDeviceFingerprintingInfo) {
//        this.deviceFingerprintingInfo = newDeviceFingerprintingInfo;
//    }

    public void updateLocationInfo(LocationInfo newLocationInfo) {
        this.locationInfo = newLocationInfo;
    }

    // --- End Methods to update embedded objects ---

    // --- Methods to update specific fields within embedded objects ---

    public void updateEmailAddress(String newEmailAddress) {
        if (this.contactInfo != null) {
            this.contactInfo.updateEmailAddress(newEmailAddress);
        }
    }

    // Using string for phone number to avoid dependency on external library
    public void updatePhoneNumber(String newPhoneNumber) {
        if (this.contactInfo != null) {
            this.contactInfo.updatePhoneNumber(newPhoneNumber);
        }
    }

    // Using Phonenumber.PhoneNumber object for phone number
    public void updatePhoneNumber(Phonenumber.PhoneNumber newPhoneNumber) {
        if (this.contactInfo != null) {
            this.contactInfo.updatePhoneNumber(newPhoneNumber);
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

    public String getSubjectId() {
        return this.authServerSubjectId;
    }

    public void updateAuthServerSubjectId(String authServerSubjectId) {
        this.authServerSubjectId = Objects.requireNonNull(authServerSubjectId, "authServerSubjectId cannot be null");
    }

    // Simple role check helper
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

//    // Check device fingerprinting status
//    public boolean isDeviceFingerprintingEnabled() {
//        return deviceFingerprintingInfo != null && deviceFingerprintingInfo.isDeviceFingerprintingEnabled();
//    }

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
        private String authServerSubjectId;
        private ContactInfo contactInfo;
        private AccountStatus accountStatus;
        //        private DeviceFingerprintingInfo deviceFingerprintingInfo;
        private LocationInfo locationInfo;
        private Set<UserRole> roles; // Builder can take the initial set/list
        private List<Address> addresses;
        private List<Category> categories;
        private Set<Product> products;
        private List<RefreshToken> refreshTokens;
//        private List<PasskeyCredential> passkeyCredentials;


        // Builder methods (Keep as is - these set values *on the builder*)
        public B userId(Long userId) {
            this.userId = userId;
            return self();
        }

        public B authServerSubjectId(String authServerSubjectId) {
            this.authServerSubjectId = authServerSubjectId;
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

//        public B deviceFingerprintingInfo(DeviceFingerprintingInfo deviceFingerprintingInfo) {
//            this.deviceFingerprintingInfo = deviceFingerprintingInfo;
//            return self();
//        }

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

//        public B passkeyCredentials(List<PasskeyCredential> passkeyCredentials) {
//            this.passkeyCredentials = passkeyCredentials;
//            return self();
//        }

        protected abstract B self();

        public abstract C build();

        @Override
        public String toString() {
            // toString for builder is okay to show contained values
            return "User.UserBuilder(super=" + super.toString() +
                    ", userId=" + this.userId + "," +
                    " authServerSubjectId=" + this.authServerSubjectId +
                    ", contactInfo=" + this.contactInfo + ", accountStatus=" + this.accountStatus +
//                    ", deviceFingerprintingInfo=" + this.deviceFingerprintingInfo +
                    ", locationInfo=" + this.locationInfo + ", roles=" + this.roles +
                    ", addresses=" + this.addresses + ", categories=" + this.categories +
                    ", products=" + this.products + ", refreshTokens=" + this.refreshTokens +
//                    ", passkeyCredentials=" + this.passkeyCredentials +
                    ")";
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

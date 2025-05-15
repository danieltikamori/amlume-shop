/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.user.model.vo.AccountStatus;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "nickname"),
        @UniqueConstraint(columnNames = "mobile_number"),
        @UniqueConstraint(columnNames = "external_id")
}, indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_backup_email", columnList = "backup_email"),
        @Index(name = "idx_nickname", columnList = "nickname"),
        @Index(name = "idx_mobile_number", columnList = "mobile_number"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_updated_at", columnList = "updated_at"),
        @Index(name = "idx_external_id", columnList = "external_id")
})
public class User implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "external_id", unique = true) // Ensure externalId is unique if used as a primary lookup
    public String externalId; // User handle for WebAuthn.

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name") // Nullable
    private String lastName;

    @Column(name = "nickname") // Nullable, user-defined display name
    private String nickname;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private EmailAddress email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "backup_email", unique = true, nullable = true)) // Nullable, maybe not unique globally
    })
    private EmailAddress backupEmail;


    @Embedded
    @AttributeOverride(name = "e164Value", column = @Column(name = "mobile_number", length = 20, unique = true))
    private PhoneNumber mobileNumber;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<PasskeyCredential> passkeyCredentials = new HashSet<>(); // Initialize to avoid NullPointerExceptions

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password", nullable = true, length = 127))
    @JsonIgnore
    private HashedPassword password;

    @Embedded
    private AccountStatus accountStatus;

    @CreationTimestamp // Automatically set on creation
    @Column(name = "created_at", nullable = false, updatable = false) // Standard auditing field
    @JsonIgnore
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private Instant updatedAt;

    // Ensure 'authorities' set is initialized by the builder
    @ManyToMany(fetch = FetchType.EAGER)
    // EAGER can be acceptable if the number of roles per user is small and always needed. LAZY is generally safer for performance.
    @JoinTable(
            name = "user_authorities",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
    )
    @JsonIgnore
    // Exclude from default toString to avoid issues with lazy loading or verbosity
    private Set<Authority> authorities = new HashSet<>(); // Initialize to avoid NullPointerExceptions

    private User(Long id, String externalId, String firstName, String lastName, String nickname, EmailAddress email, EmailAddress backupEmail, PhoneNumber mobileNumber,
                 HashedPassword password, AccountStatus accountStatus, Instant createdAt, Instant updatedAt, Set<Authority> authorities) {
        Assert.hasText(firstName, "User first name cannot be empty.");
        Assert.notNull(email, "User email cannot be null.");

        this.id = id;
        this.externalId = externalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nickname = nickname;
        this.email = email;
        this.backupEmail = backupEmail;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.accountStatus = (accountStatus != null) ? accountStatus : AccountStatus.initial();
        this.createdAt = createdAt; // Will be set by @CreationTimestamp if new and null
        this.updatedAt = updatedAt; // Will be set by @UpdateTimestamp
        this.authorities = authorities != null ? new HashSet<>(authorities) : new HashSet<>(); // Defensive copy
        this.passkeyCredentials = new HashSet<>(); // Ensure initialized
    }

    /**
     * Required by JPA.
     */
    protected User() {
        // Initialize collections to prevent NullPointerExceptions if JPA creates instance
        this.authorities = new HashSet<>();
        this.passkeyCredentials = new HashSet<>();
        this.accountStatus = AccountStatus.initial(); // Sensible default for JPA-created instances
    }

    /**
     * Static factory method to create a new, minimally initialized user instance.
     * Primarily for scenarios where an entity instance is needed before full population (e.g., by JPA or some repository logic).
     * For fully constructed users, prefer the builder.
     * @return A new user instance with default initializations.
     */
    public static User createNew() {
        User newUser = new User(); // Calls the protected constructor
        // Ensure essential defaults are set if not handled by protected constructor
        // newUser.accountStatus = AccountStatus.initial(); // Already in protected constructor
        // newUser.authorities = new HashSet<>(); // Already in protected constructor
        // newUser.passkeyCredentials = new HashSet<>(); // Already in protected constructor
        return newUser;
    }

    private static Set<Authority> $default$authorities() { // Used by the builder
        return new HashSet<>();
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    // If using @AllArgsConstructor, it would be generated based on all fields.
    // You might prefer specific constructors or a builder pattern for more control.

    // --- Behavioral Methods ---

    // --- Behavioral Methods for Account Status ---

    public void recordLoginFailure() {
        this.accountStatus = this.accountStatus.recordLoginFailure();
    }

    public void resetLoginFailures() {
        this.accountStatus = this.accountStatus.resetLoginFailures();
    }

    public void lockAccount(Duration lockDuration) {
        Assert.notNull(lockDuration, "Lock duration cannot be null.");
        Assert.isTrue(!lockDuration.isNegative() && !lockDuration.isZero(), "Lock duration must be positive.");
        this.accountStatus = this.accountStatus.lockUntil(Instant.now().plus(lockDuration));
    }
    public void unlockAccount() { this.accountStatus = this.accountStatus.unlock(); }
    public void enableAccount() { this.accountStatus = this.accountStatus.enable(); }
    public void disableAccount() { this.accountStatus = this.accountStatus.disable(); }
    public boolean isLoginAttemptsExceeded() { return this.accountStatus.getFailedLoginAttempts() >= AccountStatus.DEFAULT_MAX_FAILED_ATTEMPTS; }


    /**
     * Sets the external ID for the user.
     * @param externalId The new external ID. Must not be null or blank.
     */
    public void setExternalId(String externalId) {
        Assert.hasText(externalId, "External ID cannot be blank.");
        this.externalId = externalId;
    }

    public void updateFirstName(String newFirstName) {
        Assert.hasText(newFirstName, "User first name cannot be blank.");
        this.firstName = newFirstName;
    }

    public void updateLastName(String newLastName) {
        // lastName can be null or blank if allowed by business rules
        this.lastName = newLastName;
    }

    public void updateNickname(String newNickname) {
        // nickname can be null or blank
        this.nickname = newNickname;
    }

    public void updateEmail(EmailAddress newEmail) {
        Assert.notNull(newEmail, "User email cannot be null.");
        this.email = newEmail;
    }

    public void updateBackupEmail(EmailAddress newBackupEmail) {
        // Can be null to clear it
        this.backupEmail = newBackupEmail;
    }

    /**
     * Changes the user's password.
     * The new password must already be hashed.
     *
     * @param newHashedPassword The new, hashed password. Must not be null.
     */
    public void changePassword(HashedPassword newHashedPassword) {
        // If password is now nullable, newHashedPassword could also be null to remove a password
        // However, typically changePassword implies setting a new non-null password.
        // If removing password is a use case, a separate method like removePassword() might be clearer.
        // For now, assume changePassword means setting one.
        Objects.requireNonNull(newHashedPassword, "New hashed password for changePassword cannot be null.");
        this.password = newHashedPassword;
        // @UpdateTimestamp will handle updatedAt
    }

    /**
     * Updates the user's mobile phone number.
     *
     * @param newMobileNumber The new phone number. Can be null to remove the phone number.
     *                        The PhoneNumber VO should handle its own validation.
     */
    public void updateMobileNumber(PhoneNumber newMobileNumber) {
        this.mobileNumber = newMobileNumber;
        // @UpdateTimestamp will handle updatedAt
    }

    /**
     * Assigns an authority (role) to the user.
     * Does nothing if the authority is null or already assigned.
     *
     * @param authority The authority to assign.
     */
    public void assignAuthority(Authority authority) {
        if (authority != null) {
            if (this.authorities == null) this.authorities = new HashSet<>();
            this.authorities.add(authority);
            // @UpdateTimestamp will handle updatedAt if the collection change is detected
        }
    }

    /**
     * Revokes an authority (role) from the user.
     * Does nothing if the authority is null or not assigned.
     *
     * @param authority The authority to revoke.
     */
    public void revokeAuthority(Authority authority) {
        if (authority != null && this.authorities != null) this.authorities.remove(authority);
    }

    /**
     * Replaces all existing authorities with the given set.
     *
     * @param newAuthorities The new set of authorities. If null or empty, authorities will be cleared.
     */
    public void setAuthorities(Set<Authority> newAuthorities) {
        this.authorities.clear();
        if (newAuthorities != null) this.authorities.addAll(newAuthorities);
    }

    public void addPasskeyCredential(PasskeyCredential credential) {
        if (credential != null) {
            this.passkeyCredentials.add(credential);
            credential.setUser(this); // Maintain bidirectional relationship
        }
    }

    public void removePasskeyCredential(PasskeyCredential credential) {
        if (credential != null) {
            this.passkeyCredentials.remove(credential);
            credential.setUser(null); // Maintain bidirectional relationship
        }
    }

    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities == null) return Collections.emptySet();
        return this.authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toSet());
    }
    @Override public String getPassword() { return this.password != null ? this.password.getValue() : null; }
    @Override public String getUsername() { return this.email != null ? this.email.getValue() : null; } // Email is the username
    @Override public boolean isAccountNonExpired() { return this.accountStatus != null && this.accountStatus.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked() { return this.accountStatus != null && this.accountStatus.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return this.accountStatus != null && this.accountStatus.isCredentialsNonExpired(); }
    @Override public boolean isEnabled() { return this.accountStatus != null && this.accountStatus.isEnabled(); }

    // --- equals and hashCode ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        // Consistent with ID-based equals.
        // For new (transient) entities, Objects.hashCode(null) is 0.
        // Once persisted, the ID is used.
        return Objects.hashCode(getId());
    }

    // --- Getters ---
    public Long getId() { return this.id; }
    public String getExternalId() { return this.externalId; }
    public String getFirstName() { return this.firstName; }
    public String getLastName() { return this.lastName; }
    public String getNickname() { return this.nickname; }
    public EmailAddress getEmail() { return this.email; }

    public EmailAddress getBackupEmail() {
        return this.backupEmail;
    }
    public PhoneNumber getMobileNumber() { return this.mobileNumber; }
    public Set<PasskeyCredential> getPasskeyCredentials() { return Collections.unmodifiableSet(this.passkeyCredentials); }
    public AccountStatus getAccountStatus() { return this.accountStatus; }
    public Instant getCreatedAt() { return this.createdAt; }
    public Instant getUpdatedAt() { return this.updatedAt; }

    /**
     * Returns a displayable full name, preferring nickname if available,
     * otherwise concatenating first and last name.
     * @return A displayable full name.
     */
    @Transient // Not a persistent field, derived
    public String getDisplayableFullName() {
        if (StringUtils.hasText(this.nickname)) {
            return this.nickname;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(this.firstName)) {
            sb.append(this.firstName);
        }
        if (StringUtils.hasText(this.lastName)) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(this.lastName);
        }
        return sb.isEmpty() ? "" : sb.toString();
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email=" + email +
                ", backupEmail=" + backupEmail +
                ", mobileNumber=" + mobileNumber +
                ", accountStatus=" + accountStatus +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", authorities_count=" + (authorities != null ? authorities.size() : 0) +
                ", passkeyCredentials_count=" + (passkeyCredentials != null ? passkeyCredentials.size() : 0) +
                '}';
    }

    public UserBuilder toBuilder() {
        // Ensure all fields managed by the private constructor are included
        return new UserBuilder()
                .id(this.id)
                .externalId(this.externalId)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .nickname(this.nickname)
                .email(this.email)
                .backupEmail(this.backupEmail)
                .mobileNumber(this.mobileNumber)
                .password(this.password)
                .accountStatus(this.accountStatus)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .authorities(new HashSet<>(this.authorities));
        // externalId and displayName are not part of the private constructor,
        // so they are not set by this toBuilder() directly into the builder's fields
        // for that constructor. If they were, they'd be added here.
        // The built User would then have its public externalId/displayName fields set separately if needed.
    }

    public static class UserBuilder {
        private Long id;
        private String externalId;
        private String firstName;
        private String lastName;
        private String nickname;
        private EmailAddress email;
        private EmailAddress backupEmail;
        private PhoneNumber mobileNumber;
        private HashedPassword password;
        private AccountStatus accountStatus;
        private Instant createdAt;
        private Instant updatedAt;
        private Set<Authority> authorities$value;
        private boolean authorities$set;

        // Note: externalId and displayName are not managed by this builder directly
        // as they are not in the private User constructor's parameters.
        // They would be set on the User object after it's built, if needed.

        UserBuilder() {
        }

        public UserBuilder id(Long id) { this.id = id; return this; }
        public UserBuilder externalId(String externalId) { this.externalId = externalId; return this; }
        public UserBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserBuilder nickname(String nickname) { this.nickname = nickname; return this; }
        public UserBuilder email(EmailAddress email) { this.email = email; return this; }

        public UserBuilder backupEmail(EmailAddress backupEmail) {
            this.backupEmail = backupEmail;
            return this;
        }
        public UserBuilder mobileNumber(PhoneNumber mobileNumber) { this.mobileNumber = mobileNumber; return this; }
        @JsonIgnore public UserBuilder password(HashedPassword password) { this.password = password; return this; }
        public UserBuilder accountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; return this; }
        @JsonIgnore public UserBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        @JsonIgnore public UserBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        @JsonIgnore public UserBuilder authorities(Set<Authority> authorities) {
            this.authorities$value = authorities;
            this.authorities$set = true;
            return this;
        }

        public User build() {
            Set<Authority> authoritiesValue = this.authorities$value;
            if (!this.authorities$set) {
                authoritiesValue = User.$default$authorities();
            }
            return new User(this.id, this.externalId, this.firstName, this.lastName, this.nickname,
                    this.email, this.backupEmail, this.mobileNumber,
                            this.password, this.accountStatus, this.createdAt, this.updatedAt, authoritiesValue);
        }

        @Override
        public String toString() {
            return "User.UserBuilder(id=" + this.id +
                    ", externalId=" + this.externalId +
                    ", firstName=" + this.firstName +
                    ", lastName=" + this.lastName +
                    ", nickname=" + this.nickname +
                    ", email=" + this.email +
                    ", backupEmail=" + this.backupEmail +
                    ", mobileNumber=" + this.mobileNumber +
                    ", password=" + (password != null ? "[PROTECTED]" : "null") +
                    ", accountStatus=" + this.accountStatus +
                    ", createdAt=" + this.createdAt +
                    ", updatedAt=" + this.updatedAt +
                    ", authorities$value=" + this.authorities$value + ")";
        }
    }
}

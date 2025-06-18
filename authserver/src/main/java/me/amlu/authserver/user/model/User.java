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
import me.amlu.authserver.model.AbstractAuditableEntity;
import me.amlu.authserver.oauth2.model.Authority;
import me.amlu.authserver.passkey.model.PasskeyCredential;
import me.amlu.authserver.security.util.EncryptedEmailAddressConverter;
import me.amlu.authserver.security.util.EncryptedPhoneNumberConverter;
import me.amlu.authserver.security.util.EncryptedStringConverter;
import me.amlu.authserver.user.model.vo.AccountStatus;
import me.amlu.authserver.user.model.vo.EmailAddress;
import me.amlu.authserver.user.model.vo.HashedPassword;
import me.amlu.authserver.user.model.vo.PhoneNumber;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Collections; // Ensure this is imported
import java.util.stream.Collectors; // If used by builder or other methods

import static me.amlu.authserver.common.SecurityConstants.MAX_LOGIN_ATTEMPTS;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"), // Or email_blind_index if primary email is encrypted
        @UniqueConstraint(columnNames = "nickname"),
        // For encrypted fields that are unique, the unique constraint should be on the blind index if one exists,
        // or carefully considered if the encrypted value itself can be unique (deterministic encryption without salt/IV, not recommended for PII).
        // If recoveryEmail_encrypted and mobile_number_encrypted are to be unique, and you are using
        // non-deterministic encryption (good for security), you'd need blind indexes for them too for DB-level uniqueness.
        // For now, assuming the @Convert handles the type and the @Column defines the DB column.
        // Uniqueness on encrypted BYTEA columns is tricky.
        @UniqueConstraint(columnNames = "recovery_email_encrypted"),
        @UniqueConstraint(columnNames = "recovery_email_blind_index"),
        @UniqueConstraint(columnNames = "mobile_number_encrypted"),
        @UniqueConstraint(columnNames = "external_id")
}, indexes = {
        @Index(name = "idx_external_id", columnList = "external_id"),
        @Index(name = "idx_email", columnList = "email"), // Or email_blind_index
        @Index(name = "idx_recovery_email", columnList = "recovery_email_encrypted"),
        @Index(name = "idx_recovery_email_blind_index", columnList = "recovery_email_blind_index"),
        @Index(name = "idx_nickname", columnList = "nickname"),
        @Index(name = "idx_mobile_number", columnList = "mobile_number_encrypted"), // Index on the encrypted column
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_updated_at", columnList = "updated_at"),
        @Index(name = "idx_last_login_at", columnList = "last_login_at")
})
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?")
// @Where(clause = "deleted_at IS NULL") // Hibernate 5/6 specific, filters SELECTs
// For JPA standard filtering, you'd typically handle this in repository queries or specifications.
// However, @Where is very convenient if using Hibernate.
// If you want to use a boolean flag:
// @SQLDelete(sql = "UPDATE users SET deleted = true WHERE user_id = ?")
// @Where(clause = "deleted = false")
// private boolean deleted = false;
public class User extends AbstractAuditableEntity implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "external_id", unique = true, updatable = false, nullable = false)
    // Ensure externalId is unique if used as a primary lookup
    private final String externalId; // User handle for WebAuthn.

    @Column(name = "given_name", nullable = false, length = 127)
    private String givenName;

    @Column(name = "middle_Name", nullable = true, length = 127)
    private String middleName;

    @Column(name = "surname", columnDefinition = "BYTEA")
    @Convert(converter = EncryptedStringConverter.class)    // Apply converter
    private String surname;

    @Column(name = "nickname", length = 127) // Nullable, user-defined display name
    private String nickname;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private EmailAddress email;

    @Column(name = "recovery_email_encrypted", columnDefinition = "BYTEA", unique = true, nullable = true)
    @Convert(converter = EncryptedEmailAddressConverter.class) // Apply the converter
    private EmailAddress recoveryEmail; // Renamed from recoveryEmail to better reflect its purpose

    @Column(name = "recovery_email_blind_index", unique = true, nullable = true, length = 64)
    // SHA-256 hex string length
    @JsonIgnore // Don't expose blind index in API responses
    private String recoveryEmailBlindIndex;

    @Column(name = "profile_picture_url", length = 2046)
    private String profilePictureUrl;

    @Column(name = "mobile_number_encrypted", columnDefinition = "BYTEA", unique = true)
    // length is not needed for BYTEA
    @Convert(converter = EncryptedPhoneNumberConverter.class) // Apply the converter
    private PhoneNumber mobileNumber;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<PasskeyCredential> passkeyCredentials = new HashSet<>(); // Initialize to avoid NullPointerExceptions

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password", nullable = true, length = 127))
    @JsonIgnore
    private HashedPassword password;

    @Embedded
    @Access(AccessType.FIELD)
    @AttributeOverrides({
            @AttributeOverride(name = "emailVerified", column = @Column(name = "email_verified", nullable = false)),
            @AttributeOverride(name = "enabled", column = @Column(name = "enabled", nullable = false)),
            @AttributeOverride(name = "failedLoginAttempts", column = @Column(name = "failed_login_attempts")),
            @AttributeOverride(name = "lockoutExpirationTime", column = @Column(name = "lockout_expiration_time")),
            @AttributeOverride(name = "accountNonExpired", column = @Column(name = "account_non_expired", nullable = false)),
//            @AttributeOverride(name = "accountNonLocked", column = @Column(name = "account_non_locked", nullable = false)),
            @AttributeOverride(name = "credentialsNonExpired", column = @Column(name = "credentials_non_expired", nullable = false)),
//            @AttributeOverride(name = "lastLoginTime", column = @Column(name = "last_login_time")),
//            @AttributeOverride(name = "lockedAt", column = @Column(name = "locked_at"))
    })
    private AccountStatus accountStatus;

    /**
     * Last time this user logged in
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Last time this user changed their password
     */
    @Column(name = "last_password_change_date")
    private Instant lastPasswordChangeDate;

    /**
     * Last time this user was deleted
     * Soft delete - The user is still in the database, but is marked as deleted
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Ensure 'authorities' set is initialized by the builder
    @ManyToMany(fetch = FetchType.LAZY)
    // EAGER can be acceptable if the number of roles per user is small and always needed. LAZY is generally safer for performance.
    @JoinTable(
            name = "user_authorities",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id")
    )
    @JsonIgnore
    // Exclude from default toString to avoid issues with lazy loading or verbosity
    private Set<Authority> authorities = new HashSet<>(); // Initialize to avoid NullPointerExceptions

    @Version
    @Column(name = "version")
    private int version;

    private User(Long id, String externalIdParam, String givenName, String middleName, String surname, String nickname,
                 EmailAddress email, EmailAddress recoveryEmail, PhoneNumber mobileNumber,
                 HashedPassword password, AccountStatus accountStatus, Set<Authority> authorities,
                 Set<PasskeyCredential> passkeyCredentials) {
        Assert.hasText(givenName, "User first name cannot be empty.");
        Assert.notNull(email, "User email cannot be null.");

        this.id = id;
        // Use the parameter if provided and valid, otherwise generate a new one.
        this.externalId = (externalIdParam != null && !externalIdParam.isBlank()) ? externalIdParam : User.generateWebAuthnUserHandle();
        this.givenName = givenName;
        this.middleName = middleName;
        this.surname = surname;
        this.nickname = nickname;
        this.email = email;
        this.recoveryEmail = recoveryEmail;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.accountStatus = (accountStatus != null) ? accountStatus : AccountStatus.initial();
        this.authorities = authorities != null ? new HashSet<>(authorities) : new HashSet<>(); // Defensive copy
        this.passkeyCredentials = passkeyCredentials != null ? new HashSet<>(passkeyCredentials) : new HashSet<>(); // Initialize from builder
    }

    /**
     * Required by JPA.
     */
    protected User() {
        // Initialize collections to prevent NullPointerExceptions if JPA creates instance
        // Initialize externalId with a generated value.
        // This ensures the 'final' field contract is met even when JPA uses this constructor.
        this.externalId = User.generateWebAuthnUserHandle();
        this.authorities = new HashSet<>();
        this.passkeyCredentials = new HashSet<>();
        this.accountStatus = AccountStatus.initial(); // Sensible default for JPA-created instances
    }

    /**
     * Static factory method to create a new, minimally initialized user instance.
     * Primarily for scenarios where an entity instance is needed before full population (e.g., by JPA or some repository logic).
     * For fully constructed users, prefer the builder.
     *
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

    /**
     * Records a successful login attempt.
     * Also resets login failures count.
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.accountStatus = this.accountStatus.resetLoginFailures(); // Also reset failures
    }

    public void recordLoginFailure() {
        this.accountStatus = this.accountStatus.recordLoginFailure();
    }

    public void resetLoginFailures() {
        this.accountStatus = this.accountStatus.resetLoginFailures();
    }

    /**
     * * Locks the account for a specified duration.
     * The duration must be positive and non-negative.
     * The account is locked until the specified duration has passed since the current time.
     *
     * @param lockDuration The duration for which the account should be locked.
     * @throws IllegalArgumentException If the lockDuration is null, negative, or zero.
     */
    public void lockAccountFor(Duration lockDuration) {
        Assert.notNull(lockDuration, "Lock duration cannot be null.");
        Assert.isTrue(!lockDuration.isNegative() && !lockDuration.isZero(), "Lock duration must be positive.");
        this.accountStatus = this.accountStatus.lockUntil(Instant.now().plus(lockDuration));
        // @UpdateTimestamp will handle updatedAt
    }

    public void lockAccount(Duration lockDuration) {
        Assert.notNull(lockDuration, "Lock duration cannot be null.");
        Assert.isTrue(!lockDuration.isNegative() && !lockDuration.isZero(), "Lock duration must be positive.");
        this.accountStatus = this.accountStatus.lockUntil(Instant.now().plus(lockDuration));
    }

    public void unlockAccount() {
        this.accountStatus = this.accountStatus.unlock();
    }

    public void enableAccount() {
        this.accountStatus = this.accountStatus.enable();
    }

    public void disableAccount() {
        this.accountStatus = this.accountStatus.disable();
    }

    public boolean isLoginAttemptsExceeded() {
        return this.accountStatus.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS;
    }

    public void updateGivenName(String newGivenName) {
        Assert.hasText(newGivenName, "User given name cannot be blank.");
        this.givenName = newGivenName;
    }

    public void updateMiddleName(String newMiddleName) {
        this.middleName = newMiddleName;
    }

    public void updateSurname(String newSurname) {
        // surname can be null or blank if allowed by business rules
        this.surname = newSurname;
    }

    public void updateNickname(String newNickname) {
        // nickname can be null or blank
        this.nickname = newNickname;
    }

    public void updateEmail(EmailAddress newEmail) {
        Assert.notNull(newEmail, "User email cannot be null.");
        this.email = newEmail;
    }

    private void updateRecoveryEmailInternal(EmailAddress newRecoveryEmail) {
        this.recoveryEmail = newRecoveryEmail;
        if (newRecoveryEmail != null && StringUtils.hasText(newRecoveryEmail.getValue())) {
            this.recoveryEmailBlindIndex = generateBlindIndex(newRecoveryEmail.getValue());
        } else {
            this.recoveryEmailBlindIndex = null;
        }
    }

    public void updateRecoveryEmail(EmailAddress newRecoveryEmail) {
        // Perform existing checks (e.g., not same as primary email)
        if (newRecoveryEmail != null && newRecoveryEmail.equals(this.email)) {
            // log.warn("Attempt to set recovery email same as primary email for userId: {}", this.id);
            throw new IllegalArgumentException("Recovery email cannot be the same as the primary email.");
        }
        // You might also want to check for conflicts with other users' recovery emails here
        // if the blind index is to be unique across all users.
        updateRecoveryEmailInternal(newRecoveryEmail);
    }

    // Helper method to generate a blind index (e.g., SHA-256 hash)
    // IMPORTANT: For consistent querying, this hash MUST be deterministic (same input always yields same output).
    // If you salt this hash, the salt must be consistent for this field across all users,
    // or not used if the goal is just to prevent rainbow tables on the blind index itself.
    // For a simple existence check, an unsalted SHA-256 of the normalized email is common.
    public static String generateBlindIndex(String emailValue) {
        if (emailValue == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(emailValue.toLowerCase().trim().getBytes(StandardCharsets.UTF_8)); // Normalize email
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should not happen with SHA-256
            throw new RuntimeException("Failed to generate blind index", e);
        }
    }

    public void updateProfilePictureUrl(String pictureUrl) {
        if (pictureUrl.length() > 2046) {
            throw new IllegalArgumentException("Profile picture URL cannot exceed 2046 characters.");
        }
        this.profilePictureUrl = Objects.requireNonNull(pictureUrl);
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
        this.lastPasswordChangeDate = Instant.now();
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

    // Generate a random user handle for WebAuthn, externalId
    public static String generateWebAuthnUserHandle() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return new Bytes(bb.array()).toBase64UrlString(); // Use Base64URL
    }

    // In User.UserBuilder or constructor where externalId is set:
    // this.externalId = User.generateWebAuthnUserHandle();

    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.authorities == null) return Collections.emptySet();
        // OLD: This converts to SimpleGrantedAuthority, losing permission details
        // return this.authorities.stream()
        //         .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
        //         .collect(Collectors.toSet());

        // NEW: Return the collection of your custom Authority objects directly
        // as me.amlu.authserver.oauth2.model.Authority implements GrantedAuthority
        return Collections.unmodifiableSet(this.authorities);
    }

    /**
     * Replaces all existing authorities with the given set.
     *
     * @param newAuthorities The new set of authorities. If null or empty, authorities will be cleared.
     */
    public void setAuthorities(Set<Authority> newAuthorities) {
        if (newAuthorities == null) {
            this.authorities = new HashSet<>(); // Assign a new empty HashSet
        } else {
            // The UserDetailsService is already providing a new HashSet of de-proxied entities.
            this.authorities = newAuthorities; // Assign the new Set instance
        }
    }

    // --- UserDetails Implementation ---

    @Override
    public String getPassword() {
        return this.password != null ? this.password.getValue() : null;
    }

    @Override
    public String getUsername() {
        return this.email != null ? this.email.getValue() : null;
    } // Email is the username

    @Override
    public boolean isAccountNonExpired() {
        return this.accountStatus != null && this.accountStatus.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountStatus != null && this.accountStatus.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.accountStatus != null && this.accountStatus.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.accountStatus != null && this.accountStatus.isEnabled();
    }

    // --- equals and hashCode ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false; // Keep this check
        // Error Prone suggests instanceof for the general class check,
        // but for Hibernate proxies, getting the effective class is a common pattern.
        // Let's apply pattern matching where appropriate for the HibernateProxy check.
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User user = (User) o; // Safe cast after class check
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        // Consistent with ID-based equals.
        // For new (transient) entities, Objects.hashCode(null) is 0.
        // Once persisted, the ID is used.
        return Objects.hashCode(getId());
    }

    // --- Getters and Setters ---
    public Long getId() {
        return this.id;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public String getGivenName() {
        return this.givenName;
    }

    public String getMiddleName() {
        return this.middleName;
    }

    public String getSurname() {
        return this.surname;
    }

    public String getNickname() {
        return this.nickname;
    }

    public EmailAddress getEmail() {
        return this.email;
    }

    public EmailAddress getRecoveryEmail() {
        return this.recoveryEmail;
    }

    public String getRecoveryEmailBlindIndex() {
        return this.recoveryEmailBlindIndex;
    }

    /**
     * @deprecated Use {@link #getRecoveryEmail()} instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    public EmailAddress getBackupEmail() {
        return getRecoveryEmail();
    }

    public String getProfilePictureUrl() {
        return this.profilePictureUrl;
    }

    public PhoneNumber getMobileNumber() {
        return this.mobileNumber;
    }

    public Set<PasskeyCredential> getPasskeyCredentials() {
        // If passkeyCredentials can be null before JPA loads it (though unlikely with initialization)
        // if (this.passkeyCredentials == null) {
        //     return Collections.emptySet();
        // }
        return Collections.unmodifiableSet(this.passkeyCredentials);
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    /**
     * Returns a displayable full name, preferring nickname if available,
     * otherwise concatenating first and last name.
     *
     * @return A displayable full name.
     */
    @Transient // Not a persistent field, derived
    public String getDisplayableFullName() {
        if (StringUtils.hasText(this.nickname)) {
            return this.nickname;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(this.givenName)) {
            sb.append(this.givenName);
        }
        if (StringUtils.hasText(this.middleName)) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(this.middleName);
        }
        if (StringUtils.hasText(this.surname)) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(this.surname);
        }
        return sb.isEmpty() ? "" : sb.toString();
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getLastPasswordChangeDate() {
        return lastPasswordChangeDate;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Replaces all existing passkey credentials with the given set.
     * This method is intended for use during user loading to ensure a "clean"
     * set of de-proxied entities is associated with the User principal.
     *
     * @param newPasskeys The new set of passkey credentials. If null or empty, credentials will be cleared.
     */
    public void setPasskeyCredentials(Set<PasskeyCredential> newPasskeys) {
        if (newPasskeys == null) {
            this.passkeyCredentials = new HashSet<>(); // Assign a new empty HashSet
        } else {
            // Ensure we are assigning a new HashSet instance that is not a Hibernate proxy collection
            this.passkeyCredentials = new HashSet<>(newPasskeys);
        }
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Transient
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @Override
    public String toString() {
        // For lazy collections, just indicate presence, or if they are null,
        // rather than trying to get their size, which can trigger lazy loading.
        String passkeyStatus = (passkeyCredentials == null) ? "null_collection" : "[passkey_credentials_present]";
        // Authorities are EAGER, but being cautious is fine.
        String authoritiesStatus = (authorities == null) ? "null_collection" : "[authorities_present_count:" + authorities.size() + "]";


        return "User{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", givenName='" + givenName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", surname='" + surname + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email=" + (email != null ? email.getValue() : "null") +
                ", recoveryEmail=" + (recoveryEmail != null ? recoveryEmail.getValue() : "null") +
                ", mobileNumber=" + (mobileNumber != null ? mobileNumber.e164Value() : "null") +
                ", accountStatus=" + accountStatus +
                ", authorities_status=" + authoritiesStatus + // Changed to status
                ", passkeyCredentials_status=" + passkeyStatus + // Changed to status
                '}';
    }

    public UserBuilder toBuilder() {
        // Ensure all fields managed by the private constructor are included
        return new UserBuilder()
                .id(this.id)
                .externalId(this.externalId)
                .givenName(this.givenName)
                .middleName(this.middleName)
                .surname(this.surname)
                .nickname(this.nickname)
                .email(this.email)
                .recoveryEmail(this.recoveryEmail)
                .mobileNumber(this.mobileNumber)
                .password(this.password)
                .accountStatus(this.accountStatus)
                .authorities(this.authorities != null ? new HashSet<>(this.authorities) : null) // Pass a copy
                .passkeyCredentials(this.passkeyCredentials != null ? new HashSet<>(this.passkeyCredentials) : null); // Pass a copy
        // externalId and displayName are not part of the private constructor,
        // so they are not set by this toBuilder() directly into the builder's fields
        // for that constructor. If they were, they'd be added here.
        // The built User would then have its public externalId/displayName fields set separately if needed.
    }

    public static class UserBuilder {
        private Long id;
        private String externalId;
        private String givenName;
        private String middleName;
        private String surname;
        private String nickname;
        private EmailAddress email;
        private EmailAddress recoveryEmail;
        private PhoneNumber mobileNumber;
        private HashedPassword password;
        private AccountStatus accountStatus;
        private Set<Authority> authorities$value;
        private boolean authorities$set;
        private Set<PasskeyCredential> passkeyCredentials$value; // Added
        private boolean passkeyCredentials$set; // Added

        // Note: externalId and displayName are not managed by this builder directly
        // as they are not in the private User constructor's parameters.
        // They would be set on the User object after it's built, if needed.

        UserBuilder() {
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public UserBuilder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public UserBuilder middleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public UserBuilder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder email(EmailAddress email) {
            this.email = email;
            return this;
        }


        /**
         * @param recoveryEmail The recovery email address for the user
         * @return This builder for method chaining
         */
        public UserBuilder recoveryEmail(EmailAddress recoveryEmail) {
//            return recoveryEmail(recoveryEmail);
            this.recoveryEmail = recoveryEmail;
            return this;
        }

        public UserBuilder mobileNumber(PhoneNumber mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        @JsonIgnore
        public UserBuilder password(HashedPassword password) {
            this.password = password;
            return this;
        }

        public UserBuilder accountStatus(AccountStatus accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        @JsonIgnore
        public UserBuilder authorities(Set<Authority> authorities) {
            this.authorities$value = authorities;
            this.authorities$set = true;
            return this;
        }

        @JsonIgnore
        public UserBuilder passkeyCredentials(Set<PasskeyCredential> passkeyCredentials) {
            this.passkeyCredentials$value = passkeyCredentials;
            this.passkeyCredentials$set = true;
            return this;
        }


        public User build() {
            Set<Authority> authoritiesValue = this.authorities$value;
            if (!this.authorities$set) {
                authoritiesValue = User.$default$authorities();
            }
            // Handle passkeyCredentials default
            Set<PasskeyCredential> passkeyCredentialsValue = this.passkeyCredentials$value;
            if (!this.passkeyCredentials$set) {
                passkeyCredentialsValue = new HashSet<>(); // Default to empty set
            }

            User user = new User(this.id, this.externalId, this.givenName, this.middleName, this.surname, this.nickname,
                    this.email, this.recoveryEmail, this.mobileNumber,
                    this.password, this.accountStatus, authoritiesValue,
                    passkeyCredentialsValue);
            // Set blind index after construction if recoveryEmail is present
            if (this.recoveryEmail != null && StringUtils.hasText(this.recoveryEmail.getValue())) {
                user.recoveryEmailBlindIndex = generateBlindIndex(this.recoveryEmail.getValue());
            }
            return user;
        }

        @Override
        public String toString() {
            return "User.UserBuilder(id=" + this.id +
                    ", externalId=" + this.externalId +
                    ", givenName=" + this.givenName +
                    ", middleName=" + this.middleName +
                    ", surname=" + this.surname +
                    ", nickname=" + this.nickname +
                    ", email=" + this.email +
                    ", recoveryEmail=" + this.recoveryEmail +
                    ", mobileNumber=" + this.mobileNumber +
                    ", password=" + (password != null ? "[PROTECTED]" : "null") +
                    ", accountStatus=" + this.accountStatus +
                    ", authorities$value=" + this.authorities$value +
                    ", passkeyCredentials$value=" + (this.passkeyCredentials$value != null ? this.passkeyCredentials$value.size() : "null") + // Log size
                    ")";
        }
    }
}

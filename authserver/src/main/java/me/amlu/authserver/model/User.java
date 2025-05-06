package me.amlu.authserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import me.amlu.authserver.model.oauth2.Authority;
import me.amlu.authserver.model.vo.AccountStatus;
import me.amlu.authserver.model.vo.EmailAddress;
import me.amlu.authserver.model.vo.HashedPassword;
import me.amlu.authserver.model.vo.PhoneNumber;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", uniqueConstraints = { // Added unique constraints for email and mobile_number if desired
        @UniqueConstraint(columnNames = "email"),
        // Consider if mobile_number should truly be unique globally or per user context
        // For authserver, unique email is primary. Unique mobile can be a business rule.
        @UniqueConstraint(columnNames = "mobile_number")
})
// @Setter // Setters are intentionally limited, modifications via behavioral methods
// toBuilder=true allows creating a builder from an existing instance
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false) // Assuming name is required
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true, length = 254))
    private EmailAddress email;

    @Embedded
    @AttributeOverride(name = "e164Value", column = @Column(name = "mobile_number", length = 20, unique = true))
    // Map 'e164Value' from PhoneNumber VO
    private PhoneNumber mobileNumber; // Changed type to PhoneNumber

    // @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Handled by service layer if needed
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password", nullable = false, length = 128))
    @JsonIgnore
    private HashedPassword password;

    @Embedded
    private AccountStatus accountStatus;

    @CreationTimestamp // Automatically set on creation
    @Column(name = "created_at", nullable = false, updatable = false) // Standard auditing field
    @JsonIgnore
    private Instant createdAt; // Using java.time.Instant for timestamp

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private Instant updatedAt;

    // Ensure authorities is initialized by the builder
    @ManyToMany(fetch = FetchType.EAGER)
    // EAGER can be acceptable if the number of roles per user is small and always needed. LAZY is generally safer for performance.
    @JoinTable(
            name = "user_authorities", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"), // FK in join table to users table
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id") // FK in join table to authorities table
    )
    @JsonIgnore
            // Exclude from default toString to avoid issues with lazy loading or verbosity
    private Set<Authority> authorities = new HashSet<>();

    // Private constructor for Lombok's @Builder to use.
    // This allows for validation or default initializations if @Builder.Default isn't sufficient for all fields.
    // Lombok's @Builder will generate an all-args constructor if one isn't explicitly defined
    // or if this one doesn't match all non-@Builder.Default fields.
    // For this setup with @Builder.Default on 'authorities', this constructor should list other fields.
    private User(Long id, String name, EmailAddress email, PhoneNumber mobileNumber,
                 HashedPassword password, AccountStatus accountStatus, Instant createdAt, Instant updatedAt, Set<Authority> authorities) {
        Assert.hasText(name, "User name cannot be empty.");
        Assert.notNull(email, "User email cannot be null.");
        Assert.notNull(password, "User password cannot be null.");
        // mobileNumber can be null

        this.id = id;
        this.name = name;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.password = password;
        // Initialize accountStatus if null (e.g. when builder doesn't set it)
        this.accountStatus = (accountStatus != null) ? accountStatus : AccountStatus.initial();
        this.createdAt = createdAt; // Will be set by @CreationTimestamp if new and null
        this.updatedAt = updatedAt; // Will be set by @UpdateTimestamp
        this.authorities = authorities != null ? new HashSet<>(authorities) : new HashSet<>(); // Defensive copy
    }

    protected User() {
    }

    private static Set<Authority> $default$authorities() {
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
        return this.accountStatus.getFailedLoginAttempts() >= AccountStatus.DEFAULT_MAX_FAILED_ATTEMPTS;
    }

    /**
     * Updates the user's display name.
     *
     * @param newName The new name for the user. Must not be blank.
     */
    public void updateName(String newName) {
        Assert.hasText(newName, "User name cannot be blank.");
        this.name = newName;
        // @UpdateTimestamp will handle updatedAt
    }

    /**
     * Changes the user's password.
     * The new password must already be hashed.
     *
     * @param newHashedPassword The new, hashed password. Must not be null.
     */
    public void changePassword(HashedPassword newHashedPassword) {
        Objects.requireNonNull(newHashedPassword, "New hashed password cannot be null.");
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
            // Ensure authorities set is initialized (though @Builder.Default should handle this)
            if (this.authorities == null) {
                this.authorities = new HashSet<>();
            }
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
        if (authority != null && this.authorities != null) {
            this.authorities.remove(authority);
            // @UpdateTimestamp will handle updatedAt
        }
    }

    /**
     * Replaces all existing authorities with the given set.
     *
     * @param newAuthorities The new set of authorities. If null or empty, authorities will be cleared.
     */
    public void setAuthorities(Set<Authority> newAuthorities) {
        if (this.authorities == null) {
            this.authorities = new HashSet<>();
        } else {
            this.authorities.clear();
        }
        if (newAuthorities != null) {
            this.authorities.addAll(newAuthorities);
        }
        // @UpdateTimestamp will handle updatedAt
    }

    // --- UserDetails Implementation ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Ensure authorities set is not null before streaming
        if (this.authorities == null) {
            return Collections.emptySet();
        }
        return this.authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority())) // Assuming Authority has getName()
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() { // From UserDetails
        return this.password != null ? this.password.getValue() : null;
    }

    @Override
    public String getUsername() { // From UserDetails - typically email for authserver
        return this.email != null ? this.email.getValue() : null;
    }

    @Override
    public boolean isAccountNonExpired() { // From UserDetails
        return this.accountStatus != null && this.accountStatus.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() { // From UserDetails
        return this.accountStatus != null && this.accountStatus.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() { // From UserDetails
        return this.accountStatus != null && this.accountStatus.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() { // From UserDetails
        return this.accountStatus != null && this.accountStatus.isEnabled();
    }

    // --- equals and hashCode (ID-based, proxy-aware) ---
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

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public EmailAddress getEmail() {
        return this.email;
    }

    public PhoneNumber getMobileNumber() {
        return this.mobileNumber;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public String toString() {
        return "User(id=" + this.getId() + ", name=" + this.getName() + ", email=" + this.getEmail() + ", mobileNumber=" + this.getMobileNumber() + ", password=" + this.getPassword() + ", accountStatus=" + this.getAccountStatus() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }

    public UserBuilder toBuilder() {
        return new UserBuilder().id(this.id).name(this.name).email(this.email).mobileNumber(this.mobileNumber).password(this.password).accountStatus(this.accountStatus).createdAt(this.createdAt).updatedAt(this.updatedAt).authorities(this.authorities);
    }

    public static class UserBuilder {
        private Long id;
        private String name;
        private EmailAddress email;
        private PhoneNumber mobileNumber;
        private HashedPassword password;
        private AccountStatus accountStatus;
        private Instant createdAt;
        private Instant updatedAt;
        private Set<Authority> authorities$value;
        private boolean authorities$set;

        UserBuilder() {
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder email(EmailAddress email) {
            this.email = email;
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
        public UserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @JsonIgnore
        public UserBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @JsonIgnore
        public UserBuilder authorities(Set<Authority> authorities) {
            this.authorities$value = authorities;
            this.authorities$set = true;
            return this;
        }

        public User build() {
            Set<Authority> authorities$value = this.authorities$value;
            if (!this.authorities$set) {
                authorities$value = User.$default$authorities();
            }
            return new User(this.id, this.name, this.email, this.mobileNumber, this.password, this.accountStatus, this.createdAt, this.updatedAt, authorities$value);
        }

        public String toString() {
            return "User.UserBuilder(id=" + this.id + ", name=" + this.name + ", email=" + this.email + ", mobileNumber=" + this.mobileNumber + ", password=" + this.password + ", accountStatus=" + this.accountStatus + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ", authorities$value=" + this.authorities$value + ")";
        }
    }
}
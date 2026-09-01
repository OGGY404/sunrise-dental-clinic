package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A member of clinic staff who can log in (FR1 authentication, FR7 roles).
 *
 * IMPORTANT: this class never holds the real password. It holds a BCrypt hash,
 * which is a one-way scramble. Even someone who steals the database cannot read
 * the passwords back out of it.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt hash, always 60 characters. Never the password itself.
     *
     * The column is CHAR(60), not VARCHAR(60), because a BCrypt hash is always
     * exactly that long, so there is no reason to store a length with it.
     * columnDefinition has to say so, otherwise Hibernate expects VARCHAR and
     * refuses to start against the real MySQL database.
     */
    @Column(name = "password_hash", nullable = false, length = 60, columnDefinition = "CHAR(60)")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", length = 120)
    private String email;

    /**
     * EnumType.STRING stores the word ADMIN in the column, not the number 0.
     * If someone later adds a role in the middle of the list, the existing rows
     * still mean what they always meant.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.RECEPTIONIST;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Filled in by Hibernate when the row is first saved. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA needs a constructor with no arguments. */
    public User() {
    }

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Two users are the same person when they have the same username. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        return Objects.equals(username, ((User) other).username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    /** Deliberately does not print the password hash. */
    @Override
    public String toString() {
        return "User{username=" + username + ", fullName=" + fullName + ", role=" + role + "}";
    }
}

package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Role;
import lk.icbt.cis6003.dentalclinic.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the staff login table (FR1 - authentication).
 *
 * The login screen has to answer one question: "is there an enabled account with
 * this username?" These tests prove the repository can answer it.
 */
@DisplayName("UserRepository")
class UserRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("finds a saved user by username")
    void findsUserByUsername() {
        userRepository.save(newUser("admin", Role.ADMIN));

        Optional<User> found = userRepository.findByUsername("admin");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Test admin");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("returns empty when the username does not exist")
    void returnsEmptyForUnknownUsername() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("never stores the password in plain text")
    void storesOnlyABcryptHash() {
        User saved = userRepository.save(newUser("reception", Role.RECEPTIONIST));

        // A BCrypt hash always starts with $2 and is 60 characters long.
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getPasswordHash()).hasSize(60);
        assertThat(saved.getPasswordHash()).doesNotContain("Admin@123");
    }

    @Test
    @DisplayName("ignores a disabled account when looking for someone who may log in")
    void skipsDisabledAccounts() {
        User leaver = newUser("oldstaff", Role.RECEPTIONIST);
        leaver.setEnabled(false);
        userRepository.save(leaver);

        assertThat(userRepository.findByUsernameAndEnabledTrue("oldstaff")).isEmpty();
        // The row is still there for the audit trail, it just cannot log in.
        assertThat(userRepository.findByUsername("oldstaff")).isPresent();
    }

    @Test
    @DisplayName("refuses two accounts with the same username")
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(newUser("admin", Role.ADMIN));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("admin", Role.RECEPTIONIST)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("sets created_at automatically when a user is saved")
    void setsCreatedAtAutomatically() {
        User saved = userRepository.saveAndFlush(newUser("newstaff", Role.RECEPTIONIST));

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

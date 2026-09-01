package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.Role;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for the class that hands a staff account to Spring Security (FR1).
 *
 * This is the join between our users table and the security framework. It is
 * worth testing on its own because two details are easy to get wrong and both
 * fail quietly: the ROLE_ prefix, and what happens to a disabled account.
 */
@DisplayName("ClinicUserDetailsService")
@ExtendWith(MockitoExtension.class)
class ClinicUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private ClinicUserDetailsService service;

    /** A real BCrypt hash of "Admin@123", the same one data.sql seeds. */
    private static final String HASH =
            "$2b$10$3CG1Xb9LPd/ERcwjWy5gSe1CIFtttVsTc4FYD2lcjzsHClY/WLjj2";

    @BeforeEach
    void setUp() {
        service = new ClinicUserDetailsService(userRepository);
    }

    private User staff(String username, Role role, boolean enabled) {
        User user = new User();
        user.setUserId(1L);
        user.setUsername(username);
        user.setPasswordHash(HASH);
        user.setFullName("Test " + username);
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    @DisplayName("hands over the stored hash, never a plain password")
    void passesTheStoredHashThrough() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(staff("admin", Role.ADMIN, true)));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("adds the ROLE_ prefix Spring Security expects")
    void addsTheRolePrefix() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(staff("admin", Role.ADMIN, true)));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("a receptionist gets ROLE_RECEPTIONIST and nothing more")
    void receptionistGetsOnlyTheirOwnRole() {
        when(userRepository.findByUsername("reception"))
                .thenReturn(Optional.of(staff("reception", Role.RECEPTIONIST, true)));

        UserDetails details = service.loadUserByUsername("reception");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RECEPTIONIST");
    }

    @Test
    @DisplayName("an account that has been switched off cannot log in")
    void disabledAccountIsMarkedDisabled() {
        when(userRepository.findByUsername("oldstaff"))
                .thenReturn(Optional.of(staff("oldstaff", Role.RECEPTIONIST, false)));

        UserDetails details = service.loadUserByUsername("oldstaff");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("an unknown username is refused")
    void unknownUsernameIsRefused() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("the refusal message never says whether the name exists")
    void refusalDoesNotLeakWhetherTheAccountExists() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .hasMessageNotContaining("nobody")
                .hasMessageContaining("Bad credentials");
    }
}

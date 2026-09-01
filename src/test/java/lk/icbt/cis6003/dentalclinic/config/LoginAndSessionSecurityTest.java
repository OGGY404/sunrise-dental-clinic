package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.Role;
import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.result.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.result.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for login, sessions and role based access (FR1, and FR7 roles).
 *
 * The whole application is started for these, because security is made of
 * filters that sit in front of the controllers. Testing a controller on its own
 * would never prove that an unauthenticated request is actually stopped.
 *
 * WHY THE TWO KINDS OF REFUSAL ARE TESTED SEPARATELY
 * A person browsing to a clinic page should be sent to the login screen. A
 * program calling the web service should get 401 and a JSON message, because a
 * login page would be meaningless to it. The configuration does both, and the
 * tests below pin each one down.
 */
@DisplayName("Login, sessions and roles")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginAndSessionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createStaffAccounts() {
        userRepository.deleteAll();
        userRepository.save(staff("admin", "Admin@123", Role.ADMIN, true));
        userRepository.save(staff("reception", "Recep@123", Role.RECEPTIONIST, true));
        userRepository.save(staff("oldstaff", "Recep@123", Role.RECEPTIONIST, false));
    }

    private User staff(String username, String password, Role role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName("Test " + username);
        user.setEmail(username + "@sunrisedental.lk");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Nested
    @DisplayName("before logging in")
    class BeforeLogin {

        @Test
        @DisplayName("a person browsing to a clinic page is sent to the login screen")
        void browserIsSentToLogin() throws Exception {
            mockMvc.perform(get("/appointments"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }

        @Test
        @DisplayName("a call to the web service gets 401 and a JSON message, not a login page")
        void webServiceGets401() throws Exception {
            mockMvc.perform(get("/api/appointments/APT-20260907-0001"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("the login page itself is reachable without logging in")
        void loginPageIsOpen() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("logging in")
    class LoggingIn {

        @Test
        @DisplayName("the right password logs the receptionist in")
        void correctPasswordLogsIn() throws Exception {
            mockMvc.perform(formLogin("/login").user("reception").password("Recep@123"))
                    .andExpect(authenticated().withRoles("RECEPTIONIST"))
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("a wrong password is refused")
        void wrongPasswordIsRefused() throws Exception {
            mockMvc.perform(formLogin("/login").user("reception").password("wrong"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("an unknown username is refused the same way as a wrong password")
        void unknownUsernameIsRefused() throws Exception {
            mockMvc.perform(formLogin("/login").user("nobody").password("Recep@123"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("an account that has been switched off cannot log in")
        void disabledAccountIsRefused() throws Exception {
            mockMvc.perform(formLogin("/login").user("oldstaff").password("Recep@123"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("the time of the login is recorded against the account")
        void recordsTheLoginTime() throws Exception {
            assertThat(userRepository.findByUsername("reception"))
                    .get().extracting(User::getLastLoginAt).isNull();

            mockMvc.perform(formLogin("/login").user("reception").password("Recep@123"))
                    .andExpect(authenticated());

            assertThat(userRepository.findByUsername("reception"))
                    .get().extracting(User::getLastLoginAt).isNotNull();
        }

        @Test
        @DisplayName("logging out ends the session")
        void logoutEndsTheSession() throws Exception {
            mockMvc.perform(logout())
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?logout"));
        }
    }

    @Nested
    @DisplayName("after logging in")
    class AfterLogin {

        @Test
        @DisplayName("a receptionist can use the booking web services")
        @WithMockUser(username = "reception", roles = "RECEPTIONIST")
        void receptionistCanReadReferenceData() throws Exception {
            mockMvc.perform(get("/api/treatments"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("a receptionist is refused the admin only area")
        @WithMockUser(username = "reception", roles = "RECEPTIONIST")
        void receptionistIsRefusedAdminArea() throws Exception {
            mockMvc.perform(get("/api/admin/settings"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("an admin is allowed into the admin only area")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void adminIsAllowedIntoAdminArea() throws Exception {
            // Nothing is mapped there yet, so 404 is the right answer. What
            // matters is that it is not 403: the role rule let this one past.
            mockMvc.perform(get("/api/admin/settings"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("cross site request forgery protection")
    class CsrfProtection {

        @Test
        @DisplayName("a write without the CSRF token is refused, even when logged in")
        @WithMockUser(username = "reception", roles = "RECEPTIONIST")
        void writeWithoutTokenIsRefused() throws Exception {
            mockMvc.perform(post("/api/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("a read needs no token, because reading changes nothing")
        @WithMockUser(username = "reception", roles = "RECEPTIONIST")
        void readNeedsNoToken() throws Exception {
            mockMvc.perform(get("/api/dentists"))
                    .andExpect(status().isOk());
        }
    }
}

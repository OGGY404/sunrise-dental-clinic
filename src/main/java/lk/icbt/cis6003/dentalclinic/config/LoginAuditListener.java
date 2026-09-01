package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Writes down when each member of staff logged in, and when someone failed to.
 *
 * WHY AN EVENT LISTENER AND NOT CODE IN THE LOGIN PAGE
 * Spring Security announces every success and failure as an application event.
 * Listening for the event means this works for the login form today and for any
 * other way of signing in that is added later, without that code being touched.
 * It also keeps the record-keeping out of the security configuration, which
 * stays about rules only.
 *
 * WHY FAILURES ARE ONLY LOGGED, NOT STORED
 * A failed login has no account to attach itself to, since the username may not
 * exist at all. Writing a row for every failed attempt would also let anyone
 * fill the database by guessing passwords. The log is the right place for it,
 * and it is what an administrator would read after a suspected attack.
 */
@Component
public class LoginAuditListener {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditListener.class);

    private final UserRepository userRepository;

    public LoginAuditListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Records the moment of a successful login against the account. */
    @EventListener
    @Transactional
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();

        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("{} signed in", user.getUsername());
        });
    }

    /**
     * Notes a refused login.
     *
     * The username is written to the log but the password never is, not even
     * when it was wrong, because people reuse passwords and a log file is read
     * by more people than a database is.
     */
    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Object attemptedName = event.getAuthentication().getName();
        log.warn("Refused login for {}: {}", attemptedName, event.getException().getMessage());
    }
}

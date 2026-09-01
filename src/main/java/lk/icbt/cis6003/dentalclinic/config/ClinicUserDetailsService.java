package lk.icbt.cis6003.dentalclinic.config;

import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hands a staff account to Spring Security at login time (FR1).
 *
 * This is the join between our own users table and the security framework.
 * Spring Security does not know what a Sunrise Dental user is; it only knows
 * its own UserDetails type. This class translates one into the other.
 *
 * TWO SMALL DETAILS THAT MATTER
 *
 * 1. The ROLE_ prefix. Our database column stores ADMIN and RECEPTIONIST,
 *    because that is easier to read in MySQL Workbench. Spring Security expects
 *    ROLE_ADMIN and ROLE_RECEPTIONIST. The prefix is added here, in one place.
 *    If it were forgotten, every hasRole() check would silently fail closed and
 *    nobody could do anything.
 *
 * 2. The password is never compared here. This class only produces the stored
 *    hash. Spring Security compares it using BCrypt, in constant time, which is
 *    what stops an attacker learning the password by timing the answer.
 */
@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ClinicUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks the account up by the name typed on the login screen.
     *
     * WHY THE MESSAGE IS ALWAYS "Bad credentials"
     * Saying "no such user" would tell an attacker which usernames exist, so
     * they could stop guessing names and start guessing passwords. A wrong
     * username and a wrong password must be indistinguishable from outside.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name())))
                .disabled(!user.isEnabled())
                .build();
    }
}

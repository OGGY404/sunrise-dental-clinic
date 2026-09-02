package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for staff logins (FR1).
 *
 * DESIGN PATTERN: Repository (also called DAO).
 * The rest of the program asks for a user by name and gets an object back. It
 * never sees a SQL statement, a connection or a result set. That is what lets
 * the business tier be tested with a fake repository, and it is what keeps the
 * three tiers genuinely separate rather than separate in name only.
 *
 * Spring Data writes the SQL from the method name at start-up, so there is no
 * hand-written query to get wrong. findByUsername becomes
 * "select ... from users where username = ?".
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Any account with this username, enabled or not.
     * Optional is used instead of returning null, so the caller is forced to
     * deal with the case where nobody matches.
     */
    Optional<User> findByUsername(String username);

    /**
     * Only an account that is still allowed to log in.
     *
     * A member of staff who leaves is disabled, not deleted, because past
     * appointments and bills still point at them. The login screen must use
     * this method, not the one above.
     */
    Optional<User> findByUsernameAndEnabledTrue(String username);

    boolean existsByUsername(String username);
}

package lk.icbt.cis6003.dentalclinic.controller;

import lk.icbt.cis6003.dentalclinic.model.User;
import lk.icbt.cis6003.dentalclinic.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Optional;

/**
 * Turns the logged-in web user into the staff record the business tier stores.
 *
 * WHY THIS IS NEEDED
 * Spring Security knows the logged-in person only as a name. The appointment
 * and bill tables record which member of staff acted, and for that a real User
 * row is needed. This class is the one place that crosses between the two.
 *
 * WHY IT RETURNS AN OPTIONAL INSTEAD OF THROWING
 * Recording who booked a visit is useful, not essential. If the account has
 * been removed since the person logged in, the booking must still go through,
 * with the staff column simply left empty. Refusing a patient an appointment
 * because of a bookkeeping detail would be the wrong trade.
 *
 * It sits in the controller package on purpose: it is about the web request,
 * not about clinic rules, so it does not belong in the business tier.
 */
@Component
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The staff record for whoever sent this request, if there still is one. */
    @Transactional(readOnly = true)
    public Optional<User> resolve(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(principal.getName());
    }
}

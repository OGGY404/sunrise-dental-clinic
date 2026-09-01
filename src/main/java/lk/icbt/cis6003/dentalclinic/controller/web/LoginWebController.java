package lk.icbt.cis6003.dentalclinic.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shows the clinic's own login screen (FR1).
 *
 * Up to step 6 Spring Security drew a plain login page of its own. This
 * replaces it with the clinic's page, so staff see the same design from the
 * first screen onwards.
 *
 * There is no method for handling the login itself. Spring Security receives
 * the POST, checks the password and creates the session; a controller method
 * here would only be able to do it worse, and would be a second place where
 * passwords are handled.
 */
@Controller
public class LoginWebController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

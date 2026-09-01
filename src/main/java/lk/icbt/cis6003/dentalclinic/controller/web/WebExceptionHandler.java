package lk.icbt.cis6003.dentalclinic.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import lk.icbt.cis6003.dentalclinic.exception.ClinicException;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Turns a problem into a page a member of staff can read.
 *
 * WHY THERE ARE TWO EXCEPTION HANDLERS IN THIS PROJECT
 * GlobalExceptionHandler answers the web services with JSON, which is right for
 * a program. A receptionist looking at a screen must not be shown JSON, so the
 * screens get their own handler, which renders the clinic's error page with a
 * sentence and a way back.
 *
 * The two are kept apart by package: this one assists only the controllers in
 * controller.web, and the other only classes annotated with @RestController.
 * Without that split, whichever handler Spring found first would answer for
 * both, and one of the two audiences would get the wrong thing.
 *
 * MOST PROBLEMS NEVER REACH HERE
 * The controllers deal with the expected refusals themselves, because staying
 * on the form with the message at the top is far more useful than an error
 * page. What is left for this class is the genuinely unexpected: a mistyped web
 * address, or a real bug.
 */
@ControllerAdvice(basePackages = "lk.icbt.cis6003.dentalclinic.controller.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    /** A reference number that does not exist. */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException problem, Model model) {
        model.addAttribute("heading", "We could not find that");
        model.addAttribute("message", problem.getMessage());
        return "error";
    }

    /** A clinic rule refuses this, or the request did not make sense. */
    @ExceptionHandler(ClinicException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleClinicProblem(ClinicException problem, Model model) {
        model.addAttribute("heading", "That cannot be done");
        model.addAttribute("message", problem.getMessage());
        return "error";
    }

    /** The person is signed in, but this is not theirs to open. */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleNotAllowed(Model model) {
        model.addAttribute("heading", "Not allowed");
        model.addAttribute("message",
                "Your account does not have permission to open that screen. "
                        + "If you need it, ask an administrator.");
        return "error";
    }

    /**
     * A real bug.
     *
     * The details go to the log where a developer can find them. The screen
     * gets a plain sentence, because a stack trace on a monitor a patient may
     * be standing in front of helps nobody and shows how the system is built.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAnythingElse(Exception problem, HttpServletRequest request, Model model) {
        log.error("Unexpected failure while showing {}", request.getRequestURI(), problem);

        model.addAttribute("heading", "Something went wrong");
        model.addAttribute("message",
                "Something went wrong at our end. Please try again, and tell the system "
                        + "administrator if it keeps happening.");
        return "error";
    }
}

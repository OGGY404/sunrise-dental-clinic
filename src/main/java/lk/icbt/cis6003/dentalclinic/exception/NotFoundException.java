package lk.icbt.cis6003.dentalclinic.exception;

/**
 * Something was asked for by name or number and does not exist, for example an
 * appointment number that was typed wrongly.
 *
 * The web layer turns this into an HTTP 404 and a "we could not find that"
 * message, rather than an error page.
 */
public class NotFoundException extends ClinicException {

    public NotFoundException(String message) {
        super(message);
    }

    /** Builds the usual wording, so every message reads the same way. */
    public static NotFoundException of(String what, String reference) {
        return new NotFoundException("No " + what + " was found with the reference " + reference + ".");
    }
}

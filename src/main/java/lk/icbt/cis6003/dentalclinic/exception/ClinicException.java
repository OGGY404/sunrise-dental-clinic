package lk.icbt.cis6003.dentalclinic.exception;

/**
 * The base type for every problem this system reports to a member of staff.
 *
 * WHY THERE IS A BASE TYPE
 * The web layer needs one place to turn a business problem into a friendly page
 * rather than a stack trace. Because every one of our exceptions extends this,
 * a single handler can catch ClinicException and show its message, while a
 * genuine bug (a NullPointerException, say) still surfaces as an error the
 * developer must fix.
 *
 * These are unchecked exceptions on purpose. A receptionist choosing a slot
 * that has just been taken is not something the calling code can recover from
 * by retrying; the message has to reach the screen.
 */
public class ClinicException extends RuntimeException {

    public ClinicException(String message) {
        super(message);
    }

    public ClinicException(String message, Throwable cause) {
        super(message, cause);
    }
}

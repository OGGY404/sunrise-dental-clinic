package lk.icbt.cis6003.dentalclinic.exception;

/**
 * The chosen dentist is already booked at that date and time.
 *
 * This has its own type because it is the one error the receptionist will hit
 * most often, and the screen answers it differently from other problems: it
 * offers the next free slot instead of just refusing.
 */
public class SlotUnavailableException extends ClinicException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}

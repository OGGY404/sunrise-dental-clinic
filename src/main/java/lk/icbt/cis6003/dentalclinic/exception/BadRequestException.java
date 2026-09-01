package lk.icbt.cis6003.dentalclinic.exception;

/**
 * The request itself does not make sense, for example a patient search with
 * nothing to search for.
 *
 * This is different from BusinessRuleException. A business rule means the
 * request was understood and the clinic refuses it. This means the request
 * could not be understood in the first place, so the web layer answers 400
 * rather than 422.
 */
public class BadRequestException extends ClinicException {

    public BadRequestException(String message) {
        super(message);
    }
}

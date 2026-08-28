package lk.icbt.cis6003.dentalclinic.exception;

/**
 * A clinic rule says this cannot be done, for example billing a visit that has
 * been cancelled, or booking outside opening hours.
 *
 * The message is written for the receptionist, not for a developer, because it
 * is shown to them word for word.
 */
public class BusinessRuleException extends ClinicException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

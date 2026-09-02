package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.constraints.NotNull;
import lk.icbt.cis6003.dentalclinic.model.PaymentMethod;

/**
 * How a bill was settled (FR4).
 *
 * The method is an enum, so the only values the web service will accept are
 * CASH, CARD and INSURANCE. Anything else is refused before the business tier
 * is reached, and the database column is the same three values, so the two can
 * never disagree.
 */
public class PaymentRequest {

    @NotNull(message = "Please record how the bill was paid.")
    private PaymentMethod method;

    public PaymentRequest() {
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }
}

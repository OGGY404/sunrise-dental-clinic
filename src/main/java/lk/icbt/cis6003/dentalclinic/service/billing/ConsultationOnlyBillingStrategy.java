package lk.icbt.cis6003.dentalclinic.service.billing;

import java.math.BigDecimal;

/**
 * The rule for a visit that IS the consultation, such as a check-up or a braces
 * assessment.
 *
 * DESIGN PATTERN: Strategy.
 *
 * WHY THE FEE IS WAIVED
 * The listed price of a check-up already covers the dentist looking at the
 * patient. Adding the consultation fee on top would charge for the same thing
 * twice, which the patient would notice and rightly complain about. The old
 * paper system had exactly this problem, because whoever wrote the bill decided
 * for themselves.
 */
public class ConsultationOnlyBillingStrategy extends AbstractBillingStrategy {

    @Override
    public String getName() {
        return "Consultation only";
    }

    @Override
    protected BigDecimal treatmentCostFor(BillingContext context) {
        return context.getTreatment().getCost();
    }

    @Override
    protected BigDecimal consultationFeeFor(BillingContext context) {
        // Deliberately zero. See the note above.
        return BigDecimal.ZERO;
    }

    @Override
    protected String baseExplanation(BillingContext context) {
        return "Consultation charge: " + context.getTreatment().getName()
                + ". No separate consultation fee is added, because this visit is the consultation.";
    }
}

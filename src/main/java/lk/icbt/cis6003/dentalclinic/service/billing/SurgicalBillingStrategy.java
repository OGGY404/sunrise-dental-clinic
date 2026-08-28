package lk.icbt.cis6003.dentalclinic.service.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The rule for surgery: the price on the list plus a sterilisation supplement,
 * and then the consultation fee.
 *
 * DESIGN PATTERN: Strategy.
 *
 * WHY THE SUPPLEMENT EXISTS
 * A surgical procedure uses single-use instruments and needs the room sterilised
 * afterwards. That cost is real, and the clinic recovers it as a percentage of
 * the procedure rather than as a flat fee, because a longer operation uses more
 * of everything.
 */
public class SurgicalBillingStrategy extends AbstractBillingStrategy {

    /** 15 percent on top of the listed price, for sterilisation and supplies. */
    private static final BigDecimal STERILISATION_RATE = new BigDecimal("0.15");

    @Override
    public String getName() {
        return "Surgical";
    }

    @Override
    protected BigDecimal treatmentCostFor(BillingContext context) {
        BigDecimal listed = context.getTreatment().getCost();
        BigDecimal supplement = listed.multiply(STERILISATION_RATE).setScale(2, RoundingMode.HALF_UP);
        return listed.add(supplement);
    }

    @Override
    protected BigDecimal consultationFeeFor(BillingContext context) {
        return context.getConsultationFee();
    }

    @Override
    protected String baseExplanation(BillingContext context) {
        return "Surgical charge: the price of " + context.getTreatment().getName()
                + " plus a 15% sterilisation and supplies supplement, plus the consultation fee.";
    }
}

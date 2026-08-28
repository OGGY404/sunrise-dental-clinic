package lk.icbt.cis6003.dentalclinic.service.billing;

import java.math.BigDecimal;

/**
 * The ordinary rule: the price on the list, plus the consultation fee.
 *
 * DESIGN PATTERN: Strategy (one concrete rule), built on the Template Method in
 * AbstractBillingStrategy.
 *
 * This is what most treatments use, and it is the fallback the factory returns
 * for any treatment it does not recognise. Having a sensible default means a
 * treatment added to the price list next year still bills correctly, instead of
 * throwing an error at the front desk.
 */
public class StandardBillingStrategy extends AbstractBillingStrategy {

    @Override
    public String getName() {
        return "Standard";
    }

    @Override
    protected BigDecimal treatmentCostFor(BillingContext context) {
        return context.getTreatment().getCost();
    }

    @Override
    protected BigDecimal consultationFeeFor(BillingContext context) {
        return context.getConsultationFee();
    }

    @Override
    protected String baseExplanation(BillingContext context) {
        return "Standard charge: the price of " + context.getTreatment().getName()
                + " plus the clinic consultation fee.";
    }
}

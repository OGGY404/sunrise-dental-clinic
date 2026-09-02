package lk.icbt.cis6003.dentalclinic.service.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The part every billing rule does the same way.
 *
 * DESIGN PATTERN: Template Method.
 *
 * calculate() below is the fixed recipe, and it is final so no rule can change
 * the order of the steps. Each concrete rule fills in only the two steps that
 * differ: what the treatment costs, and what consultation fee to charge. The
 * loyalty discount and the rounding are then applied once, here, for everybody.
 *
 * WHY THIS MATTERS
 * Before this class existed, each rule would have had to remember to apply the
 * loyalty discount itself. Sooner or later one of them would forget, and a
 * regular patient would be charged full price for a filling but not for a
 * crown, with nothing in the code to explain why. Now that cannot happen.
 */
public abstract class AbstractBillingStrategy implements BillingStrategy {

    /** A patient is treated as loyal from their fifth completed visit. */
    protected static final int LOYALTY_VISIT_THRESHOLD = 5;

    /** Loyal patients get 10 percent off the treatment cost, not off the fee. */
    protected static final BigDecimal LOYALTY_DISCOUNT_RATE = new BigDecimal("0.10");

    /**
     * The fixed recipe. final on purpose: the steps always happen in this
     * order, whichever rule is in use.
     */
    @Override
    public final BillCharge calculate(BillingContext context) {
        BigDecimal treatmentCost = treatmentCostFor(context);
        BigDecimal consultationFee = consultationFeeFor(context);
        BigDecimal discount = loyaltyDiscountFor(context, treatmentCost);

        return new BillCharge(
                treatmentCost,
                consultationFee,
                discount,
                getName(),
                explanationFor(context, discount));
    }

    /** Step each rule must fill in: what the treatment itself costs. */
    protected abstract BigDecimal treatmentCostFor(BillingContext context);

    /** Step each rule must fill in: what consultation fee to add. */
    protected abstract BigDecimal consultationFeeFor(BillingContext context);

    /**
     * Shared step: the loyalty discount.
     *
     * It is worked out on the treatment cost that the rule just produced, not
     * on the price list. That way a surgical patient gets their 10 percent off
     * the supplemented cost they are actually being charged.
     */
    protected BigDecimal loyaltyDiscountFor(BillingContext context, BigDecimal treatmentCost) {
        if (context.getCompletedVisitCount() < LOYALTY_VISIT_THRESHOLD) {
            return BigDecimal.ZERO;
        }
        return treatmentCost.multiply(LOYALTY_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** Shared step: the sentence printed under the total on the bill. */
    protected String explanationFor(BillingContext context, BigDecimal discount) {
        StringBuilder text = new StringBuilder(baseExplanation(context));
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append(" A 10% loyalty discount was applied, because this patient has completed ")
                .append(context.getCompletedVisitCount())
                .append(" previous visits.");
        }
        return text.toString();
    }

    /** The first sentence of the explanation, which each rule writes itself. */
    protected abstract String baseExplanation(BillingContext context);
}

package lk.icbt.cis6003.dentalclinic.service.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The result of applying one billing rule: the three amounts that make up a
 * bill, plus a short reason the receptionist can read out to the patient.
 *
 * This is not stored in the database. It is the answer a billing rule gives
 * back, which the billing service then copies onto a real Bill row.
 *
 * All three amounts are rounded to two decimal places here, in one place, so no
 * individual rule can forget to do it.
 */
public class BillCharge {

    private final BigDecimal treatmentCost;
    private final BigDecimal consultationFee;
    private final BigDecimal discount;
    private final String strategyName;
    private final String explanation;

    public BillCharge(BigDecimal treatmentCost,
                      BigDecimal consultationFee,
                      BigDecimal discount,
                      String strategyName,
                      String explanation) {
        this.treatmentCost = money(treatmentCost);
        this.consultationFee = money(consultationFee);
        this.discount = money(discount);
        this.strategyName = strategyName;
        this.explanation = explanation;
    }

    /** Rounds to 2 decimal places, the way money is written. */
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    /** Which rule produced this charge, for example "Surgical". */
    public String getStrategyName() {
        return strategyName;
    }

    /** One sentence explaining the charge, printed on the bill. */
    public String getExplanation() {
        return explanation;
    }

    /**
     * What the patient owes.
     *
     * MySQL works the same sum out again in the bills.total_amount generated
     * column. Having it here as well lets the screen show a total before the
     * bill is saved, and the two are tested against each other.
     */
    public BigDecimal getTotal() {
        BigDecimal total = treatmentCost.add(consultationFee).subtract(discount);
        // A discount must never turn into money owed back to the patient.
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total;
    }

    @Override
    public String toString() {
        return "BillCharge{" + strategyName
                + ", treatment=" + treatmentCost
                + ", consultation=" + consultationFee
                + ", discount=" + discount
                + ", total=" + getTotal() + "}";
    }
}

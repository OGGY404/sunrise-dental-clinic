package lk.icbt.cis6003.dentalclinic.service.billing;

import lk.icbt.cis6003.dentalclinic.model.Treatment;

import java.math.BigDecimal;

/**
 * Everything a billing rule needs in order to work out a charge.
 *
 * WHY THIS CLASS EXISTS
 * Without it, every billing rule would need a method with three or four
 * parameters, and adding a fifth fact later (say, whether the patient has
 * insurance) would mean changing every rule at once. Passing one context object
 * means a new fact is added here and only the rules that care about it change.
 *
 * The object cannot be changed once it is made. A billing rule can therefore
 * never accidentally alter the facts it was given.
 */
public class BillingContext {

    private final Treatment treatment;
    private final BigDecimal consultationFee;
    private final int completedVisitCount;

    public BillingContext(Treatment treatment, BigDecimal consultationFee, int completedVisitCount) {
        if (treatment == null) {
            throw new IllegalArgumentException("A bill needs a treatment.");
        }
        if (consultationFee == null) {
            throw new IllegalArgumentException("A bill needs a consultation fee.");
        }
        this.treatment = treatment;
        this.consultationFee = consultationFee;
        this.completedVisitCount = Math.max(0, completedVisitCount);
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    /** How many visits this patient has already completed and paid for. */
    public int getCompletedVisitCount() {
        return completedVisitCount;
    }

    @Override
    public String toString() {
        return "BillingContext{treatment=" + treatment.getTreatmentCode()
                + ", consultationFee=" + consultationFee
                + ", completedVisits=" + completedVisitCount + "}";
    }
}

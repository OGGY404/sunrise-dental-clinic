package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Treatment;

import java.math.BigDecimal;

/**
 * One line of the "choose a treatment" dropdown, and of the price list.
 *
 * The cost is sent so the screen can show the patient what the visit will cost
 * before it is booked. It is only a preview: the real bill is worked out again
 * by the business tier from the stored price, so a changed number on the screen
 * cannot change what is charged.
 */
public class TreatmentResponse {

    private final Long treatmentId;
    private final String treatmentCode;
    private final String name;
    private final String description;
    private final BigDecimal cost;
    private final int durationMinutes;

    private TreatmentResponse(Treatment treatment) {
        this.treatmentId = treatment.getTreatmentId();
        this.treatmentCode = treatment.getTreatmentCode();
        this.name = treatment.getName();
        this.description = treatment.getDescription();
        this.cost = treatment.getCost();
        this.durationMinutes = treatment.getDurationMinutes();
    }

    public static TreatmentResponse from(Treatment treatment) {
        return new TreatmentResponse(treatment);
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}

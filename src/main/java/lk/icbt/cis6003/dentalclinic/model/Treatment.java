package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One item on the clinic price list, for example a filling or a root canal.
 *
 * WHY THE COST IS A BigDecimal AND NOT A double
 * A double cannot hold money exactly. In a double, 0.1 + 0.2 comes out as
 * 0.30000000000000004. On a bill that turns into rupees that do not add up.
 * BigDecimal stores the digits themselves, so 6000.00 stays 6000.00.
 */
@Entity
@Table(name = "treatments")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "treatment_id")
    private Long treatmentId;

    /** Short code on the price list, for example TRT-003. */
    @Column(name = "treatment_code", nullable = false, unique = true, length = 20)
    private String treatmentCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    /** Price of the treatment itself. The consultation fee is added separately. */
    @Column(name = "cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    /** How long the chair is needed for. Used by the dentist workload report. */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    /** false means the clinic no longer offers it, but old bills still name it. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Treatment() {
    }

    public Treatment(String treatmentCode, String name, BigDecimal cost, int durationMinutes) {
        this.treatmentCode = treatmentCode;
        this.name = name;
        this.cost = cost;
        this.durationMinutes = durationMinutes;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Treatment)) {
            return false;
        }
        return Objects.equals(treatmentCode, ((Treatment) other).treatmentCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatmentCode);
    }

    @Override
    public String toString() {
        return "Treatment{code=" + treatmentCode + ", name=" + name + ", cost=" + cost + "}";
    }
}

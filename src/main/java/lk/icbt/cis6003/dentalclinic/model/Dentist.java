package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A dentist the clinic can book patients with.
 *
 * A dentist who leaves is switched to active = false, never deleted. Deleting
 * them would break every past appointment that points at them, and the clinic
 * still needs those records for its treatment history and its accounts.
 */
@Entity
@Table(name = "dentists")
public class Dentist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dentist_id")
    private Long dentistId;

    /** Short staff code shown on the schedule, for example DEN-001. */
    @Column(name = "dentist_code", nullable = false, unique = true, length = 20)
    private String dentistCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "specialisation", length = 80)
    private String specialisation;

    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    @Column(name = "email", length = 120)
    private String email;

    /** false means the dentist has left, so they drop off the booking form. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Dentist() {
    }

    public Dentist(String dentistCode, String fullName, String specialisation) {
        this.dentistCode = dentistCode;
        this.fullName = fullName;
        this.specialisation = specialisation;
    }

    public Long getDentistId() {
        return dentistId;
    }

    public void setDentistId(Long dentistId) {
        this.dentistId = dentistId;
    }

    public String getDentistCode() {
        return dentistCode;
    }

    public void setDentistCode(String dentistCode) {
        this.dentistCode = dentistCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialisation() {
        return specialisation;
    }

    public void setSpecialisation(String specialisation) {
        this.specialisation = specialisation;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Name and specialisation together, for the drop-down on the booking form. */
    public String getDisplayName() {
        if (specialisation == null || specialisation.isBlank()) {
            return fullName;
        }
        return fullName + " (" + specialisation + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Dentist)) {
            return false;
        }
        return Objects.equals(dentistCode, ((Dentist) other).dentistCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dentistCode);
    }

    @Override
    public String toString() {
        return "Dentist{code=" + dentistCode + ", name=" + fullName + "}";
    }
}

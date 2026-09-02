package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A patient of the clinic (FR2 patient details, FR7 treatment history).
 *
 * WHY PATIENTS ARE A SEPARATE TABLE
 * The paper system wrote the patient details again on every appointment card.
 * That is what made the history impossible to follow. Here the patient is
 * stored once, and every appointment points back at that one row, so all the
 * visits of one person hang together.
 *
 * UML NOTE (Task A): the link to Appointment is a COMPOSITION. An appointment
 * cannot exist without the patient it belongs to, and deleting the patient
 * would take the appointments with it. On the class diagram this is the filled
 * diamond, with multiplicity 1 to 0..*.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    /** Human-friendly reference, for example PAT-000042. Never reused. */
    @Column(name = "patient_code", nullable = false, unique = true, length = 20)
    private String patientCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /** The clinic telephones patients to confirm, so this is required. */
    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Every visit this patient has made, newest first.
     *
     * FetchType.LAZY means the visits are only loaded from the database when
     * the code actually asks for them. Without it, opening a search results
     * page of 50 patients would also drag in all of their appointments.
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("appointmentDate DESC, appointmentTime DESC")
    private List<Appointment> appointments = new ArrayList<>();

    public Patient() {
    }

    public Patient(String patientCode, String fullName, String address, String contactNumber) {
        this.patientCode = patientCode;
        this.fullName = fullName;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    /**
     * Links an appointment to this patient and this patient to the appointment.
     *
     * Both sides have to be set. If only one side is set, the object in memory
     * disagrees with what will be written to the database, which is one of the
     * easiest mistakes to make with JPA.
     */
    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.setPatient(this);
    }

    /** Age in whole years, or null when no date of birth was given. */
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Patient)) {
            return false;
        }
        return Objects.equals(patientCode, ((Patient) other).patientCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientCode);
    }

    @Override
    public String toString() {
        return "Patient{code=" + patientCode + ", name=" + fullName + "}";
    }
}

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One booked visit (FR2 register, FR3 display, FR7 cancel and reschedule).
 *
 * This is the centre of the whole system. It joins a patient, a dentist and a
 * treatment together at one date and time.
 *
 * HOW DOUBLE BOOKING IS STOPPED
 * The database has a generated column called slot_key holding
 * "dentistId|date|time" for a live appointment, and NULL for a cancelled one,
 * with a UNIQUE index over it. That index is the real guarantee, because it
 * still holds if two receptionists press Save at the same instant. The Java
 * service checks first only so the second person gets a friendly message
 * instead of a database error.
 *
 * slot_key is deliberately NOT mapped as a field here. Java never needs to read
 * it, and leaving it out keeps the entity honest about what it owns.
 *
 * UML NOTE (Task A): Patient, Dentist and Treatment are all ManyToOne from this
 * side. Reading that backwards gives the multiplicities on the class diagram:
 * one patient has many appointments, one dentist has many appointments, one
 * treatment appears on many appointments.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    /** The number the patient is given, for example APT-20260907-0007. */
    @Column(name = "appointment_no", nullable = false, unique = true, length = 20)
    private String appointmentNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentist_id", nullable = false)
    private Dentist dentist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Which member of staff took the booking. Kept for the audit trail. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * The bill for this visit, once one has been issued.
     *
     * One appointment can only ever be billed once, which the database enforces
     * with a UNIQUE key on bills.appointment_id. That is why this is OneToOne
     * and not OneToMany.
     */
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Bill bill;

    public Appointment() {
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Dentist getDentist() {
        return dentist;
    }

    public void setDentist(Dentist dentist) {
        this.dentist = dentist;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    /** The date and time as one value, which is easier to sort and compare. */
    public LocalDateTime getStartsAt() {
        if (appointmentDate == null || appointmentTime == null) {
            return null;
        }
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }

    /** True while the appointment still holds its slot in the diary. */
    public boolean isLive() {
        return status != AppointmentStatus.CANCELLED;
    }

    /** Only a visit that actually happened may be billed. */
    public boolean isBillable() {
        return status == AppointmentStatus.COMPLETED;
    }

    public boolean isBilled() {
        return bill != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Appointment)) {
            return false;
        }
        return Objects.equals(appointmentNo, ((Appointment) other).appointmentNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentNo);
    }

    @Override
    public String toString() {
        return "Appointment{no=" + appointmentNo
                + ", date=" + appointmentDate
                + ", time=" + appointmentTime
                + ", status=" + status + "}";
    }
}

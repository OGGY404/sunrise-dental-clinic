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

    /**
     * Starts building an appointment.
     *
     * DESIGN PATTERN: Builder. See the Builder class at the bottom of this file
     * for why an eight-parameter constructor was rejected.
     */
    public static Builder builder() {
        return new Builder();
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

    /**
     * Builds an Appointment one named value at a time.
     *
     * DESIGN PATTERN: Builder.
     *
     * WHY NOT JUST A CONSTRUCTOR
     * An appointment needs eight things. A constructor taking all eight would
     * look like this at the call site:
     *
     *     new Appointment(no, patient, dentist, treatment, date, time, status, notes)
     *
     * Nothing there stops the dentist and the treatment being swapped, or the
     * date and the time. Both mistakes compile perfectly and only show up as a
     * wrong booking in front of a patient. With the builder each value is named
     * where it is given, so a swap is visible while reading the code.
     *
     * The builder also refuses to hand back an object that is missing something
     * required, which means an incomplete Appointment never exists at all,
     * rather than existing and failing later with a confusing database error.
     *
     * CRITICAL EVALUATION (for the report): the cost is roughly forty extra
     * lines for one class, and a second place to update when a field is added.
     * It is worth it here because this is the object the whole system is built
     * around, and because the compiler cannot catch the mistakes it prevents.
     * It would not be worth it for a small class such as ClinicSetting.
     */
    public static class Builder {

        private String appointmentNo;
        private Patient patient;
        private Dentist dentist;
        private Treatment treatment;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private AppointmentStatus status = AppointmentStatus.BOOKED;
        private String notes;
        private User createdBy;

        public Builder appointmentNo(String appointmentNo) {
            this.appointmentNo = appointmentNo;
            return this;
        }

        public Builder patient(Patient patient) {
            this.patient = patient;
            return this;
        }

        public Builder dentist(Dentist dentist) {
            this.dentist = dentist;
            return this;
        }

        public Builder treatment(Treatment treatment) {
            this.treatment = treatment;
            return this;
        }

        /** The date of the visit. Named "on" so the call reads like English. */
        public Builder on(LocalDate appointmentDate) {
            this.appointmentDate = appointmentDate;
            return this;
        }

        /** The time of the visit. Named "at" so the call reads like English. */
        public Builder at(LocalTime appointmentTime) {
            this.appointmentTime = appointmentTime;
            return this;
        }

        /** Optional. A new booking is BOOKED unless told otherwise. */
        public Builder status(AppointmentStatus status) {
            this.status = status;
            return this;
        }

        /** Optional. */
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        /** Optional. The member of staff who took the booking. */
        public Builder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * Checks everything required is present, then produces the appointment.
         *
         * Each check names the missing thing in its message, so the developer
         * who sees the error knows immediately what was left out.
         */
        public Appointment build() {
            require(appointmentNo != null && !appointmentNo.isBlank(),
                    "An appointment needs an appointment number.");
            require(patient != null, "An appointment needs a patient.");
            require(dentist != null, "An appointment needs a dentist.");
            require(treatment != null, "An appointment needs a treatment.");
            require(appointmentDate != null, "An appointment needs a date.");
            require(appointmentTime != null, "An appointment needs a time.");

            Appointment appointment = new Appointment();
            appointment.setAppointmentNo(appointmentNo);
            appointment.setDentist(dentist);
            appointment.setTreatment(treatment);
            appointment.setAppointmentDate(appointmentDate);
            appointment.setAppointmentTime(appointmentTime);
            appointment.setStatus(status);
            appointment.setNotes(notes);
            appointment.setCreatedBy(createdBy);

            // Sets both sides of the patient link at once, so the object in
            // memory always agrees with what will be written to the database.
            patient.addAppointment(appointment);

            return appointment;
        }

        private void require(boolean condition, String message) {
            if (!condition) {
                throw new IllegalStateException(message);
            }
        }
    }
}

package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Appointment;
import lk.icbt.cis6003.dentalclinic.model.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * What the web service says about one appointment (FR3 - display appointment
 * details).
 *
 * This is everything the brief asks the search screen to show: the appointment
 * number, the patient name, address and contact number, the dentist, the
 * treatment, and the date and time.
 *
 * The patient is nested rather than flattened, so the same PatientResponse is
 * reused on the patient screen and there is only one place that decides what a
 * patient looks like on screen.
 */
public class AppointmentResponse {

    private final String appointmentNo;
    private final AppointmentStatus status;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTime;
    private final String notes;

    private final PatientResponse patient;

    private final Long dentistId;
    private final String dentistCode;
    private final String dentistName;

    private final Long treatmentId;
    private final String treatmentCode;
    private final String treatmentName;
    private final BigDecimal treatmentCost;

    private final LocalDateTime createdAt;

    private AppointmentResponse(Appointment appointment) {
        this.appointmentNo = appointment.getAppointmentNo();
        this.status = appointment.getStatus();
        this.appointmentDate = appointment.getAppointmentDate();
        this.appointmentTime = appointment.getAppointmentTime();
        this.notes = appointment.getNotes();

        this.patient = PatientResponse.from(appointment.getPatient());

        this.dentistId = appointment.getDentist().getDentistId();
        this.dentistCode = appointment.getDentist().getDentistCode();
        this.dentistName = appointment.getDentist().getFullName();

        this.treatmentId = appointment.getTreatment().getTreatmentId();
        this.treatmentCode = appointment.getTreatment().getTreatmentCode();
        this.treatmentName = appointment.getTreatment().getName();
        this.treatmentCost = appointment.getTreatment().getCost();

        this.createdAt = appointment.getCreatedAt();
    }

    /**
     * Copies a stored appointment into the shape the screen reads.
     *
     * The patient, dentist and treatment are read here, so the repository that
     * loaded the appointment must have fetched them as well. That is why the
     * finder methods used by the web layer carry an @EntityGraph.
     */
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment);
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public String getNotes() {
        return notes;
    }

    public PatientResponse getPatient() {
        return patient;
    }

    public Long getDentistId() {
        return dentistId;
    }

    public String getDentistCode() {
        return dentistCode;
    }

    public String getDentistName() {
        return dentistName;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

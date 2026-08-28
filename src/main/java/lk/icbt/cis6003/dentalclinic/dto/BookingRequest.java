package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Gender;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Everything the booking form collects in one object (FR2).
 *
 * WHY THIS IS NOT AN ENTITY
 * The form mixes two things: details about the patient, and details about the
 * visit. Neither table matches it exactly. Sending the JPA entities straight to
 * the screen would also let a web form set fields it has no business setting,
 * such as the patient id or the created_at timestamp.
 *
 * This class is a Data Transfer Object: a plain carrier that crosses the line
 * between the presentation tier and the business tier. Step 5 adds the
 * validation annotations to it, so bad input is rejected before any service
 * code runs.
 */
public class BookingRequest {

    // --- the patient ---------------------------------------------------------
    private String fullName;
    private String address;
    private String contactNumber;
    private String email;
    private LocalDate dateOfBirth;
    private Gender gender;

    // --- the visit -----------------------------------------------------------
    private Long dentistId;
    private Long treatmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String notes;

    public BookingRequest() {
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

    public Long getDentistId() {
        return dentistId;
    }

    public void setDentistId(Long dentistId) {
        this.dentistId = dentistId;
    }

    public Long getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Long treatmentId) {
        this.treatmentId = treatmentId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "BookingRequest{patient=" + fullName
                + ", contact=" + contactNumber
                + ", dentistId=" + dentistId
                + ", treatmentId=" + treatmentId
                + ", date=" + appointmentDate
                + ", time=" + appointmentTime + "}";
    }
}

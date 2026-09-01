package lk.icbt.cis6003.dentalclinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lk.icbt.cis6003.dentalclinic.model.Gender;
import org.springframework.format.annotation.DateTimeFormat;

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
 * between the presentation tier and the business tier.
 *
 * THE VALIDATION RULES BELOW
 * Each annotation is one rule. Spring checks them all before the controller
 * method runs, so a bad form never reaches the business tier at all, and the
 * receptionist is told which box to correct rather than getting one vague
 * error. Every limit matches the column width in schema.sql, so a value that
 * passes validation can always be stored.
 */
public class BookingRequest {

    // --- the patient ---------------------------------------------------------

    /**
     * Letters, spaces, dots, hyphens and apostrophes only.
     *
     * Sri Lankan names really do contain dots and hyphens, as in
     * "W.A.G.K. Rathnayake-Silva", so those are allowed. Digits are not: a
     * number in the name box always means it was typed into the wrong box.
     */
    @NotBlank(message = "Please enter the patient name.")
    @Size(max = 100, message = "The patient name cannot be longer than 100 characters.")
    @Pattern(regexp = "^[\\p{L} .'-]+$",
             message = "A patient name can only contain letters, spaces, dots, hyphens and apostrophes.")
    private String fullName;

    @NotBlank(message = "Please enter the patient address.")
    @Size(max = 255, message = "The address cannot be longer than 255 characters.")
    private String address;

    /**
     * A Sri Lankan telephone number, written either as 0771234567 or as
     * +94771234567. Both are ten digits after the leading zero or country code.
     */
    @NotBlank(message = "Please enter a contact number.")
    @Pattern(regexp = "^(0\\d{9}|\\+94\\d{9})$",
             message = "Please enter a valid Sri Lankan telephone number, for example 0771234567.")
    private String contactNumber;

    /** Optional, because not every patient has an email address. */
    @Email(message = "Please enter a valid email address, or leave the box empty.")
    @Size(max = 120, message = "The email address cannot be longer than 120 characters.")
    private String email;

    /** Optional, but if it is given it cannot be a day that has not happened. */
    @Past(message = "The date of birth must be in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    private Gender gender;

    // --- the visit -----------------------------------------------------------

    @NotNull(message = "Please choose a dentist.")
    private Long dentistId;

    @NotNull(message = "Please choose a treatment.")
    private Long treatmentId;

    /**
     * Today is allowed, because a patient can walk in and be seen this
     * afternoon. Yesterday is not: an appointment cannot be booked into a day
     * that has already gone.
     */
    @NotNull(message = "Please choose a date for the appointment.")
    @FutureOrPresent(message = "An appointment cannot be booked in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;

    @NotNull(message = "Please choose a time for the appointment.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime appointmentTime;

    @Size(max = 500, message = "The notes cannot be longer than 500 characters.")
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

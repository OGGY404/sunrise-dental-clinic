package lk.icbt.cis6003.dentalclinic.dto;

import lk.icbt.cis6003.dentalclinic.model.Gender;
import lk.icbt.cis6003.dentalclinic.model.Patient;

import java.time.LocalDate;

/**
 * What the web service says about a patient.
 *
 * WHY THE ENTITY IS NOT SENT STRAIGHT OUT
 * The Patient entity carries its whole list of appointments, and each of those
 * carries a patient again. Sending it as JSON would loop forever. It also holds
 * the internal database id, which nothing outside the system should depend on,
 * because it would then be impossible to ever move the data.
 *
 * So the presentation tier answers with this small, flat, read-only object
 * instead. It shows the patient code, which is the reference staff actually
 * quote to a patient.
 */
public class PatientResponse {

    private final String patientCode;
    private final String fullName;
    private final String address;
    private final String contactNumber;
    private final String email;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final Integer age;

    private PatientResponse(Patient patient) {
        this.patientCode = patient.getPatientCode();
        this.fullName = patient.getFullName();
        this.address = patient.getAddress();
        this.contactNumber = patient.getContactNumber();
        this.email = patient.getEmail();
        this.dateOfBirth = patient.getDateOfBirth();
        this.gender = patient.getGender();
        this.age = patient.getAge();
    }

    /** Copies a stored patient into the shape the screen reads. */
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient);
    }

    public String getPatientCode() {
        return patientCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAddress() {
        return address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public Integer getAge() {
        return age;
    }
}

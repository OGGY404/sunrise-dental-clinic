package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Patient records (FR2, and FR7 treatment history).
 *
 * BUSINESS LOGIC TIER. This class knows the clinic rules about patients. It
 * does not know about HTTP, forms or SQL: the controller above it deals with
 * the web, and the repository below it deals with the database.
 *
 * THE ONE RULE THAT MATTERS MOST
 * A returning patient must not be entered a second time. If they are, their
 * treatment history splits into two halves and the dentist can no longer see
 * what was done last visit. That was exactly the failure of the paper system
 * this project replaces.
 */
@Service
@Transactional
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final ReferenceNumberGenerator numberGenerator;

    public PatientService(PatientRepository patientRepository,
                          ReferenceNumberGenerator numberGenerator) {
        this.patientRepository = patientRepository;
        this.numberGenerator = numberGenerator;
    }

    /**
     * Finds the patient this booking is for, or creates them.
     *
     * HOW A RETURNING PATIENT IS RECOGNISED
     * The name and the telephone number must both match. Name alone is not
     * enough, because two different people really can both be called Kamal
     * Silva. Telephone alone is not enough either, because a family shares one
     * number and a mother booking for her son is not the same patient.
     *
     * The same rule is written into the sp_find_or_create_patient stored
     * procedure. Having it in both places is deliberate: the Java version gives
     * the receptionist an instant answer on screen, and the database version
     * keeps the rule true for any other program that ever writes to this
     * database. The report discusses the cost of that duplication.
     */
    public Patient findOrCreate(BookingRequest request) {
        String name = tidyName(request.getFullName());
        String contact = tidyContactNumber(request.getContactNumber());

        Optional<Patient> existing =
                patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber(name, contact);

        if (existing.isPresent()) {
            Patient patient = existing.get();
            log.debug("Recognised returning patient {}", patient.getPatientCode());
            updateDetails(patient, request);
            return patientRepository.save(patient);
        }

        Patient created = new Patient(numberGenerator.nextPatientCode(), name, request.getAddress(), contact);
        created.setEmail(request.getEmail());
        created.setDateOfBirth(request.getDateOfBirth());
        created.setGender(request.getGender());

        log.info("Registered new patient {} ({})", created.getPatientCode(), name);
        return patientRepository.save(created);
    }

    /**
     * Refreshes the details of someone the clinic already knows.
     *
     * The address is always taken from the form, because a patient who has
     * moved house will type the new one. The optional fields are only
     * overwritten when the form actually supplied something, so a blank box
     * never wipes out a date of birth that was recorded correctly last year.
     */
    private void updateDetails(Patient patient, BookingRequest request) {
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            patient.setAddress(request.getAddress());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            patient.setEmail(request.getEmail());
        }
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
    }

    @Transactional(readOnly = true)
    public Patient findByCode(String patientCode) {
        return patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> NotFoundException.of("patient", patientCode));
    }

    @Transactional(readOnly = true)
    public Patient findById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> NotFoundException.of("patient", String.valueOf(patientId)));
    }

    /**
     * Search box on the patient screen.
     *
     * An empty search returns nothing rather than everything. Listing every
     * patient in the clinic because somebody pressed Enter on an empty box
     * would be slow and would tell the receptionist nothing.
     */
    @Transactional(readOnly = true)
    public List<Patient> searchByName(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            return Collections.emptyList();
        }
        return patientRepository.findByFullNameContainingIgnoreCase(namePart.trim());
    }

    @Transactional(readOnly = true)
    public List<Patient> searchByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            return Collections.emptyList();
        }
        return patientRepository.findByContactNumber(tidyContactNumber(contactNumber));
    }

    /**
     * Removes spaces and dashes from a telephone number.
     *
     * Staff type numbers however they like: "077-123 4567", "077 1234567".
     * Unless they are all stored the same way, the check above would fail to
     * recognise a patient the clinic has seen ten times. The same tidying is
     * done by the trg_patients_before_insert trigger in the database.
     */
    private String tidyContactNumber(String contactNumber) {
        if (contactNumber == null) {
            return null;
        }
        return contactNumber.replaceAll("[\\s-]", "").trim();
    }

    /** Collapses repeated spaces so " Kamal  Silva " becomes "Kamal Silva". */
    private String tidyName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim().replaceAll("\\s+", " ");
    }
}

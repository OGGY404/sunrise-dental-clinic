package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for patients (FR2, and FR7 treatment history).
 * DESIGN PATTERN: Repository.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    /**
     * Returning patients on one telephone number.
     *
     * This returns a list and not a single patient on purpose: a family often
     * shares one number, so the receptionist has to be shown the choices and
     * pick the right person.
     */
    List<Patient> findByContactNumber(String contactNumber);

    /** Search box on the patient screen. Matches part of a name, any case. */
    List<Patient> findByFullNameContainingIgnoreCase(String namePart);

    /**
     * The exact-match rule the sp_find_or_create_patient stored procedure uses
     * to decide that somebody is a returning patient rather than a new one.
     */
    Optional<Patient> findFirstByFullNameIgnoreCaseAndContactNumber(String fullName, String contactNumber);

    boolean existsByPatientCode(String patientCode);
}

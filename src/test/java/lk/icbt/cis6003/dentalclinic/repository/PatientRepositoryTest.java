package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for patient records (FR2, and FR7 - treatment history).
 *
 * The receptionist searches for a returning patient by telephone number or by
 * name. Getting that search right is what stops the same person being entered
 * twice, which would split their treatment history in two.
 */
@DisplayName("PatientRepository")
class PatientRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @DisplayName("saves a patient and gives them a database id")
    void savesPatientAndAssignsId() {
        Patient saved = patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));

        assertThat(saved.getPatientId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("finds a patient by their patient code")
    void findsPatientByCode() {
        patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));

        assertThat(patientRepository.findByPatientCode("PAT-000001"))
                .isPresent()
                .get()
                .extracting(Patient::getFullName)
                .isEqualTo("Kamal Silva");
    }

    @Test
    @DisplayName("finds returning patients by telephone number")
    void findsPatientsByContactNumber() {
        patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));
        patientRepository.save(newPatient("PAT-000002", "Nadeeka Silva", "0771234567"));
        patientRepository.save(newPatient("PAT-000003", "Sunil Gunawardena", "0779999999"));

        // A family often shares one telephone number, so this returns a list.
        List<Patient> family = patientRepository.findByContactNumber("0771234567");

        assertThat(family)
                .extracting(Patient::getFullName)
                .containsExactlyInAnyOrder("Kamal Silva", "Nadeeka Silva");
    }

    @Test
    @DisplayName("searches by part of a name, ignoring capital letters")
    void searchesByPartialNameIgnoringCase() {
        patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));
        patientRepository.save(newPatient("PAT-000002", "Nadeeka Silva", "0771111111"));
        patientRepository.save(newPatient("PAT-000003", "Sunil Gunawardena", "0779999999"));

        List<Patient> results = patientRepository.findByFullNameContainingIgnoreCase("silva");

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("returns nothing when the name matches no one")
    void returnsEmptyListWhenNameMatchesNobody() {
        patientRepository.save(newPatient("PAT-000001", "Kamal Silva", "0771234567"));

        assertThat(patientRepository.findByFullNameContainingIgnoreCase("Fernando")).isEmpty();
    }

    @Test
    @DisplayName("refuses two patients with the same patient code")
    void rejectsDuplicatePatientCode() {
        patientRepository.saveAndFlush(newPatient("PAT-000001", "Kamal Silva", "0771234567"));

        assertThatThrownBy(() ->
                patientRepository.saveAndFlush(newPatient("PAT-000001", "Someone Else", "0770000000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

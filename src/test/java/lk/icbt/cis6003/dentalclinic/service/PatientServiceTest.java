package lk.icbt.cis6003.dentalclinic.service;

import lk.icbt.cis6003.dentalclinic.dto.BookingRequest;
import lk.icbt.cis6003.dentalclinic.exception.NotFoundException;
import lk.icbt.cis6003.dentalclinic.model.Gender;
import lk.icbt.cis6003.dentalclinic.model.Patient;
import lk.icbt.cis6003.dentalclinic.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for patient records (FR2, FR7).
 *
 * The one rule that really matters here is not creating the same person twice.
 * If a returning patient is entered again, their treatment history splits into
 * two halves and the dentist can no longer see what was done last time. That is
 * the exact failure the paper system had.
 */
@DisplayName("PatientService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private ReferenceNumberGenerator numberGenerator;

    private PatientService service;

    private Patient existingKamal;

    @BeforeEach
    void setUp() {
        existingKamal = new Patient("PAT-000001", "Kamal Silva", "Old address", "0771234567");
        existingKamal.setPatientId(10L);

        when(numberGenerator.nextPatientCode()).thenReturn("PAT-000042");
        when(patientRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new PatientService(patientRepository, numberGenerator);
    }

    private BookingRequest request(String name, String contact) {
        BookingRequest request = new BookingRequest();
        request.setFullName(name);
        request.setContactNumber(contact);
        request.setAddress("No. 42, Galle Road, Colombo 03");
        request.setEmail("kamal@example.lk");
        request.setDateOfBirth(LocalDate.of(1990, 5, 20));
        request.setGender(Gender.MALE);
        return request;
    }

    @Test
    @DisplayName("reuses the existing record when the name and telephone both match")
    void reusesAReturningPatient() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber("Kamal Silva", "0771234567"))
                .thenReturn(Optional.of(existingKamal));

        Patient found = service.findOrCreate(request("Kamal Silva", "0771234567"));

        assertThat(found.getPatientId()).isEqualTo(10L);
        assertThat(found.getPatientCode()).isEqualTo("PAT-000001");
        verify(numberGenerator, never()).nextPatientCode();
    }

    @Test
    @DisplayName("updates the address of a returning patient who has moved house")
    void updatesTheDetailsOfAReturningPatient() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber(any(), any()))
                .thenReturn(Optional.of(existingKamal));

        Patient found = service.findOrCreate(request("Kamal Silva", "0771234567"));

        assertThat(found.getAddress()).isEqualTo("No. 42, Galle Road, Colombo 03");
        verify(patientRepository).save(existingKamal);
    }

    @Test
    @DisplayName("creates a new record when nobody matches, with the next patient code")
    void createsANewPatient() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber(any(), any()))
                .thenReturn(Optional.empty());

        Patient created = service.findOrCreate(request("Nadeeka Silva", "0779999999"));

        assertThat(created.getPatientCode()).isEqualTo("PAT-000042");
        assertThat(created.getFullName()).isEqualTo("Nadeeka Silva");
        assertThat(created.getContactNumber()).isEqualTo("0779999999");
        assertThat(created.getGender()).isEqualTo(Gender.MALE);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("treats a different telephone number as a different person")
    void differentTelephoneMeansDifferentPerson() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber("Kamal Silva", "0770000000"))
                .thenReturn(Optional.empty());

        Patient created = service.findOrCreate(request("Kamal Silva", "0770000000"));

        // Two different people really can share a name, so the telephone number
        // has to agree as well before we treat them as the same patient.
        assertThat(created.getPatientCode()).isEqualTo("PAT-000042");
    }

    @Test
    @DisplayName("tidies up spaces and dashes typed into the telephone number")
    void normalisesTheTelephoneNumber() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber(any(), any()))
                .thenReturn(Optional.empty());

        BookingRequest messy = request("Nadeeka Silva", " 077-123 4567 ");
        Patient created = service.findOrCreate(messy);

        // Staff type numbers however they like. Storing them all the same way
        // is what makes the "have we seen this person before" check work.
        assertThat(created.getContactNumber()).isEqualTo("0771234567");
    }

    @Test
    @DisplayName("tidies up extra spaces in the name")
    void trimsTheName() {
        when(patientRepository.findFirstByFullNameIgnoreCaseAndContactNumber(any(), any()))
                .thenReturn(Optional.empty());

        Patient created = service.findOrCreate(request("  Nadeeka  Silva  ", "0779999999"));

        assertThat(created.getFullName()).isEqualTo("Nadeeka Silva");
    }

    @Test
    @DisplayName("finds a patient by code for the history screen")
    void findsByPatientCode() {
        when(patientRepository.findByPatientCode("PAT-000001")).thenReturn(Optional.of(existingKamal));

        assertThat(service.findByCode("PAT-000001")).isSameAs(existingKamal);
    }

    @Test
    @DisplayName("says clearly when a patient code does not exist")
    void reportsAnUnknownPatientCode() {
        when(patientRepository.findByPatientCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("PAT-999999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("PAT-999999");
    }

    @Test
    @DisplayName("searches by part of a name")
    void searchesByName() {
        when(patientRepository.findByFullNameContainingIgnoreCase("silva"))
                .thenReturn(List.of(existingKamal));

        assertThat(service.searchByName("silva")).containsExactly(existingKamal);
    }

    @Test
    @DisplayName("returns nothing rather than everything when the search box is empty")
    void emptySearchReturnsNothing() {
        // Returning every patient in the clinic for an empty search would be a
        // slow and useless screen, so it is refused politely instead.
        assertThat(service.searchByName("   ")).isEmpty();
        verify(patientRepository, never()).findByFullNameContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searches by telephone number")
    void searchesByTelephone() {
        when(patientRepository.findByContactNumber("0771234567")).thenReturn(List.of(existingKamal));

        assertThat(service.searchByContactNumber("077-123 4567")).containsExactly(existingKamal);
    }
}

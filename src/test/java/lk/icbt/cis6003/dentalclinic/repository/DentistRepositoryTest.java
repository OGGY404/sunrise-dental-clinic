package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Dentist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the dentist list.
 *
 * The booking form shows a drop-down of dentists. A dentist who has left the
 * clinic must disappear from that drop-down, but must stay in the database,
 * because old appointments still point at them.
 */
@DisplayName("DentistRepository")
class DentistRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DentistRepository dentistRepository;

    @Test
    @DisplayName("finds a dentist by their code")
    void findsDentistByCode() {
        dentistRepository.save(newDentist("DEN-001", "Dr. Nimal Perera"));

        assertThat(dentistRepository.findByDentistCode("DEN-001"))
                .isPresent()
                .get()
                .extracting(Dentist::getFullName)
                .isEqualTo("Dr. Nimal Perera");
    }

    @Test
    @DisplayName("lists only active dentists, in name order, for the booking form")
    void listsActiveDentistsInNameOrder() {
        dentistRepository.save(newDentist("DEN-002", "Dr. Shanika Fernando"));
        dentistRepository.save(newDentist("DEN-001", "Dr. Nimal Perera"));

        Dentist leaver = newDentist("DEN-003", "Dr. Ruwan Jayasinghe");
        leaver.setActive(false);
        dentistRepository.save(leaver);

        List<Dentist> forDropDown = dentistRepository.findByActiveTrueOrderByFullNameAsc();

        assertThat(forDropDown)
                .extracting(Dentist::getFullName)
                .containsExactly("Dr. Nimal Perera", "Dr. Shanika Fernando");
    }

    @Test
    @DisplayName("keeps a retired dentist in the database")
    void keepsRetiredDentistOnRecord() {
        Dentist leaver = newDentist("DEN-003", "Dr. Ruwan Jayasinghe");
        leaver.setActive(false);
        dentistRepository.save(leaver);

        assertThat(dentistRepository.findByDentistCode("DEN-003")).isPresent();
    }
}

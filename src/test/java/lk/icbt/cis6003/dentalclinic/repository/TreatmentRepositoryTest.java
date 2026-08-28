package lk.icbt.cis6003.dentalclinic.repository;

import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the price list (FR4 - the treatment cost half of every bill).
 */
@DisplayName("TreatmentRepository")
class TreatmentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Test
    @DisplayName("finds a treatment by its code and keeps the exact price")
    void findsTreatmentByCodeWithExactPrice() {
        treatmentRepository.save(newTreatment("TRT-003", "Tooth Filling", "6000.00"));

        Treatment found = treatmentRepository.findByTreatmentCode("TRT-003").orElseThrow();

        assertThat(found.getName()).isEqualTo("Tooth Filling");
        // Money is stored as BigDecimal, so 6000.00 stays exactly 6000.00.
        assertThat(found.getCost()).isEqualByComparingTo(new BigDecimal("6000.00"));
    }

    @Test
    @DisplayName("lists only treatments the clinic still offers")
    void listsOnlyActiveTreatments() {
        treatmentRepository.save(newTreatment("TRT-001", "Dental Check-up", "2000.00"));

        Treatment discontinued = newTreatment("TRT-999", "Old Service", "500.00");
        discontinued.setActive(false);
        treatmentRepository.save(discontinued);

        List<Treatment> priceList = treatmentRepository.findByActiveTrueOrderByNameAsc();

        assertThat(priceList)
                .extracting(Treatment::getTreatmentCode)
                .containsExactly("TRT-001");
    }

    @Test
    @DisplayName("stores how long a treatment takes, for the workload report")
    void storesDurationMinutes() {
        Treatment rootCanal = newTreatment("TRT-005", "Root Canal Treatment", "25000.00");
        rootCanal.setDurationMinutes(120);

        Treatment saved = treatmentRepository.save(rootCanal);

        assertThat(saved.getDurationMinutes()).isEqualTo(120);
    }
}

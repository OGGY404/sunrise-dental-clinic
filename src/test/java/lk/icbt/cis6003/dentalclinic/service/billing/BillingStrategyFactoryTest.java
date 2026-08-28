package lk.icbt.cis6003.dentalclinic.service.billing;

import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the object that chooses the billing rule.
 *
 * DESIGN PATTERN UNDER TEST: Factory.
 * The billing service must not contain a long if-else chain deciding which sum
 * to use. It asks the factory for a strategy and then just uses it. Adding a
 * fourth rule later means adding a class and one line here, and changing
 * nothing in the service.
 */
@DisplayName("BillingStrategyFactory")
class BillingStrategyFactoryTest {

    private BillingStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new BillingStrategyFactory();
    }

    private Treatment treatment(String code, String name) {
        Treatment t = new Treatment();
        t.setTreatmentCode(code);
        t.setName(name);
        t.setCost(new BigDecimal("5000.00"));
        t.setActive(true);
        return t;
    }

    @Test
    @DisplayName("chooses the surgical rule for a tooth extraction")
    void choosesSurgicalForExtraction() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-004", "Tooth Extraction"));

        assertThat(strategy).isInstanceOf(SurgicalBillingStrategy.class);
    }

    @Test
    @DisplayName("chooses the surgical rule for wisdom tooth surgery")
    void choosesSurgicalForWisdomTooth() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-011", "Wisdom Tooth Surgery"));

        assertThat(strategy).isInstanceOf(SurgicalBillingStrategy.class);
    }

    @Test
    @DisplayName("chooses the consultation-only rule for a check-up")
    void choosesConsultationOnlyForCheckUp() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-001", "Dental Check-up"));

        assertThat(strategy).isInstanceOf(ConsultationOnlyBillingStrategy.class);
    }

    @Test
    @DisplayName("chooses the consultation-only rule for a braces consultation")
    void choosesConsultationOnlyForBracesConsultation() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-008", "Braces Consultation"));

        assertThat(strategy).isInstanceOf(ConsultationOnlyBillingStrategy.class);
    }

    @Test
    @DisplayName("falls back to the standard rule for anything else")
    void fallsBackToStandard() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-007", "Teeth Whitening"));

        assertThat(strategy).isInstanceOf(StandardBillingStrategy.class);
    }

    @Test
    @DisplayName("falls back to the standard rule for a treatment code it has never seen")
    void fallsBackForUnknownCode() {
        BillingStrategy strategy = factory.strategyFor(treatment("TRT-999", "Brand New Service"));

        assertThat(strategy).isInstanceOf(StandardBillingStrategy.class);
    }

    @Test
    @DisplayName("returns the same strategy object every time, because they hold no data")
    void reusesStrategyInstances() {
        BillingStrategy first = factory.strategyFor(treatment("TRT-004", "Tooth Extraction"));
        BillingStrategy second = factory.strategyFor(treatment("TRT-004", "Tooth Extraction"));

        // A strategy has no fields of its own, so one shared object is safe and
        // avoids creating a new object for every bill.
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("refuses a null treatment rather than failing later with a confusing error")
    void rejectsNullTreatment() {
        assertThatThrownBy(() -> factory.strategyFor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

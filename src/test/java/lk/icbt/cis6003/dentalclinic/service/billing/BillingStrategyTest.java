package lk.icbt.cis6003.dentalclinic.service.billing;

import lk.icbt.cis6003.dentalclinic.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the three billing rules (FR4).
 *
 * DESIGN PATTERN UNDER TEST: Strategy.
 * Each rule works out the charge for a visit differently. The tests describe
 * those differences in money, because that is what the patient actually sees on
 * the receipt.
 *
 * The loyalty discount is a step every rule shares, so it is tested once per
 * rule to prove the shared step really is shared (Template Method).
 */
@DisplayName("Billing strategies")
class BillingStrategyTest {

    private static final BigDecimal CONSULTATION_FEE = new BigDecimal("1500.00");

    private Treatment treatment(String code, String name, String cost) {
        Treatment t = new Treatment();
        t.setTreatmentCode(code);
        t.setName(name);
        t.setCost(new BigDecimal(cost));
        t.setDurationMinutes(45);
        t.setActive(true);
        return t;
    }

    private BillingContext context(Treatment treatment, int completedVisits) {
        return new BillingContext(treatment, CONSULTATION_FEE, completedVisits);
    }

    @Nested
    @DisplayName("Standard billing")
    class StandardBilling {

        private final BillingStrategy strategy = new StandardBillingStrategy();

        @Test
        @DisplayName("charges the treatment cost plus the consultation fee")
        void chargesTreatmentPlusConsultation() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-003", "Tooth Filling", "6000.00"), 0));

            assertThat(charge.getTreatmentCost()).isEqualByComparingTo("6000.00");
            assertThat(charge.getConsultationFee()).isEqualByComparingTo("1500.00");
            assertThat(charge.getDiscount()).isEqualByComparingTo("0.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("7500.00");
        }

        @Test
        @DisplayName("gives a loyal patient 10 percent off the treatment cost")
        void appliesLoyaltyDiscount() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-003", "Tooth Filling", "6000.00"), 5));

            // 10% of 6000 = 600 off, so 6000 + 1500 - 600 = 6900
            assertThat(charge.getDiscount()).isEqualByComparingTo("600.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("6900.00");
        }

        @Test
        @DisplayName("gives no loyalty discount before the fifth completed visit")
        void noDiscountOnTheFourthVisit() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-003", "Tooth Filling", "6000.00"), 4));

            assertThat(charge.getDiscount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("names itself, so the reason can be printed on the bill")
        void reportsItsName() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-003", "Tooth Filling", "6000.00"), 0));

            assertThat(charge.getStrategyName()).isEqualTo("Standard");
            assertThat(charge.getExplanation()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Surgical billing")
    class SurgicalBilling {

        private final BillingStrategy strategy = new SurgicalBillingStrategy();

        @Test
        @DisplayName("adds a 15 percent sterilisation supplement to the treatment cost")
        void addsSterilisationSupplement() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-011", "Wisdom Tooth Surgery", "35000.00"), 0));

            // 35000 + 15% = 40250, plus the 1500 consultation fee
            assertThat(charge.getTreatmentCost()).isEqualByComparingTo("40250.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("41750.00");
        }

        @Test
        @DisplayName("works the loyalty discount out on the supplemented cost")
        void loyaltyDiscountUsesSupplementedCost() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-011", "Wisdom Tooth Surgery", "35000.00"), 6));

            // 10% of 40250 = 4025
            assertThat(charge.getDiscount()).isEqualByComparingTo("4025.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("37725.00");
        }

        @Test
        @DisplayName("names itself Surgical")
        void reportsItsName() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-011", "Wisdom Tooth Surgery", "35000.00"), 0));

            assertThat(charge.getStrategyName()).isEqualTo("Surgical");
        }
    }

    @Nested
    @DisplayName("Consultation-only billing")
    class ConsultationOnlyBilling {

        private final BillingStrategy strategy = new ConsultationOnlyBillingStrategy();

        @Test
        @DisplayName("waives the consultation fee, because the visit is the consultation")
        void waivesTheConsultationFee() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-001", "Dental Check-up", "2000.00"), 0));

            // Charging both would bill the patient twice for the same thing.
            assertThat(charge.getTreatmentCost()).isEqualByComparingTo("2000.00");
            assertThat(charge.getConsultationFee()).isEqualByComparingTo("0.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("still gives a loyal patient the discount")
        void stillAppliesLoyaltyDiscount() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-001", "Dental Check-up", "2000.00"), 10));

            assertThat(charge.getDiscount()).isEqualByComparingTo("200.00");
            assertThat(charge.getTotal()).isEqualByComparingTo("1800.00");
        }

        @Test
        @DisplayName("names itself Consultation only")
        void reportsItsName() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-001", "Dental Check-up", "2000.00"), 0));

            assertThat(charge.getStrategyName()).isEqualTo("Consultation only");
        }
    }

    @Nested
    @DisplayName("Money handling, shared by every strategy")
    class MoneyHandling {

        private final BillingStrategy strategy = new StandardBillingStrategy();

        @Test
        @DisplayName("rounds a discount to two decimal places")
        void roundsDiscountToTwoDecimalPlaces() {
            // 10% of 4555.55 is 455.555, which is not a real amount of money.
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-002", "Scaling", "4555.55"), 5));

            assertThat(charge.getDiscount()).isEqualByComparingTo("455.56");
            assertThat(charge.getDiscount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("never returns a total below zero")
        void neverGoesBelowZero() {
            BillCharge charge = strategy.calculate(
                    context(treatment("TRT-000", "Free Advice", "0.00"), 20));

            assertThat(charge.getTotal()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }
}
